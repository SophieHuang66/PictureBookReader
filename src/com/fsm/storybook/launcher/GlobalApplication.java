package com.fsm.storybook.launcher;


import java.util.List;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.fsm.storybook.model.AuthStatus;
import com.fsm.storybook.model.BookCategory;
import com.fsm.storybook.model.BookData;
import com.fsm.storybook.model.Epub3Package;
import com.fsm.storybook.model.Unit;

public class GlobalApplication extends Application {
	
	private LruCache<String, Bitmap> mMemoryCache;
	private Epub3Package mEpub3Package;
	private AuthStatus mAuthStatus = new AuthStatus();
	private List<BookData> mBookList = null;
	private List<Unit> mUnitList = null;
	private List<BookCategory> mBookCategoryList = null;

	public void onCreate() {
		super.onCreate();
		//create cache
		/*
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        int memClassBytes = am.getMemoryClass() * 1024 * 1024;
        int cacheSize = memClassBytes / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
        */
	}
	
 	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
 	    /*
 		if (getBitmapFromMemCache(key) == null) {
 	        mMemoryCache.put(key, bitmap);
 	    }
 	    */
 	}

 	public Bitmap getBitmapFromMemCache(String key) {
 	    //return mMemoryCache.get(key);
 		return null;
 	}

	public Epub3Package getEpub3Package() {
		return mEpub3Package;
	}

	public void setEpub3Package(Epub3Package mEpub3Package) {
		this.mEpub3Package = mEpub3Package;
	} 		
	
	public AuthStatus getAuthStatus() {
		return mAuthStatus;
	}

	public void setAuthStatus(AuthStatus mAuthStatus) {
		this.mAuthStatus = mAuthStatus;
	}

	public List<BookData> getBookList() {
		return mBookList;
	}

	public void setBookList(List<BookData> mBookList) {
		this.mBookList = mBookList;
	}

	public List<Unit> getUnitList() {
		return mUnitList;
	}

	public void setUnitList(List<Unit> mUnitList) {
		this.mUnitList = mUnitList;
	}

	public List<BookCategory> getBookCategoryList() {
		return mBookCategoryList;
	}

	public void setBookCategoryList(List<BookCategory> mBookCategoryList) {
		this.mBookCategoryList = mBookCategoryList;
	}	
}
