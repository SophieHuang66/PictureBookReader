package com.fsm.storybook.launcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.readium.sdk.android.launcher.BookListAdapter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.fsm.storybook.model.BookCategory;
import com.fsm.storybook.util.CMSAgent;

public class BookCategoryActivity extends Activity implements AsyncTaskListener
{

    private static final String TAG = "BookCategoryActivity";
	private Context context;
	private GlobalApplication globalApp;
    private ProgressDialog progress;
    private List<Map<String, String>> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.table_of_contents);
        context = this;
        globalApp = (GlobalApplication)this.getApplicationContext();
        
        //if (globalApp.getBookCategoryList()==null || globalApp.getUnitList()==null) {
        if (globalApp.getBookCategoryList()==null) {
        	new FetchBookCategoryTask(this).execute();  
        } else {
        	renderList();
        }
	}

    private class FetchBookCategoryTask extends AsyncTask<Void, Void, Void>
    {
    	private AsyncTaskListener callback;
    	
    	public FetchBookCategoryTask(Activity act)
    	{
    		this.callback = (AsyncTaskListener)act;
    	}
        
		@Override
		protected Void doInBackground(Void... params) 
		{
			CMSAgent cms = new CMSAgent(getApplicationContext());
			String unitCode = getIntent().getExtras().getString(Constants.UNIT_CODE);
			try {
				globalApp.setBookCategoryList(cms.fetchBookCategoryList(unitCode));
				//globalApp.setUnitList(cms.fetchUnitList());
			} catch(Exception e) {
				Log.e(TAG, e.getMessage());
			}
			return null;
        }

		@Override
		protected void onPostExecute(Void result) 
		{
			callback.onTaskComplete(result);
		}
    }

	@Override
	public void onTaskComplete(Object o) {
		renderList();
	}

    private void renderList () {
        final ListView v_list = (ListView) findViewById(R.id.tableOfContents);
        
        List<String> list = new ArrayList<String>();
        for(int i=0 ; i<globalApp.getBookCategoryList().size() ; i++) {
        	list.add(globalApp.getBookCategoryList().get(i).getName());
        }

        BookListAdapter bookListAdapter = new BookListAdapter(this, list);
        v_list.setAdapter(bookListAdapter);
        v_list.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            	BookCategory cat = globalApp.getBookCategoryList().get(arg2);
            	Intent i = new Intent();
            	Bundle b = new Bundle();
            	b.putString(Constants.CATEGORY_CODE, cat.getCode());
            	i.putExtras(b);
            	setResult(RESULT_OK, i);
            	finish();
            }
        });
    }
    
}
