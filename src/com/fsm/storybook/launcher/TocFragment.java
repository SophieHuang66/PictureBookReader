package com.fsm.storybook.launcher;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TocFragment extends Fragment {
	  //private String value = "";
	  private Context context;
	  private TabListActivity tabListActivity;
	  
	  @Override
	  public void onAttach(Activity activity) {
	    super.onAttach(activity);
	    context = activity.getApplicationContext();
	    tabListActivity = (TabListActivity)activity;
	  }

	  @Override
	  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    return inflater.inflate(R.layout.fsm_simplelistview, container, false);
	  }
	  
	  @Override
	  public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    ListView listView = (ListView)this.getView().findViewById(R.id.datalist);
	    List<String> tocList = tabListActivity.getTocList();
	    
	    String[] values = new String[tocList.size()];
	    for(int i=0 ; i<tocList.size() ; i++) {
	    	values[i] = tocList.get(i);
	    }
	    
	    ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.fsm_listview, R.id.listTextView,values); 
	    listView.setAdapter(adapter);

	    listView.setAdapter(adapter); 
	    listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	tabListActivity.clickTocItem(position);
            }
	    });
	 }
}
