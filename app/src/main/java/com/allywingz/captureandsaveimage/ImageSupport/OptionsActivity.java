package com.allywingz.captureandsaveimage.ImageSupport;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.allywingz.captureandsaveimage.ImageSupport.imageCompression.CompressionListener;
import com.allywingz.captureandsaveimage.ImageSupport.PickImage.PickImage;
import com.allywingz.captureandsaveimage.R;

public class OptionsActivity extends Activity {
    private static final int EXTERNAL_PERMISSION_CODE = 1234;

    private ProgressBar progressBar;
    private TextView textViewCamera;
    private TextView textViewGallery;
    private TextView textViewCancel;

    private PickImage imagePicker;

    private boolean isCompress = true, isCamera = true, isGallery = true;
    public static final String FLAG_COMPRESS = "flag_compress";
    public static final String FLAG_CAMERA = "flag_camera";
    public static final String FLAG_GALLERY = "flag_gallery";

    public static final String RESULT_FILE_PATH = "result_file_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_image_select);

        imagePicker = new PickImage();

        Log.i("ImageSelectActivity", "==>Entered");


        progressBar = findViewById(R.id.progressBar);
        textViewCamera = findViewById(R.id.textViewCamera);
        textViewGallery = findViewById(R.id.textViewGallery);
        textViewCancel = findViewById(R.id.textViewCancel);

        textViewCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        textViewCamera.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                toggleProgress(true);
                imagePicker.withActivity(OptionsActivity.this).chooseFromGallery(false).chooseFromCamera(true).withCompression(isCompress).start();
            }
        });
        textViewGallery.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                toggleProgress(true);
                imagePicker.withActivity(OptionsActivity.this).chooseFromGallery(true).chooseFromCamera(false).withCompression(isCompress).start();
            }
        });

        if (getIntent() != null) {
            isCompress = getIntent().getBooleanExtra(FLAG_COMPRESS, true);
            isCamera = getIntent().getBooleanExtra(FLAG_CAMERA, true);
            isGallery = getIntent().getBooleanExtra(FLAG_GALLERY, true);
        }

        if (isCamera && isGallery) toggleProgress(false);
        else toggleProgress(true);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private boolean checkPermission() {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        return currentAPIVersion < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_PERMISSION_CODE);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == EXTERNAL_PERMISSION_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if ((!isCamera || !isGallery))
                    imagePicker.withActivity(this).chooseFromGallery(isGallery).chooseFromCamera(isCamera).withCompression(isCompress).start();
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PickImage.SELECT_IMAGE) {
            if (resultCode == RESULT_OK) {
                imagePicker.addOnCompressListener(new CompressionListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onCompressed(String filePath) {
                        if (filePath != null && isCompress) {
                            //return filepath
                            Intent intent = new Intent();
                            intent.putExtra(RESULT_FILE_PATH, filePath);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }
                });
                String filePath = imagePicker.getImageFilePath(data);
                if (filePath != null && !isCompress) {
                    //return filepath
                    Intent intent = new Intent();
                    intent.putExtra(RESULT_FILE_PATH, filePath);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    private void toggleProgress(boolean showProgress) {
        progressBar.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        textViewCamera.setVisibility(showProgress ? View.GONE : View.VISIBLE);
        textViewGallery.setVisibility(showProgress ? View.GONE : View.VISIBLE);
        textViewCancel.setVisibility(showProgress ? View.GONE : View.VISIBLE);
    }
}
