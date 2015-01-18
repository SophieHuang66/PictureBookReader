package com.fsm.storybook.util;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.fsm.storybook.model.BookData;
import com.fsm.storybook.model.Bookmark;
import com.fsm.storybook.model.Highlight;


public class BookDatabase {

	public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private DBHelper opener; 
    private SQLiteDatabase db;
    Context context;
    
    public BookDatabase (Context context) {
        this.context = context;
        this.opener = DBHelper.getInstance(context);
        db = opener.getWritableDatabase();
    }
    
    public String getBookPath()
    {
    	return context.getFilesDir().getAbsolutePath()+"/Storybook";
    }
	    
    public String getDateString() {
    	Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT);
        String formattedDate = df.format(c.getTime());
        return formattedDate;
    }
    
    //for download progress
    public void insertDownloadRecord(long downloadID, int bookCode, String fileType)
    {
    	ContentValues values = new ContentValues();
    	values.put("DownloadID", downloadID);
    	values.put("BookCode", bookCode);
    	values.put("FileType", fileType.toUpperCase());
    	db.insert("Download", null, values);
    }
    public void deleteDownloadRecord(long downloadID)
    {
    	String sql = String.format(Locale.US,"DELETE FROM Download where DownloadID = %d", downloadID);
    	db.execSQL(sql); 
    }
    public long getDownloadID(int bookCode, String fileType)
    {
    	long result = -1;
    	String sql = String.format("SELECT * FROM Download WHERE BookCode=%d AND FileType='%s'", bookCode, fileType.toUpperCase());
    	Cursor cursor = db.rawQuery(sql, null);
    	if (cursor.moveToNext()) {
    		result = cursor.getLong(0);
    	}
    	cursor.close();
    	return result;
    }
    public int getBookCodeByDownloadID(long downloadID)
    {
    	int result = -1;
    	String sql = String.format("SELECT * FROM Download WHERE DownloadID=%d", downloadID);
    	Cursor cursor = db.rawQuery(sql, null);
    	if (cursor.moveToNext()) {
    		result = cursor.getInt(1);
    	}
    	cursor.close();
    	return result;
    }
    public int getDownloadCount()
    {
    	int result = 0;
    	String sql = String.format("SELECT count(*) FROM Download");
    	Cursor cursor = db.rawQuery(sql, null);
    	if (cursor.moveToNext()) {
    		result = cursor.getInt(0);
    	}
    	cursor.close();
    	return result;
    }
    
    //for books
    public boolean bookExists(String cmsBookId)
    {
    	boolean result = false;
    	String sql = String.format("SELECT * FROM Book WHERE CMSBookID='%s'", cmsBookId);
    	Cursor cursor = db.rawQuery(sql, null);
    	if (cursor.moveToNext()) {
    		result = true;
    	}
    	cursor.close();
    	return result;
    }
    
    public BookData fetchBookByCMSBookId(String cmsBookId) {
    	BookData bd = null;
    	String sql = String.format("SELECT * FROM Book WHERE CMSBookID='%s'", cmsBookId);
    	Cursor cursor = db.rawQuery(sql, null);
    	if (cursor.moveToNext()) {
    		bd = readCursorData(cursor);
    	}
    	cursor.close();
    	return bd;
    }

    public BookData fetchBook(int bookCode) {
    	BookData bd = null;
    	String sql = "SELECT * FROM Book WHERE BookCode="+String.valueOf(bookCode);
    	Cursor cursor = db.rawQuery(sql, null);
    	if (cursor.moveToNext()) {
    		bd = readCursorData(cursor);
    	}
    	cursor.close();
    	return bd;
    }
    
    public ArrayList<BookData> fetchBookList() {
    	return fetchBookList(0, null);
    }
    
    public ArrayList<BookData> fetchBookList(int sortType, String key) {
    	ArrayList<BookData> bookList = new ArrayList<BookData>();
    	String orderBy = "";
    	if (sortType==0)		orderBy = "";
    	else if (sortType==1) 	orderBy = " ORDER BY Title";
    	else if (sortType==2)	orderBy = " ORDER BY LastOpenDateTime DESC";
    	String condition = "";
    	if (!(key==null || key.isEmpty())) {
    		condition =String.format(" WHERE LOWER(Title) like '%%%s%%' ", key.toLowerCase());
    	}
    	String selectSql = "SELECT * from Book "+condition+orderBy;
        Cursor cursor = db.rawQuery(selectSql, null);
        while (cursor.moveToNext()) {
        	BookData bd = readCursorData(cursor);
        	bookList.add(bd);
        }
		cursor.close();
		return bookList;    	
    } 
    
    private BookData readCursorData(Cursor cursor) {
    	BookData bd = new BookData();
    	bd.setBookCode(cursor.getInt(0));
    	bd.setCmsBookId(cursor.getString(1));
    	bd.setTitle(cursor.getString(2));
    	bd.setInfo(cursor.getString(3));
    	bd.setCoverImage(cursor.getString(4));
    	bd.setEpubFileName(cursor.getString(5));
    	bd.setPdfFileName(cursor.getString(6));
    	bd.setEpubDownloadURL(cursor.getString(7));
    	bd.setPdfDownloadURL(cursor.getString(8));
    	bd.setEpubDownloadStatus(cursor.getInt(9));
    	bd.setPdfDownloadStatus(cursor.getInt(10));
    	bd.setCreateDateTime(cursor.getString(11));
    	bd.setFontSize(cursor.getInt(12));
    	bd.setSpreadCount(cursor.getInt(13));
    	bd.setCurrentSpineItem(cursor.getString(14));
    	bd.setCurrentPage(cursor.getInt(15));
    	bd.setCurrentPageInSpineItem(cursor.getInt(16));
    	//bd.setTotalPageCount(cursor.getInt(17));
    	//Log.d("BookDatabase", cursor.getString(18));
    	try {
    		if (cursor.getString(18)!=null && !cursor.getString(18).isEmpty()) {
    			JSONObject jo = new JSONObject(cursor.getString(18));
    			for(String orientation : Arrays.asList(BookData.ORIENTATION_LANDSCAPE, BookData.ORIENTATION_PORTRAIT)) {
    				if (!jo.has(orientation)) continue;
    				JSONObject spinePageCount = jo.getJSONObject(orientation);
        			Map<String, Integer> pageCountList = new HashMap<String, Integer>();
        			Iterator<String> it = spinePageCount.keys();
        			while (it.hasNext()) {
        				String spine = it.next();
        				//Log.d("BookDatabase", String.format("put into map, key=%s, value=%d", spine, spinePageCount.getInt(spine)));
        				pageCountList.put(spine, Integer.valueOf(spinePageCount.getInt(spine)));
        			}
        			bd.setSpineItemPageCount(orientation, pageCountList);
    			}
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	bd.setReadCount(cursor.getInt(19));
    	bd.setLastOpenDateTime(cursor.getString(20));
    	bd.setFromUnit(cursor.getString(21));
    	bd.setProductId(cursor.getString(22));
    	bd.setPrice(cursor.getFloat(23));
    	bd.setVersion(cursor.getString(24));
    	bd.setValidDays(cursor.getInt(25));
    	bd.setExpireDateTime(cursor.getString(26));
    	return bd;
    }
    
    public int insertBook(BookData bd) {
    	String dateInString = this.getDateString(); 

    	String expireDateInString = "";
    	if (bd.getValidDays()>0) {
    		bd.refreshExpireDateTime();
    	}
    	
    	ContentValues values = new ContentValues();
    	values.put("CMSBookID", bd.getCmsBookId());
    	values.put("Title", bd.getTitle());
    	values.put("Info", bd.getInfo());
    	values.put("CoverImage", bd.getCoverImage());
    	values.put("EpubFileName", bd.getEpubFileName());
    	values.put("PdfFileName", bd.getPdfFileName());
    	values.put("EpubDownloadURL", bd.getEpubDownloadURL());
    	values.put("PdfDownloadURL", bd.getPdfDownloadURL());
    	values.put("EpubDownloadStatus", bd.getEpubDownloadStatus());
    	values.put("PdfDownloadStatus", bd.getPdfDownloadStatus());
    	values.put("CreateDateTime", dateInString);
    	values.put("FontSize", bd.getFontSize());
    	values.put("SpreadCount", bd.getSpreadCount());
    	values.put("CurrentSpineItem", bd.getCurrentSpineItem());
    	values.put("CurrentPage", bd.getCurrentPage());
    	values.put("CurrentPageInSpineItem", bd.getCurrentPageInSpineItem());
    	//values.put("TotalPageCount", bd.getTotalPageCount());
    	values.put("TotalPageCount", 0);
    	JSONObject jo = new JSONObject();
    	for(String orientation : Arrays.asList(BookData.ORIENTATION_LANDSCAPE, BookData.ORIENTATION_PORTRAIT)) {
    		if (bd.getSpineItemPageCount(orientation)!=null) {
    			JSONObject joPageCount = new JSONObject(bd.getSpineItemPageCount(orientation));
    			try { jo.put(orientation, joPageCount); } catch(Exception e) { e.printStackTrace(); }
    		}
    	}
    	values.put("SpineItemPageCount", jo.toString());
    	values.put("ReadCount", bd.getReadCount());
    	values.put("FromUnit", bd.getFromUnit());
    	values.put("ProductId", bd.getProductId());
    	values.put("Price", bd.getPrice());
    	values.put("Version", bd.getVersion());
    	values.put("ValidDays", bd.getValidDays());
    	values.put("ExpireDateTime", bd.getExpireDateTime());
    	
    	long inserted_id = db.insert("Book", null, values);
    	int result = -1;
    	if (inserted_id>0) {
    		result = Integer.parseInt(String.valueOf(inserted_id));
    		bd.setBookCode(result);
    	}
    	return result;
    }

    public void updateBook(BookData bd) {
    	ContentValues values = new ContentValues();
    	if (bd.getCmsBookId()!=null && !bd.getCmsBookId().isEmpty()) values.put("CMSBookID", bd.getCmsBookId());
    	if (bd.getTitle()!=null && !bd.getTitle().isEmpty()) values.put("Title", bd.getTitle());
    	if (bd.getInfo()!=null && !bd.getInfo().isEmpty()) values.put("Info", bd.getInfo());
    	if (bd.getCoverImage()!=null && !bd.getCoverImage().isEmpty()) values.put("CoverImage", bd.getCoverImage());
    	if (bd.getEpubFileName()!=null && !bd.getEpubFileName().isEmpty()) values.put("EpubFileName", bd.getEpubFileName());
    	if (bd.getPdfFileName()!=null && !bd.getPdfFileName().isEmpty()) values.put("EpubFileName", bd.getPdfFileName());
    	if (bd.getEpubDownloadURL()!=null && !bd.getEpubDownloadURL().isEmpty()) values.put("EpubDownloadURL", bd.getEpubDownloadURL());
    	if (bd.getPdfDownloadURL()!=null && !bd.getPdfDownloadURL().isEmpty()) values.put("PdfDownloadURL", bd.getPdfDownloadURL());
    	if (bd.getEpubDownloadStatus()!=-1) values.put("EpubDownloadStatus", bd.getEpubDownloadStatus());
    	if (bd.getPdfDownloadStatus()!=-1) values.put("PdfDownloadStatus", bd.getPdfDownloadStatus());
    	if (bd.getCreateDateTime()!=null && !bd.getCreateDateTime().isEmpty()) values.put("CreateDateTime", bd.getCreateDateTime());
    	if (bd.getFontSize()!=-1) values.put("FontSize", bd.getFontSize());
    	if (bd.getSpreadCount()!=-1) values.put("SpreadCount", bd.getSpreadCount());
    	if (bd.getCurrentSpineItem()!=null && !bd.getCurrentSpineItem().isEmpty()) values.put("CurrentSpineItem", bd.getCurrentSpineItem());
    	if (bd.getCurrentPage()!=-1) values.put("CurrentPage", bd.getCurrentPage());
    	if (bd.getCurrentPageInSpineItem()!=-1) values.put("CurrentPageInSpineItem", bd.getCurrentPageInSpineItem());
    	//if (bd.getTotalPageCount()!=-1) values.put("TotalPageCount", bd.getTotalPageCount());
    	if (bd.getSpineItemPageCount()!=null && !bd.getSpineItemPageCount().isEmpty()) {
        	JSONObject jo = new JSONObject();
        	for(String orientation : Arrays.asList(BookData.ORIENTATION_LANDSCAPE, BookData.ORIENTATION_PORTRAIT)) {
        		if (bd.getSpineItemPageCount(orientation)!=null) {
        			JSONObject joPageCount = new JSONObject(bd.getSpineItemPageCount(orientation));
        			try { jo.put(orientation, joPageCount); } catch(Exception e) { e.printStackTrace(); }
        		}
        	}
        	values.put("SpineItemPageCount", jo.toString());
    	}
    	if (bd.getReadCount()!=-1) values.put("ReadCount", bd.getReadCount());
    	if (bd.getLastOpenDateTime()!=null && !bd.getLastOpenDateTime().isEmpty()) values.put("LastOpenDateTime", bd.getLastOpenDateTime());
    	if (bd.getFromUnit()!=null && !bd.getFromUnit().isEmpty()) values.put("FromUnit", bd.getFromUnit());
    	if (bd.getProductId()!=null && !bd.getProductId().isEmpty()) values.put("ProductId", bd.getProductId());
    	if (bd.getPrice()!=-1) values.put("Price", bd.getPrice());
    	if (bd.getVersion()!=null && !bd.getVersion().isEmpty()) values.put("Version", bd.getVersion());
    	if (bd.getValidDays()!=-1) values.put("ValidDays", bd.getValidDays());
    	if (bd.getExpireDateTime()!=null && !bd.getExpireDateTime().isEmpty()) values.put("ExpireDateTime", bd.getExpireDateTime());
 	
    	String where = String.format("BookCode=%d",bd.getBookCode());
    	db.update("Book", values, where, null);
    }    
    
    public void deleteBookByBookCode(int bookCode) {
    	String sql = String.format(Locale.US,"DELETE FROM Book where BookCode = %d",bookCode);
    	db.execSQL(sql); 
    	//delete bookmarks
    	//delete highlights
    	//delete epub / pdf file
    }

    
    //for bookmarks
    public ArrayList<Bookmark> fetchBookmarkList(int bookCode) {
    	ArrayList<Bookmark> bookmarkList = new ArrayList<Bookmark>();
    	String selectSql = String.format(Locale.US,"SELECT * from Bookmark WHERE BookCode=%d",bookCode);
        Cursor cursor = db.rawQuery(selectSql, null);
        while (cursor.moveToNext()) {
        	Bookmark bm = new Bookmark();
        	bm.setBookCode(cursor.getInt(0));
        	bm.setCode(cursor.getInt(1));
        	bm.setBookmarkName(cursor.getString(2));
        	bm.setCfi(cursor.getString(3));
        	bm.setSpineItem(cursor.getString(4));
        	bm.setOffsetInSpineItem(cursor.getInt(5));
        	bm.setPage(cursor.getInt(6));
        	bm.setCreateDateTime(cursor.getString(7));

        	bookmarkList.add(bm);
        }
		cursor.close();
		return bookmarkList;    	
    } 
    
    public int insertBookmark(Bookmark bm) {
        String dateInString = this.getDateString();  

        ContentValues values = new ContentValues();
    	values.put("BookCode", bm.getBookCode());
    	values.put("BookmarkName", bm.getBookmarkName());
    	values.put("Cfi", bm.getCfi());
    	values.put("SpineItem", bm.getSpineItem());
    	values.put("OffsetInSpineItem", bm.getOffsetInSpineItem());
    	values.put("Page", bm.getPage());
    	values.put("CreateDateTime",dateInString);
    	long inserted_id = db.insert("Bookmark", null, values);
    	return Integer.parseInt(String.valueOf(inserted_id));
    }
    
    public void deleteBookmarkByCode(int code) {
    	String sql = String.format(Locale.US,"DELETE FROM Bookmark where Code = %d",code);
    	db.execSQL(sql); 
    }

    public void deleteBookmarksByBookCode(int bookCode) {
    	String sql = String.format(Locale.US,"DELETE FROM Bookmark where BookCode = %d",bookCode);
    	db.execSQL(sql); 
    }
    
    //for highlights
    public ArrayList<Highlight> fetchHighlightList(int bookCode) {
    	
    	ArrayList<Highlight> highlightList = new ArrayList<Highlight>();
    	String selectSql = String.format(Locale.US,"SELECT * from Highlight WHERE BookCode=%d",bookCode);
        Cursor cursor = db.rawQuery(selectSql, null);
        while (cursor.moveToNext()) {
        	Highlight hl = new Highlight();
        	hl.setBookCode(cursor.getInt(0));
        	hl.setCode(cursor.getInt(1));
        	hl.setCfi(cursor.getString(2));
        	hl.setSpineItem(cursor.getString(3));
        	hl.setStartOffsetInSpineItem(cursor.getInt(4));
        	hl.setEndOffsetInSpineItem(cursor.getInt(5));
        	hl.setPage(cursor.getInt(6));
        	hl.setColorCode(cursor.getString(7));
        	hl.setCreateDateTime(cursor.getString(8));
        	hl.setContent(cursor.getString(9));
        	hl.setNote(cursor.getInt(10)==1?true:false);
        	hl.setNote(cursor.getString(11));

        	highlightList.add(hl);
        }
		cursor.close();
		return highlightList;    	
    }
    
    public int insertHighlight(Highlight hl) {
        String dateInString = this.getDateString();  

        ContentValues values = new ContentValues();
    	values.put("BookCode", hl.getBookCode());
    	values.put("Cfi", hl.getCfi());
    	values.put("SpineItem", hl.getSpineItem());
    	values.put("StartOffsetInSpineItem", hl.getStartOffsetInSpineItem());
    	values.put("EndOffsetInSpineItem", hl.getEndOffsetInSpineItem());
    	values.put("Page", hl.getPage());
    	values.put("ColorCode", hl.getColorCode());
    	values.put("CreateDateTime",dateInString);
    	values.put("Content",hl.getContent());
    	values.put("IsNote", hl.isNote()?1:0);
    	values.put("Note", hl.getNote());
    	
    	long inserted_id = db.insert("Highlight", null, values);
    	return Integer.parseInt(String.valueOf(inserted_id));
    }    
    
    public void deleteHighlightByCode(int code) {
    	String sql = String.format(Locale.US,"DELETE FROM Highlight where Code = %d",code);
    	db.execSQL(sql); 
    }    
    
    public void deleteHighlightsByBookCode(int bookCode) {
    	String sql = String.format(Locale.US,"DELETE FROM Highlight where BookCode = %d",bookCode);
    	db.execSQL(sql); 
    }

}
