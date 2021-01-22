package com.kochchi.refie;

import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ads.consent.ConsentForm;
import com.google.ads.consent.ConsentFormListener;
import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.consent.DebugGeography;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import static com.kochchi.refie.CamActivity.getPath;


public class ShareActivity extends AppCompatActivity {

    private Button share_button;
    private Bitmap selectedImage;
    private  ImageView shareIV;
    private Integer mImageMaxWidth;
    private Integer mImageMaxHeight;
    private Bitmap fromGallery;
    private String imagePath;
    private String camMode;
    private ImageView imageV;
    private File file4;
    private ImageView mback;
    private ConsentForm form;
    private AdView mAdView;

    private final String TAG = "consent";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        getConsentStatus();

        imagePath = getIntent().getStringExtra("Ifile");
        camMode = getIntent().getStringExtra("bCam");
        //Toast.makeText(this, camMode, Toast.LENGTH_SHORT).show();
        imageV = findViewById(R.id.shareImageView);

        mAdView = findViewById(R.id.adView2);

        mback = findViewById(R.id.s_back);
        mback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                //startCamera();
            }
        });

        //Toast.makeText(this, imagePath, Toast.LENGTH_SHORT).show();
        ImageView imageView = findViewById(R.id.shareImageView);
        selectedImage = BitmapFactory.decodeFile(imagePath);

        //fixing selfie mirror
        if(camMode.equals("f")) {
            //Toast.makeText(this, "inside cam", Toast.LENGTH_SHORT).show();
            selectedImage = flip(selectedImage, false, true);
            selectedImage = rotate(selectedImage, 270);
            setflipImage();
        }

        try {
            imageView.setImageBitmap(modifyOrientation(selectedImage, imagePath));
        }catch (Exception ex){
            ex.printStackTrace();
        }



        share_button = findViewById(R.id.shareButton);
        share_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/png");
                share.putExtra(Intent.EXTRA_STREAM, Uri.parse(imagePath));
                startActivity(Intent.createChooser(share, "Share Image"));
            }
        });
    }

    public void setflipImage(){
        file4 = new File (imagePath);
        if (file4.exists ())
            file4.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file4);
            selectedImage.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap modifyOrientation(Bitmap bitmap, String image_absolute_path) throws IOException {
        ExifInterface ei = new ExifInterface(image_absolute_path);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotate(bitmap, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotate(bitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotate(bitmap, 270);

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flip(bitmap, true, false);

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return flip(bitmap, false, true);

            default:
                return bitmap;
        }
    }

    private Pair<Integer, Integer> getTargetedWidthHeight() {
        int targetWidth;
        int targetHeight;
        int maxWidthForPortraitMode = getImageMaxWidth();
        int maxHeightForPortraitMode = getImageMaxHeight();
        targetWidth = maxWidthForPortraitMode;
        targetHeight = maxHeightForPortraitMode;
        return new Pair<>(targetWidth, targetHeight);
    }

    private Integer getImageMaxWidth() {
        if (mImageMaxWidth == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxWidth = shareIV.getWidth();
        }

        return mImageMaxWidth;
    }

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private Integer getImageMaxHeight() {
        if (mImageMaxHeight == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxHeight =
                    shareIV.getHeight();
        }

        return mImageMaxHeight;
    }


    private void getConsentStatus() {
        ConsentInformation consentInformation = ConsentInformation.getInstance(ShareActivity.this);
        consentInformation.addTestDevice("39819FCC276E636D25C8F0B183908EF0");
        consentInformation.setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA);
        String[] publisherIds = {"pub-9277825907732792"};
        consentInformation.requestConsentInfoUpdate(publisherIds, new ConsentInfoUpdateListener() {
            @Override
            public void onConsentInfoUpdated(ConsentStatus consentStatus) {
                Log.d("consen", "inside consnrt info update");
                // User's consent status successfully updated.
                if (ConsentInformation.getInstance(getBaseContext()).isRequestLocationInEeaOrUnknown()) {
                    Log.d("consen", consentStatus.toString());
                    switch (consentStatus) {
                        case UNKNOWN:
                            displayConsentForm();
                            break;
                        case PERSONALIZED:
                            initializeAds(true);
                            break;
                        case NON_PERSONALIZED:
                            initializeAds(false);
                            break;
                    }
                } else {
                    Log.d(TAG, "Not in EU, displaying normal ads");
                    initializeAds(true);
                }
            }
            @Override
            public void onFailedToUpdateConsentInfo(String errorDescription) {
                // User's consent status failed to update.
                Log.d("consen", errorDescription);
            }
        });
    }

    /**
     * Displays the consent form for advertisements through the Google Consent SDK
     */

    private void displayConsentForm() {
        URL privacyUrl = null;
        try {
            privacyUrl = new URL("https://markrichie.github.io/privacy_policy/index.html");
        } catch (MalformedURLException e) {
            Log.e(TAG, "Error processing privacy policy url", e);
        }
        form = new ConsentForm.Builder(this, privacyUrl)
                .withListener(new ConsentFormListener() {
                    @Override
                    public void onConsentFormLoaded() {
                        // Consent form loaded successfully.
                        form.show();
                    }
                    @Override
                    public void onConsentFormOpened() {
                        // Consent form was displayed.
                    }
                    @Override
                    public void onConsentFormClosed(ConsentStatus consentStatus, Boolean userPrefersAdFree) {
                        // Consent form was closed.
                        if (consentStatus.equals(ConsentStatus.PERSONALIZED))
                            initializeAds(true);
                        else
                            initializeAds(false);
                    }
                    @Override
                    public void onConsentFormError(String errorDescription) {
                        // Consent form error. This usually happens if the user is not in the EU.
                        Log.e(TAG, "Error loading consent form: " + errorDescription);
                    }
                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .build();

        form.load();
    }

    /**
     * Initializes the applications main banner ad
     *
     * @param isPersonalized true if the ad should be personalized
     */
    private void initializeAds(boolean isPersonalized) {
        MobileAds.initialize(this);
        //mAdView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
        //mAdView.setAdSize(AdSize.BANNER);

        // this is the part you need to add/modify on your code
        AdRequest adRequest;
        if (isPersonalized) {
            adRequest = new AdRequest.Builder().build();
        } else {
            Bundle extras = new Bundle();
            extras.putString("npa", "1");
            adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                    .build();
        }

        mAdView.loadAd(adRequest);
    }
}
