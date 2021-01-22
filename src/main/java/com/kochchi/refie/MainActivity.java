package com.kochchi.refie;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private Button mTextButton;
    private CardView cardView;
    private CardView temp1;
    private CardView temp2;
    private CardView temp3;
    private CardView temp4;
    private ImageView info;
    private AdView mAdView;
    private ConsentForm form;

    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String TAG = "consent";
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.CAMERA"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getConsentStatus();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        if(allPermissionsGranted()){
            //startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }



        info = findViewById(R.id.info_i);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(MainActivity.this, R.style.myD)
                        .setTitle("Refie")
                        .setMessage("version : 1.0")
                        .show();
            }
        });

        cardView = findViewById(R.id.cardView);
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CamActivity.class);
                intent.putExtra("tem", "custom");
                startActivity(intent);
            }
        });

        temp1 = findViewById(R.id.cardView1);
        temp1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CamActivity.class);
                intent.putExtra("tem", "first");
                startActivity(intent);
            }
        });

        temp2 = findViewById(R.id.cardView2);
        temp2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CamActivity.class);
                intent.putExtra("tem", "sec");
                startActivity(intent);
            }
        });

        temp3 = findViewById(R.id.cardView3);
        temp3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CamActivity.class);
                intent.putExtra("tem", "thir");
                startActivity(intent);
            }
        });

        temp4 = findViewById(R.id.cardView4);
        temp4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CamActivity.class);
                intent.putExtra("tem", "four");
                startActivity(intent);
            }
        });


        mAdView = findViewById(R.id.adView);
        /*MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                //startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }


    private void getConsentStatus() {
        ConsentInformation consentInformation = ConsentInformation.getInstance(MainActivity.this);
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
