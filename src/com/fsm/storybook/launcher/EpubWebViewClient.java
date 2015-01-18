package com.fsm.storybook.launcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.readium.sdk.android.ManifestItem;
import org.readium.sdk.android.launcher.util.HTMLUtil;
import org.readium.sdk.android.Package;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class EpubWebViewClient extends WebViewClient {

	protected static final String ASSET_PREFIX = "file:///android_asset/readium-shared-js/";
	protected static final String HTTP = "http";
	protected static final String UTF_8 = "utf-8";
	protected static final String TAG = "EpubWebViewClient";
	protected Package mPackage;

    public EpubWebViewClient(Package pkg) {
    	super();
    	mPackage = pkg;
    }
    
	@Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
    }

    @Override
    public void onPageFinished(WebView view, String url) {
    }

    @Override
    public void onLoadResource(WebView view, String url) {
		Log.d(TAG, "onLoadResource: " + url);
    	String cleanedUrl = cleanResourceUrl(url);
		//String cleanedUrl = url;
    	byte[] data = mPackage.getContent(cleanedUrl);
        if (data != null && data.length > 0) {
        	ManifestItem item = mPackage.getManifestItem(cleanedUrl);
        	String mimetype = (item != null) ? item.getMediaType() : null;
        	view.loadData(new String(data), mimetype, UTF_8);
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
		Log.d(TAG, "shouldOverrideUrlLoading: " + url);
		return false;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
		Log.d(TAG, "shouldInterceptRequest: " + url);
		
		if (url.startsWith("res:")) return new WebResourceResponse(null, UTF_8, new ByteArrayInputStream("".getBytes()));
		
		Uri uri = Uri.parse(url);
        if (uri.getScheme().equals("file")) {
            String cleanedUrl = cleanResourceUrl(url);
            Log.d(TAG, url+" => "+cleanedUrl);
            InputStream data = mPackage.getInputStream(cleanedUrl);
            ManifestItem item = mPackage.getManifestItem(cleanedUrl);
            if (item != null && item.isHtml()) {
                byte[] binary;
                try {
                    binary = new byte[data.available()];
                    data.read(binary);
                    data.close();
                    data = new ByteArrayInputStream(HTMLUtil.htmlByReplacingMediaURLsInHTML(new String(binary),
                            cleanedUrl, "PackageUUID").getBytes());
                } catch (IOException e) {
                    Log.e(TAG, ""+e.getMessage(), e);
                }
            }
            String mimetype = (item != null) ? item.getMediaType() : null;
            return new WebResourceResponse(mimetype, UTF_8, data);
        } else if(uri.getScheme().equals("http")){
        	return super.shouldInterceptRequest(view, url);
        }

        try {
            URLConnection c = new URL(url).openConnection();
            return new WebResourceResponse(null, UTF_8, c.getInputStream());
        } catch (MalformedURLException e) {
            Log.e(TAG, ""+e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, ""+e.getMessage(), e);
        }
        return new WebResourceResponse(null, UTF_8, new ByteArrayInputStream("".getBytes()));
    }
    
    protected String cleanResourceUrl(String url) {
        String cleanUrl = url.replace(ASSET_PREFIX, "");
        //Log.d(TAG, "cleaned Url => "+cleanUrl);
        cleanUrl = (cleanUrl.startsWith(mPackage.getBasePath())) ? cleanUrl.replaceFirst(mPackage.getBasePath(), "") : cleanUrl;
        //Log.d(TAG, "cleaned Url => "+cleanUrl);
        int indexOfSharp = cleanUrl.indexOf('#');
        if (indexOfSharp >= 0) {
            cleanUrl = cleanUrl.substring(0, indexOfSharp);
            //Log.d(TAG, "cleaned Url => "+cleanUrl);
        }
        return cleanUrl;
    }
    
}