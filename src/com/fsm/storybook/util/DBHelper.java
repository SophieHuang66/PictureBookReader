package com.fsm.storybook.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DBHelper extends SQLiteOpenHelper {
	
	/*
    public DBHelper(Context context) {
        super(context, "Books.db", null, 1);
    }
	*/
	private static DBHelper mInstance = null;
	private static Context context = null;

	private static final String DATABASE_NAME = "Books.db";
	private static final int DATABASE_VERSION = 1;

	public static DBHelper getInstance(Context ctx) {
		context = ctx.getApplicationContext();
		if (mInstance == null) {
	    	mInstance = new DBHelper(ctx.getApplicationContext());
	    }
	    return mInstance;
	}

	private DBHelper(Context ctx) {
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
	}		
	
    // onCreate is called once if database not exists.
    @Override
    public void onCreate(SQLiteDatabase db) {
    	Log.w("EPub","EpubReaderDB onCreate");	    	
        db.execSQL(this.stringFromAssets("sql/book.sql"));
        db.execSQL(this.stringFromAssets("sql/highlight.sql"));
        db.execSQL(this.stringFromAssets("sql/bookmark.sql"));
        db.execSQL(this.stringFromAssets("sql/download.sql"));
        //db.execSQL(this.stringFromAssets("sql/paging.sql"));
        //db.execSQL(this.stringFromAssets("sql/setting.sql"));
        //String sql = "INSERT INTO Setting(BookCode,FontName,FontSize,LineSpacing,Foreground,Background,Theme,Brightness,TransitionType,LockRotation) " +
        //		"						VALUES(0,'',2,-1,-1,-1,0,1,2,1)";
        //db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}		
    
	public String stringFromAssets(String fileName) {
	    StringBuilder ReturnString = new StringBuilder();
	    InputStream fIn = null;
	    InputStreamReader isr = null;
	    BufferedReader input = null;
	    try {
	        fIn = context.getResources().getAssets().open(fileName, Context.MODE_PRIVATE);
	        isr = new InputStreamReader(fIn);
	        input = new BufferedReader(isr);
	        String line = "";
	        while ((line = input.readLine()) != null) {
	            ReturnString.append(line);
	        }
	    } catch (Exception e) {
	        e.getMessage();
	    } finally {
	        try {
	            if (isr != null)
	                isr.close();
	            if (fIn != null)
	                fIn.close();
	            if (input != null)
	                input.close();
	        } catch (Exception e2) {
	            e2.getMessage();
	        }
	    }
	    return ReturnString.toString();
	}
    
}	