package com.fsm.storybook.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.res.Configuration;

import com.fsm.storybook.util.BookDatabase;

public class BookData {
	
	public static int DOWNLOAD_STATUS_NONE = 0;
	public static int DOWNLOAD_STATUS_FETCHING = 1;
	public static int DOWNLOAD_STATUS_DONE = 2;
	
	public static String ORIENTATION_LANDSCAPE = String.valueOf(Configuration.ORIENTATION_LANDSCAPE);
	public static String ORIENTATION_PORTRAIT = String.valueOf(Configuration.ORIENTATION_PORTRAIT);
	
	private int bookCode;			//??????????????????????????????????????????
	private String cmsBookId;		//?????????CMS???ID
	private String coverImage;		//????????????
	private String title;			//??????
	private String info;			//????????????
	private String epubFileName;		//???local???epub?????????
	private String pdfFileName;			//???local???pdf?????????
	private String epubDownloadURL;			//epub?????????URL
	private String pdfDownloadURL;			//pdf?????????URL
	private int epubDownloadStatus = DOWNLOAD_STATUS_NONE;	//epub????????????
	private int pdfDownloadStatus = DOWNLOAD_STATUS_NONE;	//pdf????????????
	private String createDateTime;			//???????????????yyyy-MM-dd HH:mm:ss???
	
	private int fontSize = 100;			//???????????????????????????%
	private int spreadCount = 1;		//landscape?????????????????????1 or 2???
	private String currentSpineItem;	//??????????????? spine item ???idref???
	private int currentPage = 1;		//?????????????????????????????????????????????=>??????????????????1000???????????????????????????1000??????????????????
	private int currentPageInSpineItem = 1;		//??????????????????????????????spine item??????=>??????????????????1000???????????????????????????1000??????????????????
	private int totalPageCount = 0;		//?????????=>????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
	private Map<String, Map<String, Integer>> spineItemPageCountMap = new HashMap<String, Map<String, Integer>>();	//??????spineitem?????????, ???landscape/portrait
	private int readCount = 0;			//??????????????????
	private String lastOpenDateTime;	//???????????????????????????yyyy-MM-dd HH:mm:ss???

	private String fromUnit;		//????????????, IAP=???APP Store??????
	private String productId;		//??????????????????APP Store???????????????????????????
	private float price = 0;		//??????
	private String version;			//??????
	private int validDays = -1;		//????????????
	private String expireDateTime;	//???????????????yyyy-MM-dd HH:mm:ss???
	
	public String getCoverImage() {
		return coverImage;
	}
	public void setCoverImage(String coverImage) {
		this.coverImage = coverImage;
	}

	public String getEpubFileName() {
		return epubFileName;
	}
	public void setEpubFileName(String epubFileName) {
		this.epubFileName = epubFileName;
	}

	public int getFontSize() {
		return fontSize;
	}
	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}
	public int getSpreadCount() {
		return spreadCount;
	}
	public void setSpreadCount(int spreadCount) {
		this.spreadCount = spreadCount;
	}
	public String getCurrentSpineItem() {
		return currentSpineItem;
	}
	public void setCurrentSpineItem(String currentSpineItem) {
		this.currentSpineItem = currentSpineItem;
	}
	public int getCurrentPage() {
		return currentPage;
	}
	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}
	/*
	public int getTotalPageCount() {
		return totalPageCount;
	}
	*/
	public int getTotalPageCount(String orientation) {
		int result = 0;
		if (spineItemPageCountMap!=null && spineItemPageCountMap.containsKey(orientation))
		{
			Map<String, Integer> items = spineItemPageCountMap.get(orientation);
			Iterator<Integer> it = items.values().iterator();
			while(it.hasNext()) {
				result += it.next().intValue();
			}
		}
		return result;
	}
	/*
	public void setTotalPageCount(int totalPageCount) {
		this.totalPageCount = totalPageCount;
	}
	*/
	public int getReadCount() {
		return readCount;
	}
	public void setReadCount(int readCount) {
		this.readCount = readCount;
	}
	public String getCreateDateTime() {
		return createDateTime;
	}
	public void setCreateDateTime(String createDateTime) {
		this.createDateTime = createDateTime;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int getBookCode() {
		return bookCode;
	}
	public void setBookCode(int bookCode) {
		this.bookCode = bookCode;
	}
	public String getCmsBookId() {
		return cmsBookId;
	}
	public void setCmsBookId(String cmsBookId) {
		this.cmsBookId = cmsBookId;
	}
	public int getCurrentPageInSpineItem() {
		return currentPageInSpineItem;
	}
	public void setCurrentPageInSpineItem(int currentPageInSpineItem) {
		this.currentPageInSpineItem = currentPageInSpineItem;
	}
	public String getLastOpenDateTime() {
		return lastOpenDateTime;
	}
	public void setLastOpenDateTime(String lastOpenDateTime) {
		this.lastOpenDateTime = lastOpenDateTime;
	}
	public String getInfo() {
		return info;
	}
	public void setInfo(String info) {
		this.info = info;
	}
	public String getPdfFileName() {
		return pdfFileName;
	}
	public void setPdfFileName(String pdfFileName) {
		this.pdfFileName = pdfFileName;
	}
	public String getEpubDownloadURL() {
		return epubDownloadURL;
	}
	public void setEpubDownloadURL(String epubDownloadURL) {
		this.epubDownloadURL = epubDownloadURL;
	}
	public String getPdfDownloadURL() {
		return pdfDownloadURL;
	}
	public void setPdfDownloadURL(String pdfDownloadURL) {
		this.pdfDownloadURL = pdfDownloadURL;
	}
	public int getPdfDownloadStatus() {
		return pdfDownloadStatus;
	}
	public void setPdfDownloadStatus(int pdfDownloadStatus) {
		this.pdfDownloadStatus = pdfDownloadStatus;
	}
	public int getEpubDownloadStatus() {
		return epubDownloadStatus;
	}
	public void setEpubDownloadStatus(int epubDownloadStatus) {
		this.epubDownloadStatus = epubDownloadStatus;
	}
	public String getFromUnit() {
		return fromUnit;
	}
	public void setFromUnit(String fromUnit) {
		this.fromUnit = fromUnit;
	}
	public String getProductId() {
		return productId;
	}
	public void setProductId(String productId) {
		this.productId = productId;
	}
	public float getPrice() {
		return price;
	}
	public void setPrice(float price) {
		this.price = price;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public int getValidDays() {
		return validDays;
	}
	public void setValidDays(int validDays) {
		this.validDays = validDays;
	}
	public String getExpireDateTime() {
		return expireDateTime;
	}
	public void setExpireDateTime(String expireDateTime) {
		this.expireDateTime = expireDateTime;
	}
	public void refreshExpireDateTime() {
    	String expireDateInString = "";
    	if (this.validDays>0) {
    		Calendar c = Calendar.getInstance();
    		c.add(Calendar.DATE, this.validDays);
    		SimpleDateFormat df = new SimpleDateFormat(BookDatabase.DATE_TIME_FORMAT);
    		expireDateInString = df.format(c.getTime());
    	}
    	this.expireDateTime = expireDateInString;
	}
	
	public Map<String, Map<String, Integer>> getSpineItemPageCount() {
		return this.spineItemPageCountMap;
	}
	public Map<String, Integer> getSpineItemPageCount(String orientation) {
		if (spineItemPageCountMap!=null && spineItemPageCountMap.containsKey(orientation))
			return spineItemPageCountMap.get(orientation);
		return null;
	}
	public int getSpineItemPageCount(String orientation, String spineItem) {
		int result = 0;
		if (spineItemPageCountMap!=null && spineItemPageCountMap.containsKey(orientation)
				&& spineItemPageCountMap.get(orientation).containsKey(spineItem)) {
				result = spineItemPageCountMap.get(orientation).get(spineItem).intValue();
		}
		return result;
	}
	public void clearTotalPageCount(String orientation) {
		if (spineItemPageCountMap!=null && spineItemPageCountMap.containsKey(orientation))
		{
			spineItemPageCountMap.remove(orientation);
		}
	}
	public void clearTotalPageCount() {
		spineItemPageCountMap=null;
	}
	public void setSpineItemPageCount(String orientation, Map<String, Integer> spineItemPageCount) {
		if (this.spineItemPageCountMap==null) {
			this.spineItemPageCountMap = new HashMap<String, Map<String, Integer>>();
		}
		spineItemPageCountMap.put(orientation, spineItemPageCount);
	}
	public void setSpineItemPageCount(String orientation, String spineItem, int pageCount) {
		if (this.spineItemPageCountMap==null) {
			this.spineItemPageCountMap = new HashMap<String, Map<String, Integer>>();
		}
		if (!spineItemPageCountMap.containsKey(orientation)) spineItemPageCountMap.put(orientation, new HashMap<String, Integer>());
		spineItemPageCountMap.get(orientation).put(spineItem, Integer.valueOf(pageCount));
	}
}
