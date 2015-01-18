package com.fsm.storybook.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

public class EpubWebView extends WebView {
	
	public EpubWebView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public EpubWebView(Context context, AttributeSet attrs)  {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	public EpubWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	@Override
	public ActionMode startActionMode(ActionMode.Callback callback) {
	    // this will start a new, custom Contextual Action Mode, in which you can control
	    // the menu options available.
		Callback actionModeCallback = new EpubSelectActionModeCallback();
	    return super.startActionMode(actionModeCallback);
	}

	
	public class EpubSelectActionModeCallback implements ActionMode.Callback {

	    @Override
	    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
	        // TODO Auto-generated method stub
	        return false;
	    }

	    @Override
	    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
	        // TODO Auto-generated method stub
	        //mode.getMenuInflater().inflate(R.menu.highlight_menu, menu);
	    	Log.i("EpubWebView", "highlight text");
	    	
	        return true;
	    }

	    @Override
	    public void onDestroyActionMode(ActionMode mode) {
	        // TODO Auto-generated method stub
	    }

	    @Override
	    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
	        // TODO Auto-generated method stub
	        return false;
	    }

	} 	
	
}
