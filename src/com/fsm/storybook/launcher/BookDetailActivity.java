package com.fsm.storybook.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.EPub3;
import org.readium.sdk.android.SpineItem;
import org.readium.sdk.android.launcher.ContainerHolder;
import org.readium.sdk.android.launcher.model.OpenPageRequest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fsm.storybook.model.BookData;
import com.fsm.storybook.util.ActivityUtil;
import com.fsm.storybook.util.BookDatabase;
import com.fsm.storybook.util.CMSAgent;

public class BookDetailActivity extends Activity {
	private static String TAG = "BookDataActivity";
	private static final int REQUEST_LOGIN = 1;
	public static String MODE_CMS_BOOK = "CMS";
	public static String MODE_LOCAL_BOOK = "LOCAL";

	private Context context;
	private GlobalApplication globalApp = null;
	private BookDatabase bkdb = null;
	private BookData bd = null;
	private CMSAgent cms;
    private ProgressDialog progress;
    private Bitmap currentCoverBitmap = null;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        globalApp = (GlobalApplication)this.getApplicationContext();
        bkdb = new BookDatabase(getApplicationContext());
        cms = new CMSAgent(getApplicationContext());
        setContentView(R.layout.fsm_book_detail);
        
        Intent intent = getIntent();
                
        //if (intent.getFlags() == Intent.FLAG_ACTIVITY_NEW_TASK) {
        
           Bundle extras = intent.getExtras();
           if (extras != null && extras.containsKey(Constants.DETAIL_OPEN_MODE)) {
            	String mode = extras.getString(Constants.DETAIL_OPEN_MODE);
            	Log.d(TAG, "mode="+mode);
                if (mode.equals(MODE_LOCAL_BOOK))
                {
                	//hide add to bookshelf button
                	View v_add = findViewById(R.id.btnAdd);
                	v_add.setVisibility(View.GONE);
                	//view downloaded book detail 
                	int bookCode = extras.getInt(Constants.BOOK_CODE);
                	//get book data from database
                	bd = bkdb.fetchBook(bookCode);
                	//show book detail on view
                	TextView title = (TextView)findViewById(R.id.bookTitle);
                	title.setText(bd.getTitle());
                	TextView info = (TextView)findViewById(R.id.bookDescription);
                	StringBuffer info_text = new StringBuffer(bd.getInfo());
                	/*
                	info_text.append(("\r\n")).append(
                			getString(R.string.info_progress, Math.round(bd.getCurrentPage()/1000f*100)));
                	if (bd.getExpireDateTime()!=null && !bd.getExpireDateTime().isEmpty()) {
                		info_text.append("\r\n")
                			.append(getResources().getString(R.string.info_expiretime))
                			.append(bd.getExpireDateTime()); 
                	}
                	*/
                	info.setText(info_text);
                	ImageView cover = (ImageView)findViewById(R.id.bookCover);
                	String coverPath = bkdb.getBookPath()+"/"+bd.getCoverImage();
                	cover.setImageBitmap(BitmapFactory.decodeFile(coverPath));
                	
                	if (bd.getEpubDownloadStatus()!=BookData.DOWNLOAD_STATUS_NONE) {
                    	//hide download button
                    	View v_download = findViewById(R.id.btnDownload);
                    	v_download.setVisibility(View.GONE);
                	}
                	
                	if (bd.getEpubDownloadStatus()!=BookData.DOWNLOAD_STATUS_DONE) {
                    	//hide open button
                    	View v_open = findViewById(R.id.btnOpen);
                    	v_open.setVisibility(View.GONE);
                	}
                	/*
                	if (bd.getEpubDownloadStatus()==BookData.DOWNLOAD_STATUS_FETCHING) {
                    	//hide delete button
                    	View v_delete = findViewById(R.id.btnDelete);
                    	v_delete.setVisibility(View.GONE);
                	}
                	*/
                } 
                else if (mode.equals(MODE_CMS_BOOK))
                {
                	//hide download button
                	View v_download = findViewById(R.id.btnDownload);
                	v_download.setVisibility(View.GONE);
                	
                	//hide delete button
                	View v_del = findViewById(R.id.btnDelete);
                	v_del.setVisibility(View.GONE);
                	
                	//hide open button
                	View v_open = findViewById(R.id.btnOpen);
                	v_open.setVisibility(View.GONE);
                	
                	View v_add = findViewById(R.id.btnAdd);
                	v_add.setVisibility(View.GONE);

                	//show book detail
                	TextView title = (TextView)findViewById(R.id.bookTitle);
                	title.setText(extras.getString(Constants.BOOK_NAME));
                	TextView info = (TextView)findViewById(R.id.bookDescription);
                	info.setText(extras.getString(Constants.BOOK_INFO));
                	/*
                	ImageView cover = (ImageView)findViewById(R.id.bookCover);
                	Bitmap b = BitmapFactory.decodeByteArray(
                	        getIntent().getByteArrayExtra(Constants.BOOK_COVER_IMG),
                	        0,
                	        getIntent().getByteArrayExtra(Constants.BOOK_COVER_IMG).length);        
                	cover.setImageBitmap(b);
                	*/
                	
                	String cmsBookId = getIntent().getExtras().getString(Constants.BOOK_CMS_ID);
                	//已在書櫃中
                	if (cmsBookId!=null && bkdb.bookExists(cmsBookId)) 
                	{
                    	TextView status = (TextView)findViewById(R.id.bookStatus);
                    	status.setText(getResources().getString(R.string.msg_in_bookshelf));
                    	ImageView cover = (ImageView)findViewById(R.id.bookCover);
                    	bd = bkdb.fetchBookByCMSBookId(cmsBookId);
                    	String coverPath = bkdb.getBookPath()+"/"+bd.getCoverImage();
                    	cover.setImageBitmap(BitmapFactory.decodeFile(coverPath));
                	}
                	//還沒加入書櫃
                	else
                	{
                    	//download cover image from website
                    	new Thread(new Runnable() {
                            public void run() {
                            	try {
                            		URL url = new URL(getIntent().getStringExtra(Constants.BOOK_COVER_URL));
                            		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                            		InputStream is = conn.getInputStream();
                            		currentCoverBitmap = BitmapFactory.decodeStream(is);
                            		
                            		runOnUiThread(new Runnable() {
                		        		@Override
                		        		public void run()
                		        		{
                		        			ImageView cover = (ImageView)findViewById(R.id.bookCover);
                                    		cover.setImageBitmap(currentCoverBitmap);
                                        	View v_add = findViewById(R.id.btnAdd);
                                        	v_add.setVisibility(View.VISIBLE);
                		        		}
                		        	});
                            		
                            	} catch (Exception e) {
                            		Log.e(TAG, "Load cover image failed, url="+getIntent().getStringExtra(Constants.BOOK_COVER_URL));
                            		e.printStackTrace();
                            	}
                            }
                        }).start();
                     }                	
                }

            }
        //}

    }
	
	
	public void addBook(View view)
	{
		Bundle extras = getIntent().getExtras();
		
		//check if book is already in bookshelf
		if (bkdb.bookExists(extras.getString(Constants.BOOK_CMS_ID))) {
			Log.e(TAG, "book already exists, cms book id="+extras.getString(Constants.BOOK_CMS_ID));
			return;
		}
		
		//insert a record into database
		BookData bd = new BookData();
		bd.setTitle(extras.getString(Constants.BOOK_NAME));
		bd.setInfo(extras.getString(Constants.BOOK_INFO));
		bd.setCmsBookId(extras.getString(Constants.BOOK_CMS_ID));
		bd.setEpubDownloadURL(extras.getString(Constants.BOOK_EPUB_URL));
		int bookId = bkdb.insertBook(bd);
		if (bookId<0) {
			Log.e(TAG, "insert book failed");
			return;
		}
		
		//save cover image
		/*
		Bitmap bmp = BitmapFactory.decodeByteArray(
    	        getIntent().getByteArrayExtra(Constants.BOOK_COVER_IMG),
    	        0,
    	        getIntent().getByteArrayExtra(Constants.BOOK_COVER_IMG).length); 
    	*/
		
		String filename = "cover_"+String.valueOf(bookId);
		File file = new File(bkdb.getBookPath()+"/"+filename+".jpg");
		if (file.exists()) {
			file.delete();
		}
		try {
			if (currentCoverBitmap!=null) {
				file = new File(bkdb.getBookPath(), filename + ".jpg");
				if (!file.exists()) file.createNewFile();
				OutputStream outStream = null;
				outStream = new FileOutputStream(file);
				currentCoverBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outStream);
				outStream.flush();
				outStream.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//update book data record to save cover image file name
		bd.setCoverImage(filename+".jpg");
		bkdb.updateBook(bd);
		
		BookDetailActivity.this.finish();
	}
	
	public void downloadBook(View view)
	{
		//notify cms that we are going to download this book
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				cms.notifyDownload(bd.getCmsBookId());
				return null;
			}
		}.execute();
		
		//start download process using DownloadManager
		String httpUrl = bd.getEpubDownloadURL();
    	DownloadManager.Request request = new DownloadManager.Request(Uri.parse(httpUrl));
    	request.setDescription("Download epub file of "+bd.getTitle());
    	request.setTitle(bd.getTitle());
    	// in order for this if to run, you must use the android 3.2 to compile your app
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
    	    request.allowScanningByMediaScanner();
    	    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
    	}
    	String[] parts = httpUrl.split("/");
    	String filename = String.valueOf(bd.getBookCode())+"_"+parts[parts.length-1];
    	request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

    	// get download service and enqueue file
    	DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
    	long downloadID = dm.enqueue(request);
    	
    	//update download status
    	bkdb.insertDownloadRecord(downloadID, bd.getBookCode(), "EPUB");
    	bd.setEpubFileName(filename);
    	bd.setEpubDownloadStatus(BookData.DOWNLOAD_STATUS_FETCHING);
    	bkdb.updateBook(bd);
    	
    	BookDetailActivity.this.finish();
	}

	public void deleteBook(View view)
	{
		if (bd==null) return;
		
		new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.book_delete)
        .setMessage(R.string.msg_confirm_del)
        .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            	//if book is still in download, stop
            	long downloadId = bkdb.getDownloadID(bd.getBookCode(), "EPUB");
            	if (downloadId!=-1) {
            		DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            		dm.remove(downloadId);
            		bkdb.deleteDownloadRecord(downloadId);
            	}
            	
        		//delete from database
            	bkdb.deleteBookmarksByBookCode(bd.getBookCode());
            	bkdb.deleteHighlightsByBookCode(bd.getBookCode());
            	bkdb.deleteBookByBookCode(bd.getBookCode());
            	
        		//delete files : cover image, epub...
            	if (bd.getCoverImage()!=null && !bd.getCoverImage().isEmpty()) {
            		File f = new File(bkdb.getBookPath(), bd.getCoverImage());
            		if (f.exists()) f.delete();
            	}
            	if (bd.getEpubFileName()!=null && !bd.getEpubFileName().isEmpty()) {
            		File f = new File(bkdb.getBookPath(), bd.getEpubFileName());
            		if (f.exists()) f.delete();
            	}
            	if (bd.getPdfFileName()!=null && !bd.getPdfFileName().isEmpty()) {
            		File f = new File(bkdb.getBookPath(), bd.getPdfFileName());
            		if (f.exists()) f.delete();
            	}

                //Stop the activity
                BookDetailActivity.this.finish(); 
            }

        })
        .setNegativeButton(R.string.generic_cancel, null)
        .show();
	}
	
	public void openBook(View view)
	{
		//check if this user has right to open this book
		//if (!"IAP".equals(bd.getFromUnit()) && bd.getValidDays()==-1) return;
		
        Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.BOOK_NAME, bd.getTitle());
        intent.putExtra(Constants.BOOK_CODE, bd.getBookCode());

        Container container = EPub3.openBook(bkdb.getBookPath()+"/"+bd.getEpubFileName());
        ContainerHolder.getInstance().put(container.getNativePtr(), container);
		long containerId = container.getNativePtr();
		intent.putExtra(Constants.CONTAINER_ID, containerId);
		
		try {
			OpenPageRequest openPageRequest;
			/*
			for(int i=0 ; container.getPackages()!=null && i<container.getPackages().size() ; i++)
			{
				Log.i(TAG, "package "+ String.valueOf(i) + " : " + container.getPackages().get(i).getPackageID());
			}
			*/
			
			SpineItem firstSpineItem = container.getDefaultPackage().getSpineItems().get(0);
			Log.i(TAG, "Open webview at : "+firstSpineItem.getHref());
			if(bd.getCurrentPage()>0 && bd.getCurrentSpineItem()!=null && !bd.getCurrentSpineItem().isEmpty()) {
				ActivityUtil util = new ActivityUtil(this);
				int page = Math.round(bd.getCurrentPage() / 1000f * bd.getSpineItemPageCount(String.valueOf(util.getScreenOrientation()), bd.getCurrentSpineItem()));
				openPageRequest = OpenPageRequest.fromIdrefAndIndex(bd.getCurrentSpineItem(), page); 
			} else {
				openPageRequest = OpenPageRequest.fromIdref(firstSpineItem.getIdRef());
			}
			intent.putExtra(Constants.OPEN_PAGE_REQUEST_DATA, openPageRequest.toJSON().toString());
	    	startActivity(intent);
	    	BookDetailActivity.this.finish();
		} catch (JSONException e) {
			Log.e(TAG, ""+e.getMessage(), e);
		} catch (Exception e) {
			e.printStackTrace();
			runOnUiThread(new Runnable() {
        		@Override
        		public void run()
        		{
        			Toast toast = Toast.makeText(getBaseContext(), getResources().getString(R.string.err_epubformat), Toast.LENGTH_SHORT);
        			toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
        			toast.show();
        		}
			});			
		}
		
	}	
	
	public void closePage(View view)
	{
		BookDetailActivity.this.finish();
	}

 
}
