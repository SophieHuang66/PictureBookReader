package com.fsm.storybook.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.security.auth.login.LoginException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.fsm.storybook.launcher.R;
import com.fsm.storybook.model.BookCategory;
import com.fsm.storybook.model.BookData;
import com.fsm.storybook.model.Unit;

public class CMSAgent {

	private static final String TAG = "CMSAgent";
	private static final String CMS_URL = "http://cms.test.org/query";
	private Context activityContext;
	
	public CMSAgent(Context context) {
		activityContext = context;
	}
	

	public ArrayList<Unit> fetchUnitList() throws IOException, JSONException {
		ArrayList<Unit> unitList = null;

		JSONArray jsonArray = fetchDataFromCMS("m=list");
		if (jsonArray!=null) {
			unitList = new ArrayList<Unit>();
	        for(int i=0 ; i<jsonArray.length() ; i++)
	        {
	        	JSONObject jo = jsonArray.getJSONObject(i);
	        	if (jo.has("log")) break;
	        	
	        	Unit u = new Unit();
	        	u.setCode(jo.getString("code"));
	        	u.setName(jo.getString("name"));
	        	//u.setDays(jo.getInt("day"));
	        	
	        	unitList.add(u);
	        }			
		}
		
		return unitList;
	}
	
	public ArrayList<BookCategory> fetchBookCategoryList(String unitCode) throws IOException, JSONException {
		ArrayList<BookCategory> categoryList = null;

		JSONArray jsonArray = fetchDataFromCMS("m=category&c="+URLEncoder.encode(unitCode,"UTF-8"));
		if (jsonArray!=null) {
			categoryList = new ArrayList<BookCategory>();
	        for(int i=0 ; i<jsonArray.length() ; i++)
	        {
	        	JSONObject jo = jsonArray.getJSONObject(i);
	        	if (jo.has("log")) break;
	        	
	        	BookCategory cat = new BookCategory();
	        	cat.setCode(jo.getString("code"));
	        	cat.setName(jo.getString("classify"));
	        	
	        	categoryList.add(cat);
	        }			
		}
		
		return categoryList;
	}
	
	public ArrayList<BookData> fetchBookList(String unitCode) throws IOException, JSONException {
		ArrayList<BookData> bookList = null;

		JSONArray boolist_json = fetchDataFromCMS("m=list&c="+URLEncoder.encode(unitCode,"UTF-8"));
		if (boolist_json!=null) {
			bookList = JSONArrayToBookList(boolist_json);
		}
		
		return bookList;
	}
	
	
	public ArrayList<BookData> fetchBookListBySearch(String keyword) throws IOException, JSONException {
		ArrayList<BookData> bookList = null;
		JSONArray boolist_json = null;
		boolist_json = fetchDataFromCMS("m=search&k="+URLEncoder.encode(keyword,"UTF-8"));
		
		if (boolist_json!=null) {
			bookList = JSONArrayToBookList(boolist_json);
		}
		
		return bookList;
	}

	
	public ArrayList<BookData> fetchBookListBySearch(String unitCode, String keyword) throws IOException, JSONException {
		ArrayList<BookData> bookList = null;
		JSONArray boolist_json = null;
		boolist_json = fetchDataFromCMS("m=search&c="+unitCode+"&k="+URLEncoder.encode(keyword,"UTF-8"));
		
		if (boolist_json!=null) {
			bookList = JSONArrayToBookList(boolist_json);
		}
		
		return bookList;
	}
	
	
	public ArrayList<BookData> fetchBookListByCategory(String unitCode, String categoryCode) throws IOException, JSONException {
		ArrayList<BookData> bookList = null;
		JSONArray boolist_json = null;
		boolist_json = fetchDataFromCMS("m=list&c="+unitCode+"&t="+categoryCode);

		if (boolist_json!=null) {
			bookList = JSONArrayToBookList(boolist_json);
		}
		
		return bookList;
	}
	
	public ArrayList<BookData> fetchBookListFromJSON(String filename) {
		ArrayList<BookData> bookList = null;
		
		//for testing, get book list from local json file
		AssetManager manager = activityContext.getAssets();
		try {
			InputStream is = manager.open(filename);
			int size = is.available();
	        byte[] buffer = new byte[size];
	        is.read(buffer);
	        is.close();
	        String jsonStr = new String(buffer, "UTF-8");
	        JSONArray jsonArray = new JSONArray(jsonStr);
	        bookList = JSONArrayToBookList(jsonArray);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return bookList;
	}
	
	protected ArrayList<BookData> JSONArrayToBookList(JSONArray jsonArray) throws JSONException
	{
		ArrayList<BookData> bookList = new ArrayList<BookData>();
        for(int i=0 ; i<jsonArray.length() ; i++)
        {
        	JSONObject jo = jsonArray.getJSONObject(i);
        	if (jo.has("log")) break;
        	
        	BookData bd = new BookData();
        	bd.setCmsBookId(jo.getString("bookcaseid"));
        	bd.setCoverImage(jo.getString("icsrc"));
        	bd.setTitle(jo.getString("bookname"));
        	bd.setEpubDownloadURL(jo.getString("bksrc"));
        	
        	StringBuffer sb = new StringBuffer();
        	if (jo.has("au") && !jo.getString("au").isEmpty()) 
        		sb.append(activityContext.getResources().getString(R.string.info_author))
        			.append(jo.getString("au"))
        			.append("\r\n");
        	
        	if (jo.has("publish") && !jo.getString("publish").isEmpty()) 
        		sb.append(activityContext.getResources().getString(R.string.info_publisher))
        			.append(jo.getString("publish"))
        			.append("\r\n");
        	
        	if (jo.has("publishTime") && !jo.getString("publishTime").isEmpty()) 
        		sb.append(activityContext.getResources().getString(R.string.info_publishtime))
    			.append(jo.getString("publishTime"))
    			.append("\r\n");
        	
        	bd.setInfo(sb.toString());
        	
        	bookList.add(bd);
        }
		return bookList;
	}
	
	public void notifyDownload(String CMSBookId)
	{
		try {
			//JSONArray result = fetchDataFromCMS(String.format("type=add&bid=%s", CMSBookId));
			fetchDataFromCMS(String.format("type=add&bid=%s", CMSBookId));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private JSONArray fetchDataFromCMS(String queryString) throws IOException, JSONException 
	{
	    HttpClient client = new DefaultHttpClient();
	    String url = CMS_URL+"?"+queryString;
	    Log.d(TAG, "fetch data from "+url);
	    HttpGet request = new HttpGet(url);
	    HttpResponse response;
	    String result = null;
	    try {
	        response = client.execute(request);         
	        HttpEntity entity = response.getEntity();

	        if (entity != null) {

	            if (response.getStatusLine().getStatusCode() == 200) {
		            InputStream instream = entity.getContent();
		            result = convertStreamToString(instream);
		            // now you have the string representation of the HTML request
		            Log.d(TAG, "CMS Resopnse: " + result);
		            instream.close();
	            } else {
	            	Log.d(TAG, String.format("CMS response: %s %s", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase()));
	            }

	        }
	        // Headers
	        /*
	        org.apache.http.Header[] headers = response.getAllHeaders();
	        for (int i = 0; i < headers.length; i++) {
	            System.out.println(headers[i]);
	        }
	        */
	    } catch (ClientProtocolException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
	    } catch (IOException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
	        throw e1;
	    }
	    
	    try {
		    JSONArray jsonResult = new JSONArray(result);
		    return jsonResult;
	    } catch (Exception e) {
	    	return null;
	    }
	}

	private static String convertStreamToString(InputStream is)
	{
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();

	    String line = null;
	    try {
	        while ((line = reader.readLine()) != null) {
	            sb.append(line + "\n");
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            is.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    return sb.toString();
	}	

}
