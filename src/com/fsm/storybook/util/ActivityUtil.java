package com.fsm.storybook.util;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Display;

public class ActivityUtil {
	private Activity act;
	
	public ActivityUtil(Activity act) {
		this.act = act;
	}
	
	public Drawable resizeImage(int resId, int w, int h)
	{
	      // load the origial Bitmap
	      Bitmap BitmapOrg = BitmapFactory.decodeResource(act.getResources(), resId);
	      int width = BitmapOrg.getWidth();
	      int height = BitmapOrg.getHeight();
	      int newWidth = w;
	      int newHeight = h;
	      // calculate the scale
	      float scaleWidth = ((float) newWidth) / width;
	      float scaleHeight = ((float) newHeight) / height;
	      // create a matrix for the manipulation
	      Matrix matrix = new Matrix();
	      matrix.postScale(scaleWidth, scaleHeight);
	      Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0,width, height, matrix, true);
	      //return new BitmapDrawable(resizedBitmap);
	      return new BitmapDrawable(Resources.getSystem(), resizedBitmap);
	}

	public int getScreenOrientation()
	{
	    Display getOrient = act.getWindowManager().getDefaultDisplay();
	    int orientation = Configuration.ORIENTATION_UNDEFINED;
	    DisplayMetrics metrics = new DisplayMetrics();  
	    getOrient.getMetrics(metrics);
	    if(metrics.widthPixels<=metrics.heightPixels){
	    	orientation = Configuration.ORIENTATION_PORTRAIT;
	    } else{ 
	        orientation = Configuration.ORIENTATION_LANDSCAPE;
	    }
	    return orientation;
	}
	
	
}
