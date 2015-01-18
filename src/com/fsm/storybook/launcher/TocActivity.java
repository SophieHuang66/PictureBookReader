package com.fsm.storybook.launcher;

import java.util.ArrayList;
import java.util.List;

import org.readium.sdk.android.launcher.BookListAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.fsm.storybook.model.Epub3Package;
import com.fsm.storybook.model.Epub3TocItem;

public class TocActivity extends Activity {

    private static final String TAG = "TocActivity";
	private Context context;
	private GlobalApplication globalApp;
	private List<Epub3TocItem> tocItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.table_of_contents);
        context = this;
        globalApp = (GlobalApplication)this.getApplicationContext();
        Epub3Package pkg = globalApp.getEpub3Package();
        
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras==null || pkg==null) {
        	finish();
        	return;
        }

        this.setTitle(extras.getString(Constants.BOOK_NAME));

        final ListView v_toclist = (ListView) findViewById(R.id.tableOfContents);
        tocItems = pkg.getTocItems();
        List<String> list = new ArrayList<String>();
        for(Epub3TocItem item : tocItems) {
        	list.add(item.getTitle());
        }
        BookListAdapter bookListAdapter = new BookListAdapter(this, list);
        v_toclist.setAdapter(bookListAdapter);
        v_toclist.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            	Epub3TocItem tocItem = tocItems.get(arg2);
            	Log.i(TAG, "Open from TOC : "+tocItem.getHref());
            	Intent i = new Intent();
            	Bundle b = new Bundle();
            	b.putString(Constants.TOC_ITEM, tocItem.getHref());
            	i.putExtras(b);
            	setResult(RESULT_OK, i);
            	finish();
            }
        });
        
	}


}
