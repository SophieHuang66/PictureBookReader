/**
 * 
 */
package com.fsm.storybook.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.FileUtils;
import org.readium.sdk.android.EPub3;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.fsm.storybook.model.BookData;
import com.fsm.storybook.util.ActivityUtil;
import com.fsm.storybook.util.BookDatabase;
/**
 * @author chtian
 * 
 */
public class BookshelfActivity extends Activity 
implements OnItemClickListener, AsyncTaskListener {
	private static String TAG = "BookshelfActivity";
	private static final int REQUEST_LOGIN = 1;
	
	private Activity act;
	private Context context;
    private GlobalApplication globalApp;
    private ActivityUtil util;
	private BookDatabase bkdb;
    private Menu mOptionsMenu;

	private ArrayList<BookData> booklist;
	private List<Map<String, Object>> bookitems;
	private SimpleAdapter booklistAdapter;
    private int currentVisibleBookItem = -1;
    private FetchBookCoverTask currentFetchBookCoverTask = null;
    
	private DownloadManager dm;
    private BroadcastReceiver receiver;
    private Timer progressTimer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fsm_bookshelf);
        act = this;
        context = this;
        bkdb = new BookDatabase(getApplicationContext());
        globalApp = (GlobalApplication)this.getApplicationContext();
        util = new ActivityUtil(this);
        
        getActionBar().setDisplayShowCustomEnabled(true);
        //getActionBar().setDisplayShowHomeEnabled(false);
        //getActionBar().setDisplayShowTitleEnabled(false);

        //setup download broadcast message receiver
        dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        receiver = new DownloadStatusReceiver();
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        
        // Loads the native lib and sets the path to use for cache
        EPub3.setCachePath(getCacheDir().getAbsolutePath());
    }
    
    public void onResume()
    {
    	super.onResume();
    	
        //final ListView lv = (ListView) findViewById(R.id.containerList);
    	final GridView lv = (GridView) findViewById(R.id.containerList);
        boolean isDownloading = false;
        bookitems = new ArrayList<Map<String, Object>>();
        booklist = bkdb.fetchBookList(0, "");
        
        if (booklist.size()==0) {
        	Toast toast = Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_bookshelf_empty), Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
			toast.show();
        	return;
        }
        
        for(int i=0 ; i<booklist.size() ; i++)
        {
        		BookData bd = booklist.get(i);
        		Map<String, Object> map = new HashMap<String, Object>();
        		map.put("epubDownloadStatus", bd.getEpubDownloadStatus());
        		//map.put("img", R.drawable.book);
        		/*
        		String coverPath = bkdb.getBookPath()+"/"+bd.getCoverImage();
            	map.put("img", BitmapFactory.decodeFile(coverPath));
            	*/
        		map.put("title", bd.getTitle());
        		map.put("bookCode", bd.getBookCode());
        		if (bd.getEpubDownloadStatus()==BookData.DOWNLOAD_STATUS_NONE) {
        			map.put("info", getResources().getString(R.string.msg_not_downloaded));
        		} else if (bd.getEpubDownloadStatus()==BookData.DOWNLOAD_STATUS_FETCHING) {
        			map.put("info", getResources().getString(R.string.msg_downloading));
        			isDownloading = true;
        		} else {
        			//map.put("info", bd.getInfo());
        			map.put("info", "");
        		}
        		bookitems.add(map);
        }
        
        booklistAdapter = new SimpleAdapter(this, bookitems,
                R.layout.fsm_booklist_item, 
                new String[] { "img", "title", "info" },
                new int[] { R.id.img, R.id.title, R.id.info });
        //setListAdapter(adapter);
        booklistAdapter.setViewBinder(new BookItemViewBinder());
        lv.setAdapter(booklistAdapter);        

        //list item onclick event
        lv.setOnItemClickListener(this); 
 
        //grid view scroll event
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
            		currentFetchBookCoverTask = new BookshelfActivity.FetchBookCoverTask(act, firstVisibleItem, visibleItemCount);
            		currentFetchBookCoverTask.execute();
            	}
            }

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}
        });   

        currentFetchBookCoverTask = new FetchBookCoverTask(this, 0, 10);
        currentFetchBookCoverTask.execute();

        //run a thread to refresh download status
        if (isDownloading) {
        	progressTimer = new Timer();
        	progressTimer.schedule(new TimerTask() { 
        		//boolean stillDownloading = false;
                @Override
                public void run() {
               		for (int i=0 ; i<bookitems.size() ; i++)
               		{
               			Map<String, Object> item = bookitems.get(i);
               			Integer dlStatus = Integer.parseInt(item.get("epubDownloadStatus").toString());
               			if (dlStatus==BookData.DOWNLOAD_STATUS_FETCHING) {
               				long downloadId = bkdb.getDownloadID(Integer.parseInt(item.get("bookCode").toString()), "EPUB");
               				Query query = new Query();
               				query.setFilterById(downloadId);
               				Cursor c = dm.query(query);
            				BookData bd = bkdb.fetchBook(Integer.parseInt(item.get("bookCode").toString()));
               				if (c.moveToFirst()) {
               					int columnIndexBytes = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);                					
               					int downloadedBytes = c.getInt(columnIndexBytes);
                				int columnIndexTotalBytes = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                				int totalBytes = c.getInt(columnIndexTotalBytes);
                				float f_percent = ((float)downloadedBytes) / ((float)totalBytes) * 100;
                				int percent = Float.valueOf(f_percent).intValue();
                				Log.d(TAG, String.format("download of %s : %d%%, %d received / %d total.",item.get("title"), percent, downloadedBytes, totalBytes));
                				bookitems.get(i).put("info", getResources().getString(R.string.msg_downloading)+"..."+
                									(percent>=0?String.format("%d%%", percent):getResources().getString(R.string.msg_download_progress_unknown)));
                			} else {
                				//can not find download status
                				Log.e(TAG, "No download status for "+bd.getTitle());
                				bd.setEpubDownloadStatus(BookData.DOWNLOAD_STATUS_DONE);
                				item.put("epubDownloadStatus", String.valueOf(BookData.DOWNLOAD_STATUS_DONE));
                				bookitems.set(i, item);
                			}
               				c.close();
               				bkdb.updateBook(bd);
                		}
               		}                    
               		runOnUiThread(new Runnable(){
               			@Override
               			public void run(){
               				booklistAdapter.notifyDataSetChanged();
               			}
               		});
               		/*
               		if (!stillDownloading) {
               			progressTimer.cancel();
               		}
               		*/
                }
            }, 0, 1000);        	
        } else {
        	if (progressTimer!=null) progressTimer.cancel();
        }
    }
    
    private class DownloadStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                Query query = new Query();
                query.setFilterById(downloadId);
                Cursor c = dm.query(query);
                if (c.moveToFirst()) {
                    int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = c.getInt(columnIndex);
                    int bookCode = bkdb.getBookCodeByDownloadID(downloadId);
                    BookData bd = bkdb.fetchBook(bookCode);
                    if (status==DownloadManager.STATUS_SUCCESSFUL) {
                    	//move epub file from download directory to book folder
                        int columnIndexURI = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                        Uri fileURI = Uri.parse(c.getString(columnIndexURI));
                        File downloadedFile = new File(fileURI.getPath());
                        File dstFile = new File(bkdb.getBookPath(), bd.getEpubFileName());
                        try 
                        {
                        	if (dstFile.exists()) dstFile.delete();
                            FileUtils.moveFile(downloadedFile, dstFile);
                            bd.setEpubDownloadStatus(BookData.DOWNLOAD_STATUS_DONE);
                        } 
                        catch (IOException e) 
                        {
                            e.printStackTrace();
                            bd.setEpubDownloadStatus(BookData.DOWNLOAD_STATUS_NONE);
                            Log.e(TAG, "Move epub failed, reason: "+e.getMessage());
                        }
                    } else if (status==DownloadManager.STATUS_FAILED) {
                        bd.setEpubDownloadStatus(BookData.DOWNLOAD_STATUS_NONE);
                        int columnIndexReason = c.getColumnIndex(DownloadManager.COLUMN_REASON);
                        Log.d(TAG, "download failed, reason :"+c.getString(columnIndexReason));
                        /*
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.err_download),
                        	     Toast.LENGTH_SHORT).show();
                       	*/
                    }
                    c.close();
                    bkdb.updateBook(bd);
                    bkdb.deleteDownloadRecord(downloadId);
                    
                    //已經沒有下載中書籍，解除下載事件listener，解除進度檢查timer
                    if (bkdb.getDownloadCount()==0) {
                    	//unregisterReceiver(receiver);
                    	progressTimer.cancel();
                    }
                    
                    //update view
                    for(int i=0 ; i<bookitems.size() ; i++)
                    {
                    	if (Integer.parseInt(bookitems.get(i).get("bookCode").toString())==bookCode)
                    	{
                    		//bookitems.get(i).put("info", bd.getInfo());
                    		switch (status)
                    		{
                    			case DownloadManager.STATUS_FAILED:
                            		bookitems.get(i).put("info", getResources().getString(R.string.err_download));
                    				break;
                    			case DownloadManager.STATUS_SUCCESSFUL:
                    			default:
                            		bookitems.get(i).put("info", "");
                            		break;
                    		}
                    	}
                    }
                    booklistAdapter.notifyDataSetChanged();
                }
                
            }
        }
    };
    
    
    /*
    public void clickDownloadIcon(View v)
    {
    	Log.d(TAG, "download button clicked...");
    }
	*/

 	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		Log.d(TAG, "item clicked, position="+String.valueOf(position));
		try {
			BookData bd = booklist.get(position);
			
			//open book detail view
			Intent intent = new Intent(getApplicationContext(), BookDetailActivity.class);
			//pass book detail information
			intent.putExtra(Constants.DETAIL_OPEN_MODE, BookDetailActivity.MODE_LOCAL_BOOK);
			intent.putExtra(Constants.BOOK_CODE, bd.getBookCode());
			
			startActivity(intent);
			//BookListActivity.this.finish();
		} catch (Exception e) {
			e.printStackTrace();
		}
 	}    
    
 	public void gotoBooklist(View view)
 	{
		//goto bookshelf view
    	Intent intent = new Intent(getApplicationContext(), BookListActivity.class);
        startActivity(intent);
        BookshelfActivity.this.finish();
 	}
 	
 	@Override
 	protected void onDestroy()
 	{
 	    try {
 	    	unregisterReceiver(receiver);
 	    } catch(Exception e) {
 	    	
 	    }
  	    super.onDestroy();
 	}
 	
 	@Override
 	protected void onPause()
 	{
 		if (progressTimer!=null) progressTimer.cancel();
 		super.onPause();
 	}
 	
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
	    int itemId = item.getItemId();
	    switch (itemId) {
	    	case R.id.home:
		    	Log.d(TAG, "Go to bookstore");
		    	Intent intent = new Intent(getApplicationContext(), BookListActivity.class);
		        startActivity(intent);
		        BookshelfActivity.this.finish();
		    	return true;
	    }
	    return false;
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		mOptionsMenu = menu;
		
		getMenuInflater().inflate(R.menu.main, menu);
		
		menu.findItem(R.id.category).setVisible(false);
		//menu.findItem(R.id.home).setIcon(R.drawable.home);
		//menu.findItem(R.id.bookshelf).setIcon(R.drawable.bookshelf);
		menu.findItem(R.id.bookshelf).setVisible(false);
		//menu.findItem(R.id.search).setIcon(R.drawable.search);
		menu.findItem(R.id.search).setVisible(false);
		//menu.findItem(R.id.settings).setIcon(R.drawable.setting);
		
		return true;
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
	    	Log.d(TAG, "Go to bookstore");
	    	Intent intent = new Intent(getApplicationContext(), BookListActivity.class);
	        startActivity(intent);
	        BookshelfActivity.this.finish();
            return true;
        }
        
        return super.onKeyDown(keyCode, event);
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
            	
        		BookData bd = booklist.get(i);
        		String coverPath = bkdb.getBookPath()+"/"+bd.getCoverImage();
        		bookitems.get(i).put("img", BitmapFactory.decodeFile(coverPath));
            	
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
	
}
