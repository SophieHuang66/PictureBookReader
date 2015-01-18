package org.readium.sdk.android.launcher;

import java.util.Arrays;

import org.readium.sdk.android.Container;
import org.readium.sdk.android.EPub3;
import org.readium.sdk.android.launcher.model.BookmarkDatabase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.fsm.storybook.launcher.Constants;
import com.fsm.storybook.launcher.R;
import com.fsm.storybook.launcher.R.id;
import com.fsm.storybook.launcher.R.layout;
import com.fsm.storybook.launcher.R.string;

public class BookDataActivity extends Activity {

	private Context context;
	private Container container;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_data);

        context = this;
        Intent intent = getIntent();
        if (intent.getFlags() == Intent.FLAG_ACTIVITY_NEW_TASK) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String value = extras.getString(Constants.BOOK_NAME);
                getActionBar().setTitle(value);
                container = ContainerHolder.getInstance().get(extras.getLong(Constants.CONTAINER_ID));
                if (container == null) {
                	finish();
                	return;
                }
            }
        }

        initMetadata();
        initPageList();
        initBookmark();
    }

    private void initBookmark() {
        int number = BookmarkDatabase.getInstance().getBookmarks(container.getName()).size();
        final ListView bookmark = (ListView) findViewById(R.id.bookmark);
        String bookmarks = "Bookmarks (" + number + ")";
        String[] bookmark_values = new String[] { bookmarks };

        Class<?>[] classList = new Class<?>[] { BookmarksActivity.class };
        this.setListViewContent(bookmark, bookmark_values, classList);
	}

	private void initPageList() {
        final ListView pageList = (ListView) findViewById(R.id.pageList);
        String[] pageList_values = new String[] {
                getString(R.string.list_of_figures),
                getString(R.string.list_of_illustrations),
                getString(R.string.list_of_tables),
                getString(R.string.page_list),
                getString(R.string.table_of_contents) };

        Class<?>[] classList = new Class<?>[] { 
        		ListOfFiguresActivity.class,
        		ListOfIllustrationsActivity.class,
        		ListOfTablesActivity.class,
        		PageListActivity.class,
        		TableOfContentsActivity.class };
        this.setListViewContent(pageList, pageList_values, classList);
	}

	private void initMetadata() {
        final ListView metadata = (ListView) findViewById(R.id.metaData);
        String[] metadata_values = new String[] { 
        		getString(R.string.metadata),
                getString(R.string.spine_items) };

        Class<?>[] classList = new Class<?>[] { 
        		MetaDataActivity.class,
        		SpineItemsActivity.class };
        this.setListViewContent(metadata, metadata_values, classList);
	}

	private void setListViewContent(ListView view, String[] stringArray,final Class<?>[] classes) {
        BookListAdapter bookListAdapter = new BookListAdapter(this, Arrays.asList(stringArray));
        view.setAdapter(bookListAdapter);
        view.setOnItemClickListener(new ListView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                Intent intent = new Intent(context, classes[arg2]);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Constants.BOOK_NAME, container.getName());
                intent.putExtra(Constants.CONTAINER_ID, container.getNativePtr());
                
                startActivity(intent);
            }
        });
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
        initBookmark();
    }
    
    @Override
    public void onBackPressed() {
    	super.onBackPressed();
    	if (container != null) {
    		ContainerHolder.getInstance().remove(container.getNativePtr());
    		
    		// Close book (need to figure out if this is the best place...)
    		EPub3.closeBook(container);
    	}
    }

}
