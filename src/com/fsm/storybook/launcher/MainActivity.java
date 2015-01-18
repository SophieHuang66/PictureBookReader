package com.fsm.storybook.launcher;

import java.io.File;

import com.fsm.storybook.launcher.R;
import com.fsm.storybook.launcher.R.layout;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.LruCache;

public class MainActivity extends Activity {

	private static final int STOPSPLASH = 0;
    private static final long SPLASHTIME = 500;

    // handler for splash screen
    private Handler splashHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case STOPSPLASH:
                /*
            	Intent intent = new Intent(getApplicationContext(),
                        ContainerList.class);
                */
            	Intent intent = new Intent(getApplicationContext(),
                        BookListActivity.class);
                startActivity(intent);
                MainActivity.this.finish();
                break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //create folder for books
        Context context = getApplicationContext();
        File context_dir = context.getFilesDir();
		File file = new File(context_dir, "Storybook");
		if (!file.exists()) file.mkdir();
        

        Message msg = new Message();
        msg.what = STOPSPLASH;
        splashHandler.sendMessageDelayed(msg, SPLASHTIME);
    }
    


}
