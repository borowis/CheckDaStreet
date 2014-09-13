package com.checkdastreet.app;

import java.io.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    ImageButton capture, recapture, save;
    CameraPreview preview;
    Camera mCamera;
    FrameLayout mFrame;
    LinearLayout mButtons;
    Context mContext;
    Uri fileUri;
    Intent intent;
    byte[] pictureData;

    File photoFile;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        intent = getIntent();
        Log.i("MY", intent.getAction());
        mContext = this;

        File pictures = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        photoFile = new File(pictures, "myphoto.jpg");

        // кнопки
        mButtons = (LinearLayout) findViewById(R.id.buttons);

        capture = (ImageButton) findViewById(R.id.capture);
        capture.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCamera.takePicture(null, null, null, mPictureCallback);
                    }
                }
        );

        recapture = (ImageButton) findViewById(R.id.recapture);
        recapture.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideConfirm();
                        mCamera.startPreview();
                        pictureData = null;
                    }
                }
        );

        save = (ImageButton) findViewById(R.id.finish);
        save.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri pictureFile;

                        if (intent.hasExtra(MediaStore.EXTRA_OUTPUT)) {
                            pictureFile = (Uri)intent.getExtras().getParcelable(MediaStore.EXTRA_OUTPUT);
                        }
                        else
                            pictureFile = generateFile();

                        try {
                            savePhotoInFile (pictureData, pictureFile);
                            intent.putExtra("data", pictureData);
                            intent.setData(pictureFile);
                            setResult(RESULT_OK, intent);
                        } catch (Exception e) {
                            setResult(2, intent);
                        }
                        mCamera.release();
                        finish();
                    }
                }
        );

    }

    private Camera openCamera() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
            return null;

        Camera cam = null;
        if (Camera.getNumberOfCameras() > 0) {
            try {
                cam = Camera.open(0);
            }
            catch (Exception exc) {
                //
            }
        }

        return cam;
    }

    private PictureCallback mPictureCallback = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            if (intent.getAction().equals (MediaStore.ACTION_IMAGE_CAPTURE)) {
                pictureData = data;
                showConfirm ();
            }
            else {
                Uri pictureFile = generateFile();
                try {
                    //SAVE TO FILE

                    FileOutputStream fos = new FileOutputStream(photoFile);
                    fos.write(data);
                    fos.close();



                    Toast.makeText(mContext, "Save file: " + photoFile.getName(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {

                    Toast.makeText(mContext, "Error: can't save file", Toast.LENGTH_LONG).show();
                }
                mCamera.startPreview();
            }
        }
    };

    private void showConfirm() {
        capture.setVisibility(View.INVISIBLE);
        mButtons.setVisibility(View.VISIBLE);
    }

    private void hideConfirm() {
        mButtons.setVisibility(View.INVISIBLE);
        capture.setVisibility(View.VISIBLE);
    }

    private void savePhotoInFile(byte[] data, Uri pictureFile) throws Exception {

        if (pictureFile == null)
            throw new Exception();

        OutputStream os = getContentResolver().openOutputStream(pictureFile);
        os.write(data);
        os.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
            mFrame.removeView(preview);
            mCamera = null;
            preview = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamera = openCamera ();
        if (mCamera == null) {
            Toast.makeText(this, "Opening camera failed", Toast.LENGTH_LONG).show();
            return;
        }

        preview = new CameraPreview(this, mCamera);
        mFrame = (FrameLayout) findViewById(R.id.layout);
        mFrame.addView(preview, 0);
    }

    private Uri generateFile() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return null;

        File path = new File (Environment.getExternalStorageDirectory(), "CameraTest");
        if (! path.exists()){
            if (! path.mkdirs()){
                return null;
            }
        }

        String timeStamp = String.valueOf(System.currentTimeMillis());
        File newFile = new File(path.getPath() + File.separator + timeStamp + ".jpg");
        return Uri.fromFile(newFile);
    }
}