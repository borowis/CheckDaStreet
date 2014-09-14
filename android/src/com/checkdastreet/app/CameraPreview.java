package com.checkdastreet.app;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

public class CameraPreview extends SurfaceView implements Callback {
	
	SurfaceHolder mHolder;
	Camera mCamera;
	Context mContext;
	
	@SuppressWarnings("deprecation")
	public CameraPreview(Context context, Camera camera) {
		super(context);
		mContext = context;
		mCamera = camera;
		mHolder = getHolder();
        mHolder.addCallback(this);
        
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		
		if (mHolder.getSurface() == null)
          return;

		mCamera.stopPreview();

		setCameraDisplayOrientation();
		
		try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (IOException e) {
        	Toast.makeText(mContext, "Camera preview failed", Toast.LENGTH_LONG).show();
        }
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
        	Toast.makeText(mContext, "Camera preview failed", Toast.LENGTH_LONG).show();
        }
	}
	
	public void setCameraDisplayOrientation() 
	{        
	     if (mCamera == null)
	         return;             

	     Camera.CameraInfo info = new Camera.CameraInfo();
	     Camera.getCameraInfo(0, info);
	     Camera.Parameters parameters = mCamera.getParameters();

	     WindowManager winManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
	     int rotation = winManager.getDefaultDisplay().getRotation();

	     int degrees = 0;

	     switch (rotation) {
	         case Surface.ROTATION_0: degrees = 0; break;
	         case Surface.ROTATION_90: degrees = 90; break;
	         case Surface.ROTATION_180: degrees = 180; break;
	         case Surface.ROTATION_270: degrees = 270; break;
	     }

	     int result;
	     if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) 
	     {
	         result = (info.orientation + degrees) % 360;
	         result = (360 - result) % 360;  // + зеркало
	     } else {
	         result = (info.orientation - degrees + 360) % 360;
	     }

	     mCamera.setDisplayOrientation(result);
	     
	     int rotate = (degrees + 270) % 360;
	     parameters.setRotation(rotate);
	     mCamera.setParameters(parameters);
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

}
