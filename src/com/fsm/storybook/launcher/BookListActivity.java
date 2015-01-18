/**
 * 
 */
package com.fsm.storybook.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.fsm.storybook.model.BookData;
import com.fsm.storybook.model.Unit;
import com.fsm.storybook.util.ActivityUtil;
import com.fsm.storybook.util.BookDatabase;
import com.fsm.storybook.util.CMSAgent;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class BookListActivity extends Activity 
implements OnItemClickListener, AsyncTaskListener, OnQueryTextListener {
	
	private static final String TAG = "BookListActivity"; 
	private static final int REQUEST_LOGIN = 1;
	private static final int REQUEST_CATEGORY = 2;

	private static final int LIST_NORMAL = 1;
	private static final int LIST_SEARCH = 2;
	private static final int LIST_CATEGORY = 3;
	private static final int LIST_UNIT = 4;

	private Activity act;
	private Context context;
    private GlobalApplication globalApp;
    private ActivityUtil util;
	private BookDatabase bkdb;   
    private ProgressDialog progress;
    private Menu mOptionsMenu;

    private SimpleAdapter booklistAdapter;
    private List<Map<String, Object>> bookitems = null;
    private List<BookData> cmsbooklist = null;
    private int currentVisibleBookItem = -1;
    private FetchBookCoverTask currentFetchBookCoverTask = null;
    
    private ArrayAdapter<String> unitAdapter; 
    private List<Unit> unitList = null;
    
	//private boolean shouldDoSearch = false;
	
	private int currentListMode = LIST_NORMAL;
	private String currentUnitCode = "";
	private int prevSelectedUnitPosition = -1;
	private String currentCategoryCode;
	private String searchKeyword;

	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fsm_booklist);
        act = this;
        context = this;
        bkdb = new BookDatabase(getApplicationContext());
        globalApp = (GlobalApplication)this.getApplicationContext();
        util = new ActivityUtil(this);
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);
    
        getActionBar().setDisplayShowCustomEnabled(true);
        //getActionBar().setDisplayShowHomeEnabled(false);
        //getActionBar().setDisplayShowTitleEnabled(false);
        
        unitAdapter = new ArrayAdapter<String>(context, R.layout.fsm_listview, R.id.listTextView); 
    }

 	public void onResume()
 	{
 		Log.d(TAG, "onResume");
 		super.onResume();

 		if (currentListMode!=LIST_SEARCH && mOptionsMenu!=null) {
 			MenuItem searchItem = (MenuItem) mOptionsMenu.findItem(R.id.search);
 			if (searchItem!=null) searchItem.collapseActionView();
 		}
    	
    	//fetch book list
        if (bookitems==null)
        {
            progress = ProgressDialog.show(this, "Loading", getResources().getString(R.string.msg_fetch_booklist), true);
            
        	bookitems = new ArrayList<Map<String, Object>>();
        	//get cms book list in another thread
        	new Thread(new Runnable() {
        		@Override
        		public void run()
        		{
			    	//ArrayList<BookData> downloadedBooklist = bkdb.fetchBookList();

		        	CMSAgent cms = new CMSAgent(getApplicationContext());

		        	//fetch book list
		        	//cmsbooklist = cms.fetchBookListFromJSON("booklist.json");
		        	try {
			        	if (currentListMode==LIST_SEARCH) {
			        		cmsbooklist = cms.fetchBookListBySearch(currentUnitCode, searchKeyword);
			        	} else if (currentListMode==LIST_CATEGORY) {
			        		cmsbooklist = cms.fetchBookListByCategory(currentUnitCode, currentCategoryCode);
			        	} else if (globalApp.getBookList()!=null) {
			        		cmsbooklist = globalApp.getBookList();
			        	} else {
			        		cmsbooklist = cms.fetchBookList(currentUnitCode);
			        	}
		        	} catch (IOException ie) {
		        		//Toast.makeText(getBaseContext(), getResources().getString(R.string.err_ioexception), Toast.LENGTH_SHORT).show();
		        		Log.e(TAG, ie.getMessage());
		        	} catch (JSONException je) {
		        		//Toast.makeText(getBaseContext(), getResources().getString(R.string.err_ioexception), Toast.LENGTH_SHORT).show();
		        		Log.e(TAG, je.getMessage());
		        	} finally {
		        		if (cmsbooklist==null) {
		        			runOnUiThread(new Runnable() {
				        		@Override
				        		public void run()
				        		{
				        			progress.dismiss();
				        			Toast.makeText(getBaseContext(), getResources().getString(R.string.err_ioexception), Toast.LENGTH_SHORT).show();
				        		}
				        	});
		        		}
		        	}
		        	
		        	for(int i=0 ; cmsbooklist!=null && i<cmsbooklist.size() ; i++)
		        	{
		        		BookData bd = cmsbooklist.get(i);
		        		Map<String, Object> map = new HashMap<String, Object>();
		        		/*
		        		boolean isDownloaded = false;
		        		//check if book has been downloaded
		        		for (int j=0 ; j<downloadedBooklist.size() ; j++)
        		        {
		        			if (bd.getCmsBookId().equals(downloadedBooklist.get(j).getCmsBookId()))
		        			{
		        				isDownloaded = true;
		        				
		        				break;
		        			}
        		        }
        		        */
		        		map.put("cmsBookId", bd.getCmsBookId());
		        		//map.put("img", getBitmap(bd.getCoverImage()));
		        		//map.put("img", R.drawable.book);
		        		map.put("title", bd.getTitle());
		        		//map.put("info", isDownloaded?getResources().getString(R.string.msg_in_bookshelf):bd.getInfo());
		        		map.put("coverURL", bd.getCoverImage());
		        		map.put("cmsBookId", bd.getCmsBookId());
		        		
		        		bookitems.add(map);
		        	}
		        	
		        	if (currentListMode==LIST_NORMAL) {
		        		globalApp.setBookList(cmsbooklist);
		        	}
		        	
		        	runOnUiThread(new Runnable() {
		        		@Override
		        		public void run()
		        		{
		        			progress.dismiss();
		        			fetchBookListCallback();
		        		}
		        	});
        		}
        	}).start();		
        }
        else
        {
        	/*
    		ArrayList<BookData> downloadedBooklist = bkdb.fetchBookList(0, "");
	        for(int i=0 ; i<bookitems.size() ; i++)
	        {
	        	Map<String, Object> item = bookitems.get(i);
	        		//check if book has been downloaded
	        		for (int j=0 ; j<downloadedBooklist.size() ; j++)
    		        {
	        			if (item.containsKey("cmsBookId")
	        					&& item.get("cmsBookId").equals(downloadedBooklist.get(j).getCmsBookId()))
	        			{
	        				bookitems.get(i).put("info", getResources().getString(R.string.msg_in_bookshelf));
	        				break;
	        			}
    		        }
	        }
	        */
	        if (booklistAdapter!=null) booklistAdapter.notifyDataSetChanged();
        }
        
        //grid view scroll event
        GridView lv = (GridView)findViewById(R.id.containerList);
        lv.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) 
            {
            	if (currentVisibleBookItem == firstVisibleItem) return;
            	
            	currentVisibleBookItem = firstVisibleItem;
            	
            	Log.d(TAG, "onScroll, first="+String.valueOf(firstVisibleItem) + ", count="+String.valueOf(visibleItemCount));
            	if (booklistAdapter!=null) {
            		//clear book covers for invisible items
            		for (int i=0 ; i<bookitems.size() ; i++) {
            			if (i<firstVisibleItem || i>firstVisibleItem+visibleItemCount) {
            				bookitems.get(i).remove("img");
            			}
            		}
            		
            		//fetch book covers for visible items
            		if (currentFetchBookCoverTask!=null) currentFetchBookCoverTask.cancel(true);
            		currentFetchBookCoverTask = new BookListActivity.FetchBookCoverTask(act, firstVisibleItem, visibleItemCount);
            		currentFetchBookCoverTask.execute();
            	}
            }

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}
        });        
 	}

    //do after we got book list data from CMS via HTTP
    private void fetchBookListCallback()
    {
    	
        //ListView lv = (ListView)findViewById(R.id.containerList);
    	GridView lv = (GridView)findViewById(R.id.containerList);
        booklistAdapter = new SimpleAdapter(this, bookitems,
                R.layout.fsm_booklist_item, 
                new String[] { "img", "title", "info" },
                new int[] { R.id.img, R.id.title, R.id.info });
        //setListAdapter(adapter);
        booklistAdapter.setViewBinder(new BookItemViewBinder());
        lv.setAdapter(booklistAdapter);        

        //list item onclick event
        lv.setOnItemClickListener(this);
        
        /*
        ImageButton btnDownload = (ImageButton)lv.findViewById(R.id.btnDownload);
        btnDownload.setVisibility(View.GONE);
        */
        
        //start another task to fetch book cover image
        //new FetchBookCoverTask(this, 0, bookitems.size()).execute();
        currentFetchBookCoverTask = new FetchBookCoverTask(this, 0, 10);
        currentFetchBookCoverTask.execute();
    }

   //fetching book cover images
    private class FetchBookCoverTask extends AsyncTask<Void, Void, Void>
    {
    	private Activity activity;
    	private AsyncTaskListener callback;
    	private int startIdx = 0;
    	private int count = 0;
    	
    	public FetchBookCoverTask(Activity act, int startFromIdx, int fetchCount)
    	{
    		this.activity = act;
    		this.callback = (AsyncTaskListener)act;
    		this.startIdx = startFromIdx;
    		this.count = fetchCount;
    	}
        
		@Override
		protected Void doInBackground(Void... params) {
            for (int i = startIdx; i < startIdx+count && i < bookitems.size() ; i++) {
            	
            	if (bookitems.get(i).containsKey("img")) continue;
            	if (isCancelled()) break;
            	
            	String httpUrl = bookitems.get(i).get("coverURL").toString();
            	
            	//use Universal Image Loader, just put url
            	Bitmap bmp = ImageLoader.getInstance().loadImageSync(httpUrl);
            	if (bmp!=null) bookitems.get(i).put("img", bmp);
            	
/*            	
            	Bitmap bmp = null;
                
                Bitmap cache_bmp = globalApp.getBitmapFromMemCache(httpUrl);
                if (cache_bmp != null) {
                    bmp = cache_bmp;
                } else {
                	Log.d(TAG, "Download image from "+httpUrl);
                	try {
                		
                		URL url = new URL(httpUrl);
                		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                		InputStream is = conn.getInputStream();
                		//BitmapFactory.Options options = new BitmapFactory.Options();
                		bmp = BitmapFactory.decodeStream(is);
                		
//                		int w = bmp.getWidth();
//                		int h = bmp.getHeight();
//                		if (w>Constants.BOOK_COVER_SIZE || h>Constants.BOOK_COVER_SIZE) {
//                			if (w>h) {
//                				h = Math.round((float)h*(Constants.BOOK_COVER_SIZE/(float)w));
//                				w = Constants.BOOK_COVER_SIZE;
//                			} else {
//                				w = Math.round((float)w*(Constants.BOOK_COVER_SIZE/(float)h));
//                				h = Constants.BOOK_COVER_SIZE;
//                			}
//                			Log.d(TAG, "scale image to w="+String.valueOf(w)+", h="+String.valueOf(h));
//                    		bmp = Bitmap.createScaledBitmap(bmp, w, h, false);
//                		}
//                		globalApp.addBitmapToMemoryCache(httpUrl, bmp);
                		
                	} catch (Exception e) {
                		//e.printStackTrace();
                		Log.d(TAG, "download image failed, "+e.getMessage());
                	}
                }
            	if (bmp!=null) bookitems.get(i).put("img", bmp);
*/                
            	publishProgress();
            }
			return null;
        }

	     protected void onProgressUpdate(Void... progress) {
	    	 runOnUiThread(new Runnable() {
	        		@Override
	        		public void run()
	        		{
	        			booklistAdapter.notifyDataSetChanged();
	        		}
	        	});
	     }

		protected void onPostExecute(Void result) {
			callback.onTaskComplete(result);
		}
    }

	@Override
	public void onTaskComplete(Object o) {
		// TODO Auto-generated method stub
		booklistAdapter.notifyDataSetChanged();
	}
    
 	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch (parent.getId()) {
		case R.id.containerList:
			Log.d(TAG, "book item clicked, position="+String.valueOf(position));
			try {
				BookData bd = cmsbooklist.get(position);
				
				//open book detail view
				Intent intent = new Intent(getApplicationContext(), BookDetailActivity.class);
				//pass book detail information
				intent.putExtra(Constants.DETAIL_OPEN_MODE, BookDetailActivity.MODE_CMS_BOOK);
				intent.putExtra(Constants.BOOK_COVER_URL, bd.getCoverImage());
				intent.putExtra(Constants.BOOK_NAME, bd.getTitle());
				intent.putExtra(Constants.BOOK_INFO, bd.getInfo());
				intent.putExtra(Constants.BOOK_EPUB_URL, bd.getEpubDownloadURL());
				//intent.putExtra(Constants.BOOK_PDF_URL, bd.getPdfDownloadURL());
				intent.putExtra(Constants.BOOK_CMS_ID, bd.getCmsBookId());
				//pass cover image
				/*
				Bitmap bm = (Bitmap)bookitems.get(position).get("img");
				ByteArrayOutputStream bs = new ByteArrayOutputStream();
				bm.compress(Bitmap.CompressFormat.JPEG, 75, bs);
				intent.putExtra(Constants.BOOK_COVER_IMG, bs.toByteArray());
				*/				
				startActivity(intent);
				//BookListActivity.this.finish();
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}

 	}
 	
 	public void gotoBookshelf(View view)
 	{
		//goto bookshelf view
    	Intent intent = new Intent(getApplicationContext(), BookshelfActivity.class);
        startActivity(intent);
        BookListActivity.this.finish();
 	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
	    int itemId = item.getItemId();
	    switch (itemId) {
	    	case R.id.category:
	    		showCategory();
	    		return true;
	    	case R.id.home:
	    		currentListMode = LIST_NORMAL;
	    		bookitems = null;
	    		onResume();
	    		return true;
		    case R.id.bookshelf:
		    	Log.d(TAG, "Go to bookshelf");
		    	Intent intent = new Intent(getApplicationContext(), BookshelfActivity.class);
		        startActivity(intent);
		        BookListActivity.this.finish();
		    	return true;
	    }
	    return false;
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		mOptionsMenu = menu;
		
		getMenuInflater().inflate(R.menu.main, menu);
		
		//menu.findItem(R.id.home).setIcon(R.drawable.home);
		//menu.findItem(R.id.bookshelf).setIcon(R.drawable.bookshelf);
		//menu.findItem(R.id.search).setIcon(R.drawable.search);
		//menu.findItem(R.id.settings).setIcon(R.drawable.setting);
		
		// Get the SearchView and set the searchable configuration
	    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	    SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
	    // Assumes current activity is the searchable activity
	    searchView.setOnQueryTextListener(this);
	    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
	    searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
		
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
    	searchKeyword = query;
    	Log.d(TAG, "search: "+searchKeyword);
    	currentListMode = LIST_SEARCH;
        bookitems = null;
        
        //dismiss virtual keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        SearchView searchView = (SearchView) mOptionsMenu.findItem(R.id.search).getActionView();
        searchView.clearFocus();
        
        //reload book lsit
        onResume();
        
		return true;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		return false;
	}	
	
	public void showCategory() {
		Intent intent = new Intent(this, BookCategoryActivity.class);
		intent.putExtra(Constants.UNIT_CODE, currentUnitCode);
		startActivityForResult(intent, REQUEST_CATEGORY);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		Log.d(TAG, "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);

		if (data==null) return;
		
		switch(requestCode){
			case REQUEST_CATEGORY:
				Log.d(TAG, "Got activity result from category / unit list");
				if (data.getExtras().containsKey(Constants.CATEGORY_CODE))
				{
					currentListMode = LIST_CATEGORY;
					currentCategoryCode = data.getExtras().getString(Constants.CATEGORY_CODE);
					Log.d(TAG, "code=" + currentCategoryCode);
				}
		        bookitems = null;
		        //reload book lsit
		        onResume();
				break;
			default:
				break;
		}
	}		
}

