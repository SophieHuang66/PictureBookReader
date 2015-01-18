package com.fsm.storybook.launcher;

import android.util.Log;
import android.webkit.JavascriptInterface;

public class EpubJavaScriptInterface {
	
	private static final String TAG = "EpubJavaScriptInterface";
	
		@JavascriptInterface
		public void onPaginationChanged(String currentPagesInfo) {
			Log.d(TAG, "onPaginationChanged: "+currentPagesInfo);
		}
		
		@JavascriptInterface
		public void onSettingsApplied() {
			Log.d(TAG, "onSettingsApplied");
		}
		
		@JavascriptInterface
		public void onReaderInitialized() {
			Log.d(TAG, "onReaderInitialized");
		}
		
		@JavascriptInterface
		public void onContentLoaded() {
			Log.d(TAG, "onContentLoaded");
		}
		
		@JavascriptInterface
		public void onPageLoaded() {
			Log.d(TAG, "onPageLoaded");
		}
		
		@JavascriptInterface
		public void onIsMediaOverlayAvailable(String available){
			Log.d(TAG, "onIsMediaOverlayAvailable:" + available);
		}
		
		@JavascriptInterface
		public void onMediaOverlayStatusChanged(String status) {
			Log.d(TAG, "onMediaOverlayStatusChanged:" + status);
		}

		@JavascriptInterface
		public void getBookmarkData(final String bookmarkData) {
			Log.d(TAG, "getBookmarkData:" + bookmarkData);
		}

//		
//		@JavascriptInterface
//		public void onMediaOverlayTTSSpeak() {
//			Log.d(TAG, "onMediaOverlayTTSSpeak");
//		}
//		
//		@JavascriptInterface
//		public void onMediaOverlayTTSStop() {
//			Log.d(TAG, "onMediaOverlayTTSStop");
//		}
		
		
}
