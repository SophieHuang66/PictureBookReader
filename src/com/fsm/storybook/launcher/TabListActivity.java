package com.fsm.storybook.launcher;

import java.util.ArrayList;
import java.util.List;

import org.readium.sdk.android.Container;
import org.readium.sdk.android.Package;
import org.readium.sdk.android.components.navigation.NavigationElement;
import org.readium.sdk.android.components.navigation.NavigationPoint;
import org.readium.sdk.android.components.navigation.NavigationTable;
import org.readium.sdk.android.launcher.ContainerHolder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.util.Log;

import com.fsm.storybook.model.Bookmark;
import com.fsm.storybook.model.Highlight;
import com.fsm.storybook.util.BookDatabase;

public class TabListActivity extends FragmentActivity {

    private static final String TAG = "TabListActivity";
	private Context context;
	private GlobalApplication globalApp;
	private BookDatabase bkdb = null;
	
    private Package pckg;
	private long containerId;
	private int bookCode;
	
	private List<NavigationElement> tocList;
	private List<Bookmark> bookmarkList;
	private List<Highlight> highlightList;
	
	  @Override
	  protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
        context = this;
        globalApp = (GlobalApplication)this.getApplicationContext();
        bkdb = new BookDatabase(getApplicationContext());
        
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras==null) {
        	finish();
        	return;
        }
        
        String bookName = extras.getString(Constants.BOOK_NAME);
        this.setTitle(bookName);
        
        bookCode = extras.getInt(Constants.BOOK_CODE);
        
        containerId = extras.getLong(Constants.CONTAINER_ID);
        Container container = ContainerHolder.getInstance().get(containerId);
        if (container == null) {
        	finish();
        	return;
        }
        
        pckg = container.getDefaultPackage();
		
        //get TOC list
        NavigationTable navigationTable = pckg.getTableOfContents();
		tocList = navigationTable.getChildren();

		//get bookmark list
		bookmarkList = bkdb.fetchBookmarkList(bookCode);
		
		//get highlight list
		highlightList = bkdb.fetchHighlightList(bookCode);
		
	    setContentView(R.layout.fsm_tab_list);

	    FragmentTabHost tabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
	    tabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);
	    //TOC tab
	    tabHost.addTab(tabHost.newTabSpec("toc")
	                          .setIndicator(getString(R.string.menu_toc)), 
	                   TocFragment.class, 
	                   null);
	    //Bookmark tab
	    tabHost.addTab(tabHost.newTabSpec("bookmark")
	                          .setIndicator(getString(R.string.menu_bookmark)), 
	                   BookmarkFragment.class, 
	                   null);
	    //Highlight tab
	    tabHost.addTab(tabHost.newTabSpec("highlight")
	                          .setIndicator(getString(R.string.menu_highlight)), 
	                   HighlightFragment.class, 
	                   null);
	  }

	  /**************************
	  * 
	  * ?????????????????????
	  * 
	  **************************/
	  public List<String> getTocList() {
		  ArrayList<String> result = new ArrayList<String>();

		  for(int i=0 ; i<tocList.size() ; i++) {
			  NavigationElement element = tocList.get(i);
			  result.add(element.getTitle());
		  }
		  
		  return result;
	  }
	  
	  public void clickTocItem(int pos) {
		  NavigationElement navigation = tocList.get(pos);
		  
		  if (navigation instanceof NavigationPoint) {
			  NavigationPoint point = (NavigationPoint) navigation;
			  Log.i(TAG, "Open content from TOC : "+point.getContent());
			  Intent i = new Intent();
			  Bundle b = new Bundle();
			  b.putString(Constants.TOC_ITEM, point.getContent());
			  i.putExtras(b);
			  setResult(RESULT_OK, i);
			  finish();
		  }
	  }

	  
	  public List<String> getBookmarkList() {
		  ArrayList<String> result = new ArrayList<String>();
		  for(int i=0 ; i<bookmarkList.size() ; i++) {
			  result.add(bookmarkList.get(i).getBookmarkName());
		  }
		  return result;
	  }
	  
	  public void clickBookmarkItem(int pos) {
		  Bookmark bm = bookmarkList.get(pos);
		  Log.i(TAG, "Open content from Bookmark : "+bm.getBookmarkName()+", "+bm.getCfi());
		  Intent i = new Intent();
		  Bundle b = new Bundle();
		  b.putString(Constants.BOOKMARK_CFI, bm.getCfi());
		  b.putString(Constants.SPINE_IDREF, bm.getSpineItem());
		  i.putExtras(b);
		  setResult(RESULT_OK, i);
		  finish();
	  }


	  public List<String> getHighlightList() {
		  ArrayList<String> result = new ArrayList<String>();
		  for(int i=0 ; i<highlightList.size() ; i++) {
			  result.add(highlightList.get(i).getContent());
		  }
		  return result;
	  }
	  
	  public void clickHighlightItem(int pos) {
		  Highlight hl = highlightList.get(pos);
		  Log.i(TAG, "Open content from Highlight : "+hl.getContent()+", "+hl.getCfi());
		  Intent i = new Intent();
		  Bundle b = new Bundle();
		  b.putString(Constants.HIGHLIGHT_CFI, hl.getCfi());
		  b.putString(Constants.SPINE_IDREF, hl.getSpineItem());
		  i.putExtras(b);
		  setResult(RESULT_OK, i);
		  finish();
	  }
	  
}
