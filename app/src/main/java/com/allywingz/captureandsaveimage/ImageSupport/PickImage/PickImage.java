package com.allywingz.captureandsaveimage.ImageSupport.PickImage;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.allywingz.captureandsaveimage.ImageSupport.imageCompression.CompressImage;
import com.allywingz.captureandsaveimage.ImageSupport.imageCompression.CompressionListener;
import com.allywingz.captureandsaveimage.ImageSupport.provider.SelectionProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PickImage {
    private Activity activity;
    private Fragment fragment;
    private boolean isCompress = true, isCamera = true, isGallery = true;
    public static final int SELECT_IMAGE = 121;
    private CompressionListener imageCompressionListener;
    private String filePath;

    public PickImage withActivity(Activity activity) {
        this.activity = activity;
        return this;
    }

    public PickImage chooseFromCamera(boolean isCamera) {
        this.isCamera = isCamera;
        return this;
    }

    public PickImage chooseFromGallery(boolean isGallery) {
        this.isGallery = isGallery;
        return this;
    }

    public PickImage withCompression(boolean isCompress) {
        this.isCompress = isCompress;
        return this;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void start() {
        if (activity != null && fragment != null) {
            throw new IllegalStateException("Cannot add both activity and fragment");
        } else if (activity == null && fragment == null) {
            throw new IllegalStateException("Activity and fragment both are null");
        } else {
            if (!checkPermission()) {
                throw new IllegalStateException("Write External Permission not found");
            } else {
                if (!isCamera && !isGallery) {
                    throw new IllegalStateException("select source to pick image");
                } else {
                    if (activity != null)
                        activity.startActivityForResult(getPickImageChooserIntent(), SELECT_IMAGE);
                    else
                        fragment.startActivityForResult(getPickImageChooserIntent(), SELECT_IMAGE);
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private boolean checkPermission() {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        return currentAPIVersion < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(activity != null ? activity : Objects.requireNonNull(fragment.getActivity()), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(activity != null ? activity : Objects.requireNonNull(fragment.getActivity()), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(activity != null ? activity : Objects.requireNonNull(fragment.getActivity()), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private Intent getPickImageChooserIntent() {
        // Determine Uri of camera image to save.
        Uri outputFileUri = getCaptureImageOutputUri();

        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = activity != null ? activity.getPackageManager() : Objects.requireNonNull(fragment.getActivity()).getPackageManager();

        if (isCamera) {
            // collect all camera intents
            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            captureIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
            List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
            for (ResolveInfo res : listCam) {
                Intent intent = new Intent(captureIntent);
                intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
                intent.setPackage(res.activityInfo.packageName);
                if (outputFileUri != null) {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                }
                allIntents.add(intent);
            }
        }

        if (isGallery) {
            // collect all gallery intents
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
            for (ResolveInfo res : listGallery) {
                Intent intent = new Intent(galleryIntent);
                intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
                intent.setPackage(res.activityInfo.packageName);
                allIntents.add(intent);
            }
        }

        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent() != null && intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        // Create a chooser from the main intent
        Intent chooserIntent = Intent.createChooser(mainIntent, "Select source");

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[0]));

        return chooserIntent;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = activity != null ? activity.getExternalFilesDir("") : Objects.requireNonNull(fragment.getActivity()).getExternalFilesDir("");
        if (getImage != null) {
            String fileName = "IMG_" + System.currentTimeMillis() + ".Jpg";
            filePath = new File(getImage.getPath(), fileName).getPath();
            outputFileUri = SelectionProvider.getUriForFile(activity != null ? activity : Objects.requireNonNull(fragment.getActivity()),
                    activity != null ? activity.getApplicationContext().getPackageName()+".provider" : fragment.getActivity().getApplicationContext().getPackageName()+".provider",
                    new File(getImage.getPath(), fileName));
        }
        return outputFileUri;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String getPickImageResultFilePath(Intent data) {
        boolean isCamera = data == null || data.getData() == null;
        if (isCamera) return filePath;
        else return getRealPathFromURI(data.getData());
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String getRealPathFromURI(Uri contentUri) {
        OutputStream out;
        File file = new File(getFilename());

        try {
            if (file.createNewFile()) {
                InputStream iStream;
                iStream = activity != null ? activity.getContentResolver().openInputStream(contentUri) : Objects.requireNonNull(fragment.getContext()).getContentResolver().openInputStream(contentUri);
                byte[] inputData = getBytes(Objects.requireNonNull(iStream));
                out = new FileOutputStream(file);
                out.write(inputData);
                out.close();
                return file.getAbsolutePath();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String getImageFilePath(Intent data) {
        if (!isCompress)
            return getPickImageResultFilePath(data);
        else if (getPickImageResultFilePath(data) != null) {
            new CompressImage(activity != null ? activity : fragment.getActivity(), getPickImageResultFilePath(data), imageCompressionListener).execute();
        }
        return null;
    }

    public void addOnCompressListener(CompressionListener imageCompressionListener) {
        this.imageCompressionListener = imageCompressionListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String getFilename() {
        Context context = activity != null ? activity : fragment.getContext();
        File mediaStorageDir = new File(Objects.requireNonNull(context).getExternalFilesDir(""), "uncompressed");
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
        }

        String mImageName = "IMG_" + System.currentTimeMillis() + ".Jpg";
        return mediaStorageDir.getAbsolutePath() + "/" + mImageName;

    }

    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
    }
}
