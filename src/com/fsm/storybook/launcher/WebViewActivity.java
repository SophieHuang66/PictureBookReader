/*
 * WebViewActivity.java
 * SDKLauncher-Android
 *
 * Created by Yonathan Teitelbaum (Mantano) on 2013-07-10.
 * Copyright (c) 2012-2013 The Readium Foundation and contributors.
 * 
 * The Readium SDK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.fsm.storybook.launcher;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.EPub3;
import org.readium.sdk.android.Package;
import org.readium.sdk.android.SpineItem;
import org.readium.sdk.android.components.navigation.NavigationTable;
import org.readium.sdk.android.launcher.ContainerHolder;
import org.readium.sdk.android.launcher.model.OpenPageRequest;
import org.readium.sdk.android.launcher.model.Page;
import org.readium.sdk.android.launcher.model.PaginationInfo;
import org.readium.sdk.android.launcher.model.ReadiumJSApi;
import org.readium.sdk.android.launcher.model.ViewerSettings;
import org.readium.sdk.android.launcher.util.EpubServer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

import com.fsm.storybook.model.BookData;
import com.fsm.storybook.model.Bookmark;
import com.fsm.storybook.model.Highlight;
import com.fsm.storybook.util.ActivityUtil;
import com.fsm.storybook.util.BookDatabase;

public class WebViewActivity 
	extends FragmentActivity 
	implements OnClickListener, OnSeekBarChangeListener, OnQueryTextListener, OnHighlightListener {

	private static final String TAG = "WebViewActivity";
	private static final String READER_SKELETON = "file:///android_asset/readium-shared-js/reader.html";
	private static final String HIDDEN_READER_SKELETON = "file:///android_asset/readium-shared-js/reader_hidden.html";
	
	private Context context;
	private WebView mWebview;
	private WebView mHiddenWebview;
	private Container mContainer;
	private Package mPackage;
	private OpenPageRequest mOpenPageRequestData;
	private OpenPageRequest mTempOpenPageRequestData;
	private TextView mPageInfo;
	private SeekBar mPageSlider;
	private ViewerSettings mViewerSettings;
	private ReadiumJSApi mReadiumJSApi;
	private ReadiumJSApi mReadiumJSApiForHiddenWebView;
	private EpubServer mServer;
	private Menu mOptionsMenu;
	//private ProgressDialog progress;
	private BookData mBookData;
	private ArrayList<Bookmark> mBookmarkList;
	private ArrayList<Highlight> mHighlightList;
	private boolean isVerticalMode = false;
	private boolean allowZoomControls = false;
	private String currentOrientation;
	private int currentBookmarkCode = 0;
	
	private int pageCountCaculateStatus;
	private static final int PAGE_COUNT_CAL_STATUS_NONE = 0;
	private static final int PAGE_COUNT_CAL_STATUS_EXEC = 1;
	private static final int PAGE_COUNT_CAL_STATUS_DONE = 2;
	//private String orgPagesInfo;
	
	private boolean mIsMoAvailable;
	private boolean mIsMoPlaying;
	private boolean mIsToolbarVisible = false;
	
	private BookDatabase bkdb = null;
	private GlobalApplication globalApp;
	private ActivityUtil util;
	
    private View.OnTouchListener gestureListener;

    private static final int REQUEST_TOC = 1;
    private static final int REQUEST_SETTING = 2;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_web_view);
		
		context = this;
        bkdb = new BookDatabase(getApplicationContext());
        globalApp = (GlobalApplication)this.getApplicationContext();
        util = new ActivityUtil(this);
        currentOrientation = String.valueOf(util.getScreenOrientation());
		gestureListener = new ContentGestureListener(this);
        //gestureListener = new WebViewGestureDetector();
        
        mWebview = (WebView) findViewById(R.id.webview);
        //mWebview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		mWebview.setOnClickListener(WebViewActivity.this); 
		mWebview.setOnTouchListener(gestureListener);
        //mHiddenWebview = (WebView) findViewById(R.id.webview_hidden);
        //mHiddenWebview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		mPageInfo = (TextView) findViewById(R.id.page_info);
		mPageSlider = (SeekBar)findViewById(R.id.page_slider);
		mPageSlider.setOnSeekBarChangeListener(this);
		mPageInfo.setVisibility(View.GONE);
		mPageSlider.setVisibility(View.GONE);
		
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        
        if (extras==null) return;
        //if (intent.getFlags() == Intent.FLAG_ACTIVITY_NEW_TASK) {
            
                mContainer = ContainerHolder.getInstance().get(extras.getLong(Constants.CONTAINER_ID));
                if (mContainer == null) {
                	finish();
                	return;
                }
                mPackage = mContainer.getDefaultPackage();
                
                //找出書籍資料，書籤，閱讀設定等
            	int bookCode = extras.getInt(Constants.BOOK_CODE);
            	mBookData = bkdb.fetchBook(bookCode);
            	setTitle(mBookData.getTitle());
            	
            	//圓夢繪本每一頁就是一個spineitem，每個spineitem只有一頁
				List<SpineItem> spineItems = mPackage.getSpineItems();
				for(SpineItem item : spineItems) {
					String idref = item.getIdRef();
					mBookData.setSpineItemPageCount(String.valueOf(Configuration.ORIENTATION_PORTRAIT), idref, 1);
					mBookData.setSpineItemPageCount(String.valueOf(Configuration.ORIENTATION_LANDSCAPE), idref, 1);
				}
            	bkdb.updateBook(mBookData);
            	
            	/*
            	mBookmarkList = bkdb.fetchBookmarkList(bookCode);
            	mHighlightList = bkdb.fetchHighlightList(bookCode);
            	*/
            	mViewerSettings = new ViewerSettings(mBookData.getSpreadCount()==2, mBookData.getFontSize(), 20);
            	try {
            		Log.d(TAG, "openPageRequest JSON:"+extras.getString(Constants.OPEN_PAGE_REQUEST_DATA));
					mOpenPageRequestData = OpenPageRequest.fromJSON(extras.getString(Constants.OPEN_PAGE_REQUEST_DATA));
				} catch (JSONException e) {
					Log.e(TAG, "Constants.OPEN_PAGE_REQUEST_DATA must be a valid JSON object: "+e.getMessage(), e);
				}
            	
            	//是否已經取得總頁數
                if (mBookData.getTotalPageCount(currentOrientation)<=0) {
            		pageCountCaculateStatus = PAGE_COUNT_CAL_STATUS_NONE;
            	} else {
            		pageCountCaculateStatus = PAGE_COUNT_CAL_STATUS_DONE;
            	}
            	
              //啟動epub web server
                new AsyncTask<Void, Void, Void>() {
        			@Override
        			protected Void doInBackground(Void... params) {
                		mServer = new EpubServer(EpubServer.HTTP_HOST, EpubServer.HTTP_PORT, mPackage, false);
            			mServer.startServer();
            			return null;
                	}
                }.execute();
                
              //初始化計算頁數用的hidden webview
                /*
        		initHiddenWebView();
                mHiddenWebview.loadUrl(HIDDEN_READER_SKELETON);
                mReadiumJSApiForHiddenWebView = new ReadiumJSApi(new ReadiumJSApi.JSLoader() {
        			@Override
        			public void loadJS(String javascript) {
        				mHiddenWebview.loadUrl(javascript);
        			}
        		});
        		*/

              //初始化閱讀的webview
        		initWebView();
                mWebview.loadUrl(READER_SKELETON);
                mReadiumJSApi = new ReadiumJSApi(new ReadiumJSApi.JSLoader() {
        			@Override
        			public void loadJS(String javascript) {
        				mWebview.loadUrl(javascript);
        			}
        		});

        //}
	
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mServer!=null) mServer.stop();
        mWebview.loadUrl(READER_SKELETON);
		((ViewGroup) mWebview.getParent()).removeView(mWebview);
		mWebview.removeAllViews();
		mWebview.clearCache(true);
		mWebview.clearHistory();
		mWebview.destroy();
    	if (mContainer != null) {
    		ContainerHolder.getInstance().remove(mContainer.getNativePtr());
    		// Close book (need to figure out if this is the best place...)
    		EPub3.closeBook(mContainer);
    	}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mWebview.onPause();
		}
	}
	
	@Override
	protected void onResume() {
		Log.v(TAG, "onResume");
		super.onResume();
		
		//if we are still calculating total page count, don't do anything
		//if (pageCountCaculateStatus == PAGE_COUNT_CAL_STATUS_EXEC) return;
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			//mWebview.onResume();
			try {
				mWebview.getClass().getMethod("onResume").invoke(mWebview, (Object[]) null);  
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		//if we haven't calculated page count for this orientation, do it
        currentOrientation = String.valueOf(util.getScreenOrientation());
    	if (mBookData.getTotalPageCount(currentOrientation)<=0) {
    		pageCountCaculateStatus = PAGE_COUNT_CAL_STATUS_NONE;
    		startCalculateTotalPageCount();
    	} else {
    		pageCountCaculateStatus = PAGE_COUNT_CAL_STATUS_DONE;
    	}
	    super.onConfigurationChanged(newConfig);
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mOptionsMenu = menu;
		
		getMenuInflater().inflate(R.menu.web_view, menu);
		
		menu.findItem(R.id.settings).setIcon(R.drawable.setting);
		menu.findItem(R.id.settings).setVisible(false);
		
		menu.findItem(R.id.mo_previous).setIcon(R.drawable.mo_previous);
		menu.findItem(R.id.mo_next).setIcon(R.drawable.mo_next);
		menu.findItem(R.id.mo_play).setIcon(R.drawable.mo_play);
		menu.findItem(R.id.mo_pause).setIcon(R.drawable.mo_pause);

		MenuItem mo_previous = menu.findItem(R.id.mo_previous);
		MenuItem mo_next = menu.findItem(R.id.mo_next);
		MenuItem mo_play = menu.findItem(R.id.mo_play);
		MenuItem mo_pause = menu.findItem(R.id.mo_pause);
		
		mo_previous.setVisible(mIsMoAvailable);
		mo_next.setVisible(mIsMoAvailable);
		if(mIsMoAvailable){
			mo_play.setVisible(!mIsMoPlaying);
			mo_pause.setVisible(mIsMoPlaying);
		}

		//force buttons to show up
		/*
		mo_previous.setVisible(true);
		mo_next.setVisible(true);
		mo_play.setVisible(!mIsMoPlaying);
		mo_pause.setVisible(mIsMoPlaying);
		*/
		
		return true;
	}
	
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
	    int itemId = item.getItemId();
	    switch (itemId) {
	    case R.id.bookshelf:
	    	gotoBookshelf();
	    	return true;
	    case R.id.settings:
			Log.d(TAG, "Show settings");
			showSettings();
			/*
			//直接切換單跨頁
			int newSpreadCount = (mBookData.getSpreadCount()==2)?1:2;
			mBookData.setSpreadCount(newSpreadCount);
			bkdb.updateBook(mBookData);
			mViewerSettings = new ViewerSettings(newSpreadCount==2, mBookData.getFontSize(), 20);
			mReadiumJSApi.updateSettings(mViewerSettings);
			*/
			return true;
	    case R.id.mo_previous:
	    	mReadiumJSApi.previousMediaOverlay();
	    	return true;
		case R.id.mo_play:
			mReadiumJSApi.toggleMediaOverlay();
			return true;
		case R.id.mo_pause:
			mReadiumJSApi.toggleMediaOverlay();
			return true;
		case R.id.mo_next:
			mReadiumJSApi.nextMediaOverlay();
			return true;
	    }
	    return false;
	}

	@SuppressLint({ "SetJavaScriptEnabled", "NewApi" })
	private void initWebView() {
		mWebview.getSettings().setJavaScriptEnabled(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			mWebview.getSettings().setAllowUniversalAccessFromFileURLs(true);
		}
		mWebview.setWebViewClient(new EpubMainWebViewClient(mPackage));
		mWebview.setWebChromeClient(new EpubWebChromeClient());

		mWebview.addJavascriptInterface(new EpubInterface(), "LauncherUI");
	}
	
	/*
	@SuppressLint({ "SetJavaScriptEnabled", "NewApi" })
	private void initHiddenWebView() {
		mHiddenWebview.getSettings().setJavaScriptEnabled(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			mHiddenWebview.getSettings().setAllowUniversalAccessFromFileURLs(true);
		}
		mHiddenWebview.setWebViewClient(new EpubHiddenWebViewClient(mPackage));
		//mHiddenWebview.setWebChromeClient(new EpubWebChromeClient());

		mHiddenWebview.addJavascriptInterface(new EpubInterfaceForHiddenWebView(), "HiddenLauncherUI");
	}
	*/
	
	public void onClick(View v) {
		/*
		if (v.getId() == R.id.left) {
			mReadiumJSApi.openPageLeft();
		} else if (v.getId() == R.id.right) {
			mReadiumJSApi.openPageRight();
		}
		 */
	}

	private void gotoBookshelf()
 	{
		//goto bookshelf view
    	Intent intent = new Intent(getApplicationContext(), BookshelfActivity.class);
        startActivity(intent);
        finish();
 	}
	/*
	private void showTOC() {
		Bundle extras = getIntent().getExtras();
		//Intent intent = new Intent(this, TableOfContentsActivity.class);
		Intent intent = new Intent(this, TabListActivity.class);
		
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.BOOK_NAME, extras.getString(Constants.BOOK_NAME));
		intent.putExtra(Constants.CONTAINER_ID, extras.getLong(Constants.CONTAINER_ID));		
		intent.putExtra(Constants.BOOK_CODE, extras.getInt(Constants.BOOK_CODE));		
		//startActivity(intent);
		startActivityForResult(intent, REQUEST_TOC);
	}
	*/
	private void showSettings() {
		Intent intent = new Intent(this, ViewerSettingActivity.class);
        //spread count is not allowed to modified in vertical writing mode
        if (isVerticalMode) {
        	intent.putExtra(Constants.SETTING_MODE, Constants.SETTING_MODE_VERTICAL_BOOK);
        } else {
        	intent.putExtra(Constants.SETTING_MODE, Constants.SETTING_MODE_NORMAL);
        }
		intent.putExtra(Constants.SETTING_FONT_SIZE, mBookData.getFontSize());
		intent.putExtra(Constants.SETTING_SPREAD_COUNT, mBookData.getSpreadCount());		
		startActivityForResult(intent, REQUEST_SETTING);
	}

    public final class EpubMainWebViewClient extends EpubWebViewClient {

        private boolean skeletonPageLoaded = false;

        public EpubMainWebViewClient(Package pkg) {
			super(pkg);
		}

        @Override
        public void onPageFinished(WebView view, String url) {
        	Log.d(TAG, "onPageFinished: "+url);
        	if (!skeletonPageLoaded && url.equals(READER_SKELETON)) {
        		skeletonPageLoaded = true;
        		Log.d(TAG, "openPageRequestData: "+mOpenPageRequestData);
	            runOnUiThread(new Runnable() {
		        	@Override
		        	public void run()
		        	{
		            	mReadiumJSApi.openBook(mPackage, mViewerSettings, mOpenPageRequestData);
		        	}
			    });
        	}
        }

    }
    
    
    public final class EpubHiddenWebViewClient extends EpubWebViewClient {

		private boolean skeletonPageLoaded = false;

        public EpubHiddenWebViewClient(Package pkg) {
			super(pkg);
		}

        @Override
        public void onPageFinished(WebView view, String url) {
        	Log.d(TAG, "Hidden Webview - onPageFinished: "+url);
        	if (!skeletonPageLoaded && url.equals(HIDDEN_READER_SKELETON)) {
        		skeletonPageLoaded = true;
        		Log.d(TAG, "Hidden Webview - openPageRequestData: "+mOpenPageRequestData);
        		if (pageCountCaculateStatus==PAGE_COUNT_CAL_STATUS_DONE) return;

        		//reader主頁載入完成，開啓第一章（開始算頁數）
        		if (pageCountCaculateStatus==PAGE_COUNT_CAL_STATUS_NONE) {
        			startCalculateTotalPageCount();
        			//其他章載入完成，等onPaginationChanged的callback取得該章頁數
        		} else {
        			//do thing, let onPaginationChanged handle page info
        		}
        	}
        }
    }
    
	public class EpubWebChromeClient extends WebChromeClient implements
			MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
		@Override
		public void onShowCustomView(View view, CustomViewCallback callback) {
			Log.d(TAG, "here in on ShowCustomView: " + view);
			super.onShowCustomView(view, callback);
			if (view instanceof FrameLayout) {
				FrameLayout frame = (FrameLayout) view;
				Log.d(TAG, "frame.getFocusedChild(): " + frame.getFocusedChild());
				if (frame.getFocusedChild() instanceof VideoView) {
					VideoView video = (VideoView) frame.getFocusedChild();
					// frame.removeView(video);
					// a.setContentView(video);
					video.setOnCompletionListener(this);
					video.setOnErrorListener(this);
					video.start();
				}
			}
		}

		public void onCompletion(MediaPlayer mp) {
			Log.d(TAG, "Video completed");
			// a.setContentView(R.layout.main);
			// WebView wb = (WebView) a.findViewById(R.id.webview);
			// a.initWebView();
		}

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			Log.d(TAG, "MediaPlayer onError: " + what + ", " + extra);
			return false;
		}
	}
    
	/*
	public class EpubInterfaceForHiddenWebView extends EpubJavaScriptInterface {
		@JavascriptInterface
		public void onPaginationChanged(String currentPagesInfo) {
			try {
				PaginationInfo paginationInfo = PaginationInfo.fromJson(currentPagesInfo);
				isVerticalMode = paginationInfo.isVerticalMode();
				List<Page> openPages = paginationInfo.getOpenPages();
				if (!openPages.isEmpty()) {
					Page page = openPages.get(0);
					
					//還在計算總頁數
					if (pageCountCaculateStatus!=PAGE_COUNT_CAL_STATUS_DONE)
					{
						Log.d(TAG, String.format("spine %s has %d pages", page.getIdref(), page.getSpineItemPageCount()));
						mBookData.setSpineItemPageCount(currentOrientation, page.getIdref(), page.getSpineItemPageCount());

						List<SpineItem> spineItems = mPackage.getSpineItems();
						if (page.getSpineItemIndex()>=(spineItems.size()-1)) {
							Log.d(TAG, "reach the last spineItem");
							//已經是最後一個spineitem
							bkdb.updateBook(mBookData);
							stopCalculateTotalPageCount();
						} else {
							Log.d(TAG, "goto next spineItem");
							//到下一個spineitem去
							mTempOpenPageRequestData = OpenPageRequest.fromIdref(spineItems.get(page.getSpineItemIndex()+1).getIdRef());
						}
						
						//open page in webview
		            	runOnUiThread(new Runnable() {
			        		@Override
			        		public void run()
			        		{
			        			mReadiumJSApiForHiddenWebView.openBook(mPackage, mViewerSettings, mTempOpenPageRequestData);
			        		}
				    	});

					}

					//update pagination information
					runOnUiThread(new Runnable() {
						public void run() {
							//updatePageInfo(page.getSpineItemPageIndex() + 1, page.getSpineItemPageCount());
							int totalPageInBook = mBookData.getTotalPageCount(currentOrientation);
							//int currentPage = Math.round(mBookData.getCurrentPage() * totalPageInBook / 1000f);
							int currentPage = mBookData.getCurrentPage();
							if (currentPage==0) currentPage=1;
							updatePageInfo(currentPage, totalPageInBook);
						}
					});
				}
			} catch (JSONException e) {
				Log.e(TAG, ""+e.getMessage(), e);
			}
		}
		
	}
	*/
	
	public class EpubInterface extends EpubJavaScriptInterface {
		
		@JavascriptInterface
		public void onPaginationChanged(String currentPagesInfo) {
			Log.d(TAG, "onPaginationChanged: "+currentPagesInfo);
			try {
				PaginationInfo pinfo = PaginationInfo.fromJson(currentPagesInfo);
				isVerticalMode = pinfo.isVerticalMode();
				List<Page> openPages = pinfo.getOpenPages();
				if (!openPages.isEmpty()) {
					Page page = openPages.get(0);
					
						//set pagination information
						List<Page> pages = pinfo.getOpenPages();
						if (pages.isEmpty()) return;
						Page p = openPages.get(0);
						List<SpineItem> spineItems = mPackage.getSpineItems();
						int pagesBeforeCurrentSpineItem = 0;
						for(SpineItem item : spineItems) {
							String idref = item.getIdRef();
							if (idref.equals(p.getIdref())) break;
							pagesBeforeCurrentSpineItem += mBookData.getSpineItemPageCount(currentOrientation, idref);
						}
						/*
						int currentPageByBaseInBook = Math.round((pagesBeforeCurrentSpineItem+p.getSpineItemPageIndex()+1)*1000f/mBookData.getTotalPageCount(currentOrientation));
						int currentPageByBaseInSpine = Math.round((p.getSpineItemPageIndex()+1)*1000f/p.getSpineItemPageCount());
						Log.d(TAG, String.format("page in book: %d/%d, =%d/1000", pagesBeforeCurrentSpineItem+p.getSpineItemPageIndex()+1, mBookData.getTotalPageCount(currentOrientation), currentPageByBaseInBook));
						Log.d(TAG, String.format("page in spine: %d/%d, =%d/1000",p.getSpineItemPageIndex()+1, p.getSpineItemPageCount(), currentPageByBaseInSpine));
						*/
						int currentPageByBaseInBook = pagesBeforeCurrentSpineItem+p.getSpineItemPageIndex()+1;
						int currentPageByBaseInSpine = p.getSpineItemPageIndex()+1;
						mBookData.setCurrentPage(currentPageByBaseInBook);
						mBookData.setCurrentPageInSpineItem(currentPageByBaseInSpine);
						mBookData.setCurrentSpineItem(p.getIdref());
						//save pagination info
						bkdb.updateBook(mBookData);
						//check bookmark status
						/*
						currentBookmarkCode = 0;
						int pageRangeFrom = mBookData.getCurrentPage();
						int pageRangeTo = Math.round((pagesBeforeCurrentSpineItem+p.getSpineItemPageIndex()+2)*1000f/mBookData.getTotalPageCount(currentOrientation));
						if (pageRangeTo>pageRangeFrom) pageRangeTo--;
						for(Bookmark bm : mBookmarkList) {
							//Log.d(TAG, String.format("bookmark of page %d, between %d - %d?", bm.getPage(), pageRangeFrom, pageRangeTo));
							if (bm.getPage()>=pageRangeFrom && bm.getPage()<=pageRangeTo) {
								currentBookmarkCode = bm.getCode();
								break;
							}
						}
						*/
						//check if we should allow zoom controls
						allowZoomControls = false;
						SpineItem spineItem = mPackage.getSpineItem(page.getIdref());
						allowZoomControls = spineItem.isFixedLayout();

						runOnUiThread(new Runnable() {
							public void run() {
								//set zoom controls option
								mWebview.getSettings().setBuiltInZoomControls(allowZoomControls);
								mWebview.getSettings().setDisplayZoomControls(false);
							}
						});
					}
					
					//update pagination information
					runOnUiThread(new Runnable() {
						public void run() {
							//updatePageInfo(page.getSpineItemPageIndex() + 1, page.getSpineItemPageCount());
							int totalPageInBook = mBookData.getTotalPageCount(currentOrientation);
							//int currentPage = Math.round(mBookData.getCurrentPage() * totalPageInBook / 1000f);
							int currentPage = mBookData.getCurrentPage();
							if (currentPage==0) currentPage=1;
							updatePageInfo(currentPage, totalPageInBook);
						}
					});
				
			} catch (JSONException e) {
				Log.e(TAG, ""+e.getMessage(), e);
			}
		}
		
		@JavascriptInterface
		public void onIsMediaOverlayAvailable(String available){
			Log.d(TAG, "onIsMediaOverlayAvailable:" + available);
			final String isMediaOverlayAvailable = available;
			runOnUiThread(new Runnable() {
				public void run() {
					mIsMoAvailable = isMediaOverlayAvailable.equals("true");
					invalidateOptionsMenu();
				}
			});

		}
		
		@JavascriptInterface
		public void onMediaOverlayStatusChanged(String status) {
			Log.d(TAG, "onMediaOverlayStatusChanged:" + status);
			//this should be real json parsing if there will be more data that needs to be extracted
			final String mediaOverlayStatus = status; 
			runOnUiThread(new Runnable() {
				public void run() {
					if(mediaOverlayStatus.indexOf("isPlaying") > -1){
						mIsMoPlaying = mediaOverlayStatus.indexOf("\"isPlaying\":true") > -1;
					}
					
					invalidateOptionsMenu();
				}
			});
		}
		
		@JavascriptInterface
		public void getBookmarkData(final String bookmarkData) {
			//do nothing
		}
	}
	

	class ContentGestureListener extends SimpleOnGestureListener implements OnTouchListener
    {
        Context context;
        GestureDetector gDetector;

        public ContentGestureListener()
        {
            super();
        }

        public ContentGestureListener(Context context) {
            this(context, null);
        }

        public ContentGestureListener(Context context, GestureDetector gDetector) {

            if(gDetector == null)
                gDetector = new GestureDetector(context, this);

            this.context = context;
            this.gDetector = gDetector;
        }


        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //return super.onFling(e1, e2, velocityX, velocityY);
            try {
                if (Math.abs(e1.getY() - e2.getY()) > Constants.SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if(e1.getX() - e2.getX() > Constants.SWIPE_MIN_DISTANCE && Math.abs(velocityX) > Constants.SWIPE_THRESHOLD_VELOCITY) 
                {
                	runOnUiThread(new Runnable() {
    	        		@Override
    	        		public void run()
    	        		{
    	        			mReadiumJSApi.openPageRight();
    	        		}
    		    	});
                	return true;
                }  
                else if (e2.getX() - e1.getX() > Constants.SWIPE_MIN_DISTANCE && Math.abs(velocityX) > Constants.SWIPE_THRESHOLD_VELOCITY) 
                {
                	runOnUiThread(new Runnable() {
    	        		@Override
    	        		public void run()
    	        		{
    	            		mReadiumJSApi.openPageLeft();
    	        		}
    		    	});
                	return true;
                }
            } catch (Exception e) {
                // nothing
            }
            return false;        	
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
        	if (mIsToolbarVisible) {
        		mIsToolbarVisible = false;
        		mPageSlider.setVisibility(View.GONE);
        		mPageInfo.setVisibility(View.GONE);
        	} else {
        		mIsToolbarVisible = true;
        		mPageSlider.setVisibility(View.VISIBLE);
        		mPageInfo.setVisibility(View.VISIBLE);
        	}
        	return true;
        	//return super.onSingleTapConfirmed(e);
        }

        public GestureDetector getDetector()
        {
            return gDetector;
        }

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			return gDetector.onTouchEvent(event);
		}       
    }

	
	/**
	 * To detect swipe (turning page) action
	 */	
	/*
	class WebViewGestureDetector extends SimpleOnGestureListener implements OnGestureListener, OnTouchListener
    {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > Constants.SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if(e1.getX() - e2.getX() > Constants.SWIPE_MIN_DISTANCE && Math.abs(velocityX) > Constants.SWIPE_THRESHOLD_VELOCITY) 
                {
                	runOnUiThread(new Runnable() {
    	        		@Override
    	        		public void run()
    	        		{
    	        			mReadiumJSApi.openPageRight();
    	        		}
    		    	});
                }  
                else if (e2.getX() - e1.getX() > Constants.SWIPE_MIN_DISTANCE && Math.abs(velocityX) > Constants.SWIPE_THRESHOLD_VELOCITY) 
                {
                	runOnUiThread(new Runnable() {
    	        		@Override
    	        		public void run()
    	        		{
    	            		mReadiumJSApi.openPageLeft();
    	        		}
    		    	});
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
        	return false;
        }
                
		@Override
		public boolean onTouch(View v, MotionEvent event) {
        	Log.d(TAG,"onTouch");
        	return false
		}
 
    }
	*/
	
	/**
	 * To receive TOC selection result or Viewer Setting result
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		Log.d(TAG, "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);

		if (data==null) return;
		
		switch(requestCode){
			case REQUEST_TOC:
				if (data.getExtras().containsKey(Constants.TOC_ITEM)) 
				{
					String content = data.getExtras().getString(Constants.TOC_ITEM);
					Log.d(TAG, "Got activity result from TOC selection, open content : "+content);
					NavigationTable toc = mContainer.getDefaultPackage().getTableOfContents();
					mReadiumJSApi.openContentUrl(content, toc.getSourceHref());
				} 
				else if (data.getExtras().containsKey(Constants.BOOKMARK_CFI)) 
				{
					String idref = data.getExtras().getString(Constants.SPINE_IDREF);
					String cfi = data.getExtras().getString(Constants.BOOKMARK_CFI);
					Log.d(TAG, "Got activity result from Bookmark selection, open cfi : "+idref+", "+cfi);
					mReadiumJSApi.openSpineItemElementCfi(idref, cfi);
				} 
				else if (data.getExtras().containsKey(Constants.HIGHLIGHT_CFI)) 
				{
					String idref = data.getExtras().getString(Constants.SPINE_IDREF);
					String cfi = data.getExtras().getString(Constants.HIGHLIGHT_CFI);
					Log.d(TAG, "Got activity result from Highlight selection, open cfi : "+idref+", "+cfi);
					mReadiumJSApi.openSpineItemElementCfi(idref, cfi);
				}
				break;
			case REQUEST_SETTING:
				Log.d(TAG, "Got activity result from ViewerSetting ");
				/*
				boolean fontChanged = false;
				int fontSize = data.getExtras().getInt(Constants.SETTING_FONT_SIZE);
				*/
				int speadCount = data.getExtras().getInt(Constants.SETTING_SPREAD_COUNT);
				//如果改變了字型大小，重算頁數
				/*
				if (mBookData.getFontSize()!=fontSize) fontChanged = true;
				mBookData.setFontSize(fontSize);
				*/
				mBookData.setSpreadCount(speadCount);
				bkdb.updateBook(mBookData);
				//mViewerSettings = new ViewerSettings(speadCount==2, fontSize, 20);
				mViewerSettings = new ViewerSettings(speadCount==2, mBookData.getFontSize(), 20);
				mReadiumJSApi.updateSettings(mViewerSettings);
				/*
				if (fontChanged) {
					mBookData.clearTotalPageCount();
					startCalculateTotalPageCount();
				}
				*/
			default:
				break;
		}
	}
	
	private void updatePageInfo(int currentPage, int totalPage)
	{

		if (pageCountCaculateStatus != PAGE_COUNT_CAL_STATUS_DONE) {
			mPageInfo.setText(getString(R.string.page_calculating,
					totalPage));	
			mPageSlider.setMax(totalPage-1);
		} else {
			mPageInfo.setText(getString(R.string.page_x_of_y,
				currentPage,
				totalPage));	
			mPageSlider.setMax(totalPage-1);
			mPageSlider.setProgress(currentPage-1);
		}
	}

	/**
	 * for seekbar 
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser && pageCountCaculateStatus==PAGE_COUNT_CAL_STATUS_DONE) {
			int page_to_open = progress + 1;
			//找出progress數字在哪一章
			List<SpineItem> spineItems = mPackage.getSpineItems();
			int pagesBeforeCurrentSpineItem = 0;
			for(SpineItem item : spineItems) {
				String idref = item.getIdRef();
				int pagesOfCurrentSpineItem = mBookData.getSpineItemPageCount(currentOrientation, idref);
				if ( page_to_open <= pagesBeforeCurrentSpineItem + pagesOfCurrentSpineItem ) {
					int spineItemPageIndex = page_to_open - pagesBeforeCurrentSpineItem - 1;
					mOpenPageRequestData = OpenPageRequest.fromIdrefAndIndex(idref, spineItemPageIndex);
					break;
				}
				pagesBeforeCurrentSpineItem += mBookData.getSpineItemPageCount(currentOrientation, idref);
			}
        	runOnUiThread(new Runnable() {
        		@Override
        		public void run()
        		{
        			mReadiumJSApi.openBook(mPackage, mViewerSettings, mOpenPageRequestData);
        		}
	    	});
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
	
	private void lockScreenRotation() {
		//lock screen rotation, so that user can't disturb the calculation of page count
		if (currentOrientation==String.valueOf(Configuration.ORIENTATION_PORTRAIT)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
	}
	private void unlockScreenRotation() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}

	private void startCalculateTotalPageCount() {
		Log.i(TAG, "startCalculateTotalPageCount");
		pageCountCaculateStatus = PAGE_COUNT_CAL_STATUS_EXEC;
		lockScreenRotation();
		//到第一頁（開始算頁數）
		List<SpineItem> spineItems = mPackage.getSpineItems();
		mTempOpenPageRequestData = OpenPageRequest.fromIdref(spineItems.get(0).getIdRef());
		runOnUiThread(new Runnable() {
			@Override
			public void run()
			{
				//progress = ProgressDialog.show(context, "Loading", getResources().getString(R.string.msg_calculate_page), true);
    			//mHiddenWebview.setVisibility(View.INVISIBLE);
				mReadiumJSApiForHiddenWebView.openBook(mPackage, mViewerSettings, mTempOpenPageRequestData);
			}
		});
	}
	private void stopCalculateTotalPageCount() {
		Log.i(TAG, "stopCalculateTotalPageCount");
		pageCountCaculateStatus = PAGE_COUNT_CAL_STATUS_DONE;
		unlockScreenRotation();
		runOnUiThread(new Runnable() {
    		@Override
    		public void run()
    		{
				//progress.dismiss();
    			//mWebview.setVisibility(View.VISIBLE);
				int totalPageInBook = mBookData.getTotalPageCount(currentOrientation);
				//int currentPage = Math.round(mBookData.getCurrentPage() * totalPageInBook / 1000f);'
				int currentPage = mBookData.getCurrentPage();
				if (currentPage==0) currentPage=1;
				updatePageInfo(currentPage, totalPageInBook);
    		}
    	});
	}
	
	/**
	 * Handle Search Text Action
	 */
	@Override
	public boolean onQueryTextSubmit(String query) {
		return true;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		return false;
	}

	/**
	 * Handle Highlight Action
	 */
	@Override
	public int saveHighlight(String cfi, String text) {
		return 0;
	}

	@Override
	public int saveHighlight(String cfi, String text, String note) {
		return 0;
	}

	@Override
	public Highlight getHighlight(int id) {
		return null;
	}

	@Override
	public boolean deleteHighlight(int id) {
		return true;
	}	
	
}
