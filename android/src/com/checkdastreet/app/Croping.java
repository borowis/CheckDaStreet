package com.checkdastreet.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;
import boofcv.alg.filter.binary.GThresholdImageOps;

import boofcv.android.ConvertBitmap;
import boofcv.android.VisualizeImageData;
import boofcv.struct.image.ImageUInt8;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by redcat on 14.09.2014.
 */
public class Croping extends Activity {
    private Uri mImageCaptureUri;
    private ImageView mImageView;
    private static final int PICK_FROM_CAMERA = 1;
    private static final int CROP_FROM_CAMERA = 2;
    private static final int THRESHOLD = 150;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.croping);

        Intent intent 	 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                "tmp_avatar_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));

        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);


        try {
            intent.putExtra("return-data", true);
            startActivityForResult(intent, PICK_FROM_CAMERA);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }

        mImageView		= (ImageView) findViewById(R.id.iv_photo);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        switch (requestCode) {
            case PICK_FROM_CAMERA:
                doCrop();

                break;

            case CROP_FROM_CAMERA:
                Bundle extras = data.getExtras();

                if (extras != null) {
                    Bitmap photo = extras.getParcelable("data");

                    binarization(photo);

                    mImageView.setImageBitmap(photo);
                } else {
                    Toast.makeText(this, "Empty", Toast.LENGTH_SHORT).show();
                }


                File f = new File(mImageCaptureUri.getPath());

                if (f.exists()) f.delete();

                break;
        }
    }

    private void doCrop() {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");

        List<ResolveInfo> list = getPackageManager().queryIntentActivities( intent, 0 );

        int size = list.size();

        if (size == 0) {
            Toast.makeText(this, "Can not find image crop app", Toast.LENGTH_SHORT).show();

            return;
        } else {
            intent.setData(mImageCaptureUri);

            intent.putExtra("outputX", 300);
            intent.putExtra("outputY", 300);
            //intent.putExtra("aspectX", 1);
            //intent.putExtra("aspectY", 1);
            intent.putExtra("scale", true);
            intent.putExtra("return-data", true);

            if (size == 1) {
                Intent i 		= new Intent(intent);
                ResolveInfo res	= list.get(0);

                i.setComponent( new ComponentName(res.activityInfo.packageName, res.activityInfo.name));

                startActivityForResult(i, CROP_FROM_CAMERA);
            }
        }
    }

    protected void binarization(Bitmap photo){
        ImageUInt8 binary =  new ImageUInt8(photo.getWidth(),photo.getHeight());
        ImageUInt8 afterOps = new ImageUInt8(photo.getWidth(),photo.getHeight());
        ImageUInt8 input =  new ImageUInt8(photo.getWidth(),photo.getHeight());

        byte[] workBuffer = ConvertBitmap.declareStorage(photo, null);
        ConvertBitmap.bitmapToGray(photo,input,workBuffer);

        GThresholdImageOps.threshold(input,binary,THRESHOLD, false);

        afterOps.setTo(binary);

        VisualizeImageData.binaryToBitmap(afterOps, photo, workBuffer);

    }
}