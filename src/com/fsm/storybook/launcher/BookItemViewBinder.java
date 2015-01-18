package com.fsm.storybook.launcher;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SimpleAdapter.ViewBinder;

public class BookItemViewBinder implements ViewBinder {
    @Override
    public boolean setViewValue(View view, Object data,String textRepresentation) {
        
    	if (view.getId()==R.id.img)
    	{
            ImageView iv = (ImageView)view;
    		if (data instanceof Bitmap) {
                Bitmap bmp = (Bitmap)data;
                iv.setImageBitmap(bmp);
    		}
    		else
    		{
    			iv.setImageResource(R.drawable.book_downloading);
    		}
    		return true;
    	}
        else if ((view instanceof ImageButton))
        {
        	ImageButton ib = (ImageButton)view;
        	ib.setTag(data);
        	return true;
        }
        return false;

        /*
    	if((view instanceof ImageView) && (data instanceof Bitmap))
        {
            ImageView iv = (ImageView)view;
            Bitmap bmp = (Bitmap)data;
            iv.setImageBitmap(bmp);
            return true;
        }
        else if ((view instanceof ImageButton))
        {
        	ImageButton ib = (ImageButton)view;
        	ib.setTag(data);
        	return true;
        }
        return false;
        */
    }
	
}
