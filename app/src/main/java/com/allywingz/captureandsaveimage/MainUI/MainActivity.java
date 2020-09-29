package com.allywingz.captureandsaveimage.MainUI;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.allywingz.captureandsaveimage.ImageSupport.OptionsActivity;
import com.allywingz.captureandsaveimage.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    ImageView mImage;
    Bitmap imageBitmap;
    String filePath ;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImage = findViewById(R.id.mImage);
    }

    public void selectImage(View v) {
        try {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_IMAGE_CAPTURE);
            } else {
                Intent intent = new Intent(MainActivity.this, OptionsActivity.class);
                intent.putExtra(OptionsActivity.FLAG_COMPRESS, true);
                intent.putExtra(OptionsActivity.FLAG_CAMERA, true);
                intent.putExtra(OptionsActivity.FLAG_GALLERY, true);
                startActivityForResult(intent, 123);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MainActivity.this, OptionsActivity.class);
                intent.putExtra(OptionsActivity.FLAG_COMPRESS, true);
                intent.putExtra(OptionsActivity.FLAG_CAMERA, true);
                intent.putExtra(OptionsActivity.FLAG_GALLERY, true);
                startActivityForResult(intent, 123);
            } else {
                Toast.makeText(MainActivity.this, "Camera Permission is Required to Use camera.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("value", "Permission Granted, Now you can use local drive .");
            } else {
                Log.e("value", "Permission Denied, You cannot use local drive .");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && resultCode == Activity.RESULT_OK) {
            filePath = Objects.requireNonNull(data).getStringExtra(OptionsActivity.RESULT_FILE_PATH);
            Log.i("filePath", "0==>" + filePath);
            imageBitmap = BitmapFactory.decodeFile(filePath);
            new AsyncTaskExample().execute();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class AsyncTaskExample extends AsyncTask<String, String, Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                OutputStream fos;
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    ContentResolver resolver = getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SaveImages");

                    String fileName = getString(R.string.app_name) + "_" + System.currentTimeMillis() + ".Jpg";
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName); // My Image Name
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");

                    Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    Log.i("imageUri", "===>" + Objects.requireNonNull(imageUri).toString());
                    fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
                } else {
                    String ImageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/SaveImages";
                    String fileName = getString(R.string.app_name) + "_" + System.currentTimeMillis() + ".Jpg";
                    File image = new File(ImageDir, fileName);
                    fos = new FileOutputStream(image);
                }
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                mImage.setImageBitmap(imageBitmap);
                Objects.requireNonNull(fos).close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return imageBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mImage.setImageBitmap(imageBitmap);
        }
    }


}