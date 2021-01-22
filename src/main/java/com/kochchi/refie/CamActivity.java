package com.kochchi.refie;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.lifecycle.LifecycleOwner;

import android.os.Environment;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.kochchi.refie.GraphicOverlay.Graphic;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

public class CamActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    private static final String TAG = "MainActivity";
    private ImageView mImageView;
    private Button camButton;
    private Button camButton1;
    private Button camButton2;
    private TextView tip;
    private int template;
    private Bitmap passBit;
   // private Button mFaceButton;
    private Button useButton;
    private Button newButton;
    private Bitmap mSelectedImage;
    private Bitmap fromGallery;
    private int backCam = 0;
    private GraphicOverlay mGraphicOverlay;
    // Max width (portrait mode)
    private Integer mImageMaxWidth;
    // Max height (portrait mode)
    private Integer mImageMaxHeight;
    private List<FirebaseVisionFace> fa;
    private String xy;
    private String ti;
    private String mode;
    private int pstn;
    FirebaseVisionImage finalImage;
    private ImageView mback;
    /**
     * Name of the model file hosted with Firebase.
     */
    private static final String HOSTED_MODEL_NAME = "cloud_model_1";
    private static final String LOCAL_MODEL_ASSET = "mobilenet_v1_1.0_224_quant.tflite";
    /**
     * Name of the label file stored in Assets.
     */
    private static final String LABEL_PATH = "labels.txt";
    /**
     * Number of results to show in the UI.
     */
    private static final int RESULTS_TO_SHOW = 3;
    /**
     * Dimensions of inputs.
     */
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    private static final int DIM_IMG_SIZE_X = 224;
    private static final int DIM_IMG_SIZE_Y = 224;
    /**
     * Labels corresponding to the output of the vision model.
     */
    private List<String> mLabelList;

    private final PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float>
                                o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });
    /* Preallocated buffers for storing image data. */
    private final int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
    /**
     * An instance of the driver class to run model inference with Firebase.
     */
    private FirebaseModelInterpreter mInterpreter;
    /**
     * Data configuration of input & output data of model.
     */
    private FirebaseModelInputOutputOptions mDataOptions;


    TextureView textureView;

    public static final int GET_FROM_GALLERY = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam);


        final Toolbar myTool = (Toolbar) findViewById(R.id.blank_toolBar);
        setSupportActionBar(myTool);

        myTool.setVisibility(View.VISIBLE);

        mImageView = findViewById(R.id.image_view);
        mImageView.setVisibility(View.VISIBLE);
        pstn = 0;


        //mode = getIntent().getStringExtra("temp");
        /*if(mode.equals("one")){
            Toast.makeText(this, "in mode", Toast.LENGTH_SHORT).show();
            mSelectedImage = BitmapFactory.decodeFile("te.jpg");


        }else{*/

        //}


        //cam code start
        textureView = findViewById(R.id.view_finder);
        mGraphicOverlay = findViewById(R.id.graphic_overlay);

        //cam code end

        mback = findViewById(R.id.back);
        mback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                //startCamera();
            }
        });

        useButton = findViewById(R.id.use);
        newButton = findViewById(R.id.newI);
        camButton = findViewById(R.id.imgCapture);
        camButton.setVisibility(View.INVISIBLE);
        camButton1 = findViewById(R.id.imgCapture2);
        camButton1.setVisibility(View.INVISIBLE);
        camButton2 = findViewById(R.id.imgCapture3);
        camButton2.setVisibility(View.INVISIBLE);
        tip = findViewById(R.id.textView2);
        tip.setVisibility(View.INVISIBLE);

        /*mCloudButton = findViewById(R.id.button_cloud_text);
        mRunCustomModelButton = findViewById(R.id.button_run_custom_model);

        mGraphicOverlay = findViewById(R.id.graphic_overlay);
        mTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runTextRecognition();
                //startCamera();
            }
        });*/
       /* mFaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runFaceContourDetection();
                //startCamera();
            }
        });
        /*mCloudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runCloudTextRecognition();
            }
        });*/
        useButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              useButton.setVisibility(View.INVISIBLE);
              newButton.setVisibility(View.INVISIBLE);
              camButton.setVisibility(View.VISIBLE);
              camButton1.setVisibility(View.VISIBLE);
              camButton2.setVisibility(View.VISIBLE);
              //flipButton.setVisibility(View.VISIBLE);
              mImageView.setVisibility(View.INVISIBLE);
              textureView.setVisibility(View.VISIBLE);
              tip.setVisibility(View.VISIBLE);
              backCam = 0;
              myTool.setVisibility(View.INVISIBLE);
              startCamera();
            }
        });

        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myTool.setVisibility(View.VISIBLE);
                startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), GET_FROM_GALLERY);
                //selectImage();
            }
        });

        camButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    CameraX.unbindAll();

                    if(backCam == 1){
                        backCam = 0;
                        startCamera();
                    }else{
                        backCam = 1;
                        startCam();
                    }

            }
        });

        camButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if(mode.equals("custom")){
                mImageView.setVisibility(View.VISIBLE);
                myTool.setVisibility(View.VISIBLE);
                /*}else{
                    mImageView.setVisibility(View.INVISIBLE);
                }*/
                useButton.setVisibility(View.VISIBLE);
                newButton.setVisibility(View.VISIBLE);
                camButton.setVisibility(View.INVISIBLE);
                camButton1.setVisibility(View.INVISIBLE);
                camButton2.setVisibility(View.INVISIBLE);
                tip.setVisibility(View.INVISIBLE);
                //flipButton.setVisibility(View.VISIBLE);
                CameraX.unbindAll();
                textureView.setVisibility(View.INVISIBLE);
            }
        });

        /*flipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CamActivity.this, "Voila!", Toast.LENGTH_SHORT).show();
            }
        });*/

        Spinner dropdown = findViewById(R.id.spinner);
        dropdown.setVisibility(View.INVISIBLE);
        String[] items = new String[]{"Test Image 1 (Text)", "Test Image 2 (Text)", "Test Image 3" +
                " (Face)", "Test Image 4 (Object)", "Test Image 5 (Object)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout
                .simple_spinner_dropdown_item, items);

        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(this);
        initCustomModel();

        //mSelectedImage = getBitmapFromAsset(this, "tennis.jpg");
        //mImageView.setImageBitmap(mSelectedImage);
        //runFaceContourDetection();
        //processFaceContourDetectionResult(fa);
        //setbitM();

        //onItemSelected(adapter, View.generateViewId(),1,);
        mode = getIntent().getStringExtra("tem");
        //Toast.makeText(this, mode, Toast.LENGTH_SHORT).show();
        if(mode.equals("first")){
            pstn = 1;
            //mImageView.setVisibility(View.INVISIBLE);
        }else if(mode.equals("sec")){
            pstn = 2;
            //mImageView.setVisibility(View.INVISIBLE);
        }else if(mode.equals("thir")){
            pstn = 3;
           // mImageView.setVisibility(View.INVISIBLE);
        }else if(mode.equals("four")){
            pstn = 4;
            //mImageView.setVisibility(View.INVISIBLE);
        }else if(mode.equals("custom")){
            startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), GET_FROM_GALLERY);
            //pstn = 5;
            //selectImage();
        }
    }


    public void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, GET_FROM_GALLERY);
        }
    }

    private void processTextRecognitionResult(FirebaseVisionText texts) {

    }

    public void runFaceContourDetection() {


        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mSelectedImage);
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                        .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                        .build();
        Log.d("CameraXApp", "inside run");
        //mFaceButton.setEnabled(false);
        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<FirebaseVisionFace>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionFace> faces) {
                                //mFaceButton.setEnabled(true);
                                Log.d("CameraXApp", "inside success part");
                                fa = faces;
                                processFaceContourDetectionResult(faces);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                //mFaceButton.setEnabled(true);
                                e.printStackTrace();
                            }
                        });


    }

    public void processFaceContourDetectionResult(List<FirebaseVisionFace> faces) {
        // Task completed successfully
        if (faces.size() == 0) {
            showToast("No face found");
            return;
        }
        mGraphicOverlay.clear();
        //for (int i = 0; i < faces.size(); ++i) {
        //Toast.makeText(this, String.valueOf(faces.size()), Toast.LENGTH_SHORT).show();
        FirebaseVisionFace face = faces.get(0);
        //float rotY = face.getHeadEulerAngleZ();
        //String val = String.valueOf(rotY);
        //Toast.makeText(this, val, Toast.LENGTH_SHORT).show();

        FaceContourGraphic faceGraphic = new FaceContourGraphic(mGraphicOverlay);
        mGraphicOverlay.add(faceGraphic);
        //faceGraphic.draw();
        faceGraphic.updateFace(face);
        //ImageView img = (ImageView) findViewById(R.id.image_view);
        //img.setImageResource(R.drawable.common_google_signin_btn_text_dark);
        //}
    }

    private void runCloudTextRecognition() {

        processFaceContourDetectionResult(fa);
        //faceContourGraphic.draw();
    }


    private void initCustomModel() {
        mLabelList = loadLabelList(this);

        int[] inputDims = {DIM_BATCH_SIZE, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, DIM_PIXEL_SIZE};
        int[] outputDims = {DIM_BATCH_SIZE, mLabelList.size()};
        try {
            mDataOptions =
                    new FirebaseModelInputOutputOptions.Builder()
                            .setInputFormat(0, FirebaseModelDataType.BYTE, inputDims)
                            .setOutputFormat(0, FirebaseModelDataType.BYTE, outputDims)
                            .build();
            /*FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions
                    .Builder()
                    .requireWifi()
                    .build();*/
            /*FirebaseRemoteModel remoteModel = new FirebaseRemoteModel.Builder
                    (HOSTED_MODEL_NAME)
                    .enableModelUpdates(true)
                    .setInitialDownloadConditions(conditions)
                    .setUpdatesDownloadConditions(conditions)  // You could also specify
                    // different conditions
                    // for updates
                    .build();*/
            FirebaseLocalModel localModel =
                    new FirebaseLocalModel.Builder("asset")
                            .setAssetFilePath(LOCAL_MODEL_ASSET).build();
            FirebaseModelManager manager = FirebaseModelManager.getInstance();
            //manager.registerRemoteModel(remoteModel);
            manager.registerLocalModel(localModel);
            FirebaseModelOptions modelOptions =
                    new FirebaseModelOptions.Builder()
                            //.setRemoteModelName(HOSTED_MODEL_NAME)
                            .setLocalModelName("asset")
                            .build();
            mInterpreter = FirebaseModelInterpreter.getInstance(modelOptions);
        } catch (FirebaseMLException e) {
            showToast("Error while setting up the model");
            e.printStackTrace();
        }
    }




    /**
     * Gets the top labels in the results.
     */


    /**
     * Reads label list from Assets.
     */
    private List<String> loadLabelList(Activity activity) {
        List<String> labelList = new ArrayList<>();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(activity.getAssets().open
                             (LABEL_PATH)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                labelList.add(line);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read label list.", e);
        }
        return labelList;
    }

    /**
     * Writes Image data into a {@code ByteBuffer}.
     */
    private synchronized ByteBuffer convertBitmapToByteBuffer(
            Bitmap bitmap, int width, int height) {
        ByteBuffer imgData =
                ByteBuffer.allocateDirect(
                        DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y,
                true);
        imgData.rewind();
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0,
                scaledBitmap.getWidth(), scaledBitmap.getHeight());
        // Convert the image to int points.
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                imgData.put((byte) ((val >> 16) & 0xFF));
                imgData.put((byte) ((val >> 8) & 0xFF));
                imgData.put((byte) (val & 0xFF));
            }
        }
        return imgData;
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Functions for loading images from app assets.

    // Returns max image width, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private Integer getImageMaxWidth() {
        if (mImageMaxWidth == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxWidth = mImageView.getWidth();
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
                    mImageView.getHeight();
        }

        return mImageMaxHeight;
    }

    // Gets the targeted width / height.
    private Pair<Integer, Integer> getTargetedWidthHeight() {
        int targetWidth;
        int targetHeight;
        int maxWidthForPortraitMode = getImageMaxWidth();
        int maxHeightForPortraitMode = getImageMaxHeight();
        targetWidth = maxWidthForPortraitMode;
        targetHeight = maxHeightForPortraitMode;
        return new Pair<>(targetWidth, targetHeight);
    }

    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        //mGraphicOverlay.clear();
        position = pstn;

        switch (position) {
            case 0:
                //mSelectedImage = getBitmapFromAsset(this, "flip.jpg");
                break;
            case 1:
                // Whatever you want to happen when the thrid item gets selected
                mSelectedImage = getBitmapFromAsset(this, "tennis.jpg");
                break;
            case 2:
                // Whatever you want to happen when the thrid item gets selected
                mSelectedImage = getBitmapFromAsset(this, "sider.jpg");
                break;
            case 3:
                // Whatever you want to happen when the thrid item gets selected
                //mSelectedImage = fromGallery;
                mSelectedImage = getBitmapFromAsset(this, "front.jpg");
                break;
            case 4:
                mSelectedImage = getBitmapFromAsset(this, "flip.jpg");
                //mSelectedImage = getBitmapFromAsset(this, "mountain.jpg");
                //mImageView.setVisibility(View.INVISIBLE);
                //mSelectedImage = getBitmapFromAsset(this, "mirror.jpg");
                break;
            case 5:
                selectImage();
               // mSelectedImage = passBit;
                //mSelectedImage = getBitmapFromAsset(this, "mountain.jpg");
                //mImageView.setVisibility(View.INVISIBLE);
                //mSelectedImage = getBitmapFromAsset(this, "mirror.jpg");
                break;


        }

        //selectImageBit(mSelectedImage);
        if (mSelectedImage != null) {
            // Get the dimensions of the View
            Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

            int targetWidth = targetedSize.first;
            int maxHeight = targetedSize.second;

            // Determine how much to scale down the image
            float scaleFactor =
                    Math.max(
                            (float) mSelectedImage.getWidth() / (float) targetWidth,
                            (float) mSelectedImage.getHeight() / (float) maxHeight);

            Bitmap resizedBitmap =
                    Bitmap.createScaledBitmap(
                            mSelectedImage,
                            (int) (mSelectedImage.getWidth() / scaleFactor),
                            (int) (mSelectedImage.getHeight() / scaleFactor),
                            true);

            mImageView.setImageBitmap(resizedBitmap);
            mSelectedImage = resizedBitmap;

            runFaceContourDetection();
        }


    }

    public void selectImageBit(Bitmap mSelectedImage){

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();

        InputStream is;
        Bitmap bitmap = null;
        try {
            is = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }


    // cam code

    /*private void startCamera() {

        CameraX.unbindAll();

        Rational aspectRatio = new Rational (textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen

        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).setLensFacing(CameraX.LensFacing.BACK).build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    //to update the surface texture we  have to destroy it first then re-add it
                    @Override
                    public void onUpdated(Preview.PreviewOutput output){
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                });


        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).setLensFacing(CameraX.LensFacing.BACK).build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

        findViewById(R.id.imgCapture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".png");
                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        String msg = "Pic captured at " + file.getAbsolutePath();
                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                        String msg = "Pic capture failed : " + message;
                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                        if(cause != null){
                            cause.printStackTrace();
                        }
                    }
                });
            }
        });

        //bind to lifecycle:
        CameraX.bindToLifecycle((LifecycleOwner)this, preview, imgCap);
    }

    private void updateTransform(){
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)textureView.getRotation();

        switch(rotation){
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float)rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }*/
    private void updateTransform() {
        Matrix matrix = new Matrix();

        // Compute the center of the view finder
        float centerX = textureView.getWidth() / 2;
        float centerY = textureView.getHeight() / 2;

        // Correct preview output to account for display rotation
        int rotationDegrees;
        switch (textureView.getDisplay().getRotation()) {
            case Surface.ROTATION_0:
                rotationDegrees = 0;
                break;
            case Surface.ROTATION_90:
                rotationDegrees = 90;
                break;
            case Surface.ROTATION_180:
                rotationDegrees = 180;
                break;
            case Surface.ROTATION_270:
                rotationDegrees = 270;
                break;
            default:
                return;
        }
        matrix.postRotate(rotationDegrees, centerX, centerY);

        // Finally, apply transformations to our TextureView
        textureView.setTransform(matrix);
    }

    private void startCamera() {
        // Create configuration object for the viewfinder use case
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetAspectRatio(new Rational(textureView.getWidth(), textureView.getHeight()))
                .setTargetResolution(new Size(textureView.getWidth(), textureView.getHeight()))
                .setLensFacing(CameraX.LensFacing.FRONT)
                .build();

        // Build the viewfinder use case
        Preview preview = new Preview(previewConfig);

        // Everytime the viewfinder is updated, recompute the layout
        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                // To update the SurfaceTexture, we have to remove it and re-add it
                ViewGroup parent = (ViewGroup) textureView.getParent();
                parent.removeView(textureView);
                parent.addView(textureView, 0);

                textureView.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();
            }
        });

        // Create configuration object for the image capture use case

        // Setup image analysis pipeline that computes average pixel luminance
        // TODO add analyzerThread and setCallbackHandler as in the original example in Kotlin
        /*ImageAnalysisConfig analysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setLensFacing(CameraX.LensFacing.FRONT)
                .build();

        // Build the image analysis use case and instantiate our analyzer
        ImageAnalysis imageAnalysis = new ImageAnalysis(analysisConfig);
        //imageAnalysis.setAnalyzer(new LuminosityAnalyzer());
        imageAnalysis.setAnalyzer(
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(ImageProxy imageProxy, int degrees) {
                        // insert your code here.
                        //Log.d("inside","analyzer");

                        if (imageProxy == null || imageProxy.getImage() == null) {
                            return;
                        }
                        Image mediaImage = imageProxy.getImage();
                        int rotation = degreesToFirebaseRotation(degrees);
                        FirebaseVisionImage image =
                                FirebaseVisionImage.fromMediaImage(mediaImage, rotation);

                        runFaceContourDetection(image);

                    }
                });*/

        final ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).setLensFacing(CameraX.LensFacing.FRONT).build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

        findViewById(R.id.imgCapture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*String root = Environment.getExternalStorageDirectory().toString();
                File myDir = new File(root + "/Refie");
                myDir.mkdirs();*/
                ti = System.currentTimeMillis() + ".png";
                File file = new File( Environment.getExternalStorageDirectory()+ "/" + ti);
                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        String msg = file.getAbsolutePath();
                        //Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();

                        Intent i = new Intent(CamActivity.this, ShareActivity.class);
                        i.putExtra("Ifile", msg);
                        i.putExtra("bCam", "f");
                        startActivity(i);

                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                        String msg = "Pic capture failed : " + message;
                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                        if(cause != null){
                            cause.printStackTrace();
                        }
                    }
                });
            }
        });


        // Bind use cases to lifecycle
        CameraX.bindToLifecycle(this, preview, imgCap);
    }

    private void startCam() {
        // Create configuration object for the viewfinder use case
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetAspectRatio(new Rational(textureView.getWidth(), textureView.getHeight()))
                .setTargetResolution(new Size(textureView.getWidth(), textureView.getHeight()))
                .setLensFacing(CameraX.LensFacing.BACK)
                .build();

        // Build the viewfinder use case
        Preview preview = new Preview(previewConfig);

        // Everytime the viewfinder is updated, recompute the layout
        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                // To update the SurfaceTexture, we have to remove it and re-add it
                ViewGroup parent = (ViewGroup) textureView.getParent();
                parent.removeView(textureView);
                parent.addView(textureView, 0);

                textureView.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();
            }
        });

        // Create configuration object for the image capture use case

        // Setup image analysis pipeline that computes average pixel luminance
        // TODO add analyzerThread and setCallbackHandler as in the original example in Kotlin
        /*ImageAnalysisConfig analysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setLensFacing(CameraX.LensFacing.FRONT)
                .build();

        // Build the image analysis use case and instantiate our analyzer
        ImageAnalysis imageAnalysis = new ImageAnalysis(analysisConfig);
        //imageAnalysis.setAnalyzer(new LuminosityAnalyzer());
        imageAnalysis.setAnalyzer(
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(ImageProxy imageProxy, int degrees) {
                        // insert your code here.
                        //Log.d("inside","analyzer");

                        if (imageProxy == null || imageProxy.getImage() == null) {
                            return;
                        }
                        Image mediaImage = imageProxy.getImage();
                        int rotation = degreesToFirebaseRotation(degrees);
                        FirebaseVisionImage image =
                                FirebaseVisionImage.fromMediaImage(mediaImage, rotation);

                        runFaceContourDetection(image);

                    }
                });*/

        final ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).setLensFacing(CameraX.LensFacing.BACK).build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

        findViewById(R.id.imgCapture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*String root = Environment.getExternalStorageDirectory().toString();
                File myDir = new File(root + "/Refie");
                myDir.mkdirs();*/
                ti = System.currentTimeMillis() + ".png";
                File file = new File( Environment.getExternalStorageDirectory()+ "/" + ti);
                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        String msg = file.getAbsolutePath();
                        //Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();

                        Intent i = new Intent(CamActivity.this, ShareActivity.class);
                        i.putExtra("Ifile", msg);
                        i.putExtra("bCam", "b");
                        startActivity(i);

                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                        String msg = "Pic capture failed : " + message;
                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                        if(cause != null){
                            cause.printStackTrace();
                        }
                    }
                });
            }
        });


        // Bind use cases to lifecycle
        CameraX.bindToLifecycle(this, preview, imgCap);
    }


    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException(
                        "Rotation must be 0, 90, 180, or 270.");
        }
    }


    //get from gallery

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        //Detects request codes
        if(requestCode==GET_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            Uri selectedImage = data.getData();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                mSelectedImage = modifyOrientation(bitmap, getPath(this , selectedImage));
                /*passBit = fromGallery;
                mImageView.setImageBitmap(fromGallery);
                mSelectedImage = fromGallery;
                if (mSelectedImage != null) {
                    // Get the dimensions of the View
                    Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

                    int targetWidth = targetedSize.first;
                    int maxHeight = targetedSize.second;

                    // Determine how much to scale down the image
                    float scaleFactor =
                            Math.max(
                                    (float) mSelectedImage.getWidth() / (float) targetWidth,
                                    (float) mSelectedImage.getHeight() / (float) maxHeight);

                    Bitmap resizedBitmap =
                            Bitmap.createScaledBitmap(
                                    mSelectedImage,
                                    (int) (mSelectedImage.getWidth() / scaleFactor),
                                    (int) (mSelectedImage.getHeight() / scaleFactor),
                                    true);

                    mImageView.setImageBitmap(resizedBitmap);
                    mSelectedImage = resizedBitmap;
                }


                //runFaceContourDetection();
                //processFaceContourDetectionResult(fa);*/
                if (mSelectedImage != null) {

                    Log.d("inside", "imagr not null");
                    // Get the dimensions of the View
                    Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

                    int targetWidth = targetedSize.first;
                    int maxHeight = targetedSize.second;

                    // Determine how much to scale down the image
                    float scaleFactor =
                            Math.max(
                                    (float) mSelectedImage.getWidth() / (float) targetWidth,
                                    (float) mSelectedImage.getHeight() / (float) maxHeight);

                    Bitmap resizedBitmap =
                            Bitmap.createScaledBitmap(
                                    mSelectedImage,
                                    (int) (mSelectedImage.getWidth() / scaleFactor),
                                    (int) (mSelectedImage.getHeight() / scaleFactor),
                                    true);

                    mImageView.setImageBitmap(resizedBitmap);
                    mSelectedImage = resizedBitmap;

                    runFaceContourDetection();
                }
                else{
                    Log.d("inside", "image null");
                }


                //fromGallery =getBitmapFromAsset(this, selectedImage.getPath());
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    //absolute path
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public void setbitM() {
        mSelectedImage = getBitmapFromAsset(this, "tennis.jpg");
        try{
            mImageView.setImageBitmap(mSelectedImage);
            if (mSelectedImage != null) {
                Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

                int targetWidth = targetedSize.first;
                int maxHeight = targetedSize.second;

                // Determine how much to scale down the image
                float scaleFactor =
                        Math.max(
                                (float) mSelectedImage.getWidth() / (float) targetWidth,
                                (float) mSelectedImage.getHeight() / (float) maxHeight);

                Bitmap resizedBitmap =
                        Bitmap.createScaledBitmap(
                                mSelectedImage,
                                (int) (mSelectedImage.getWidth() / scaleFactor),
                                (int) (mSelectedImage.getHeight() / scaleFactor),
                                true);

                mImageView.setImageBitmap(resizedBitmap);
                mSelectedImage = resizedBitmap;

            }

        }catch (Exception ex){
            ex.printStackTrace();
        }

    }




}
