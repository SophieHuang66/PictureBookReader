package com.fsm.storybook.launcher;

import com.fsm.storybook.launcher.R;
import com.fsm.storybook.launcher.R.id;
import com.fsm.storybook.launcher.R.layout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;

public class ViewerSettingActivity extends Activity
implements OnClickListener {
	private Context context;
	private static String TAG = "ViewerSettingActivity";

	private TextView fontSizeText;
	private CheckBox synSpreadChkBox;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.viewer_settings);
        
        //fontSizeText = (TextView)findViewById(R.id.fontSize);
        synSpreadChkBox = (CheckBox)findViewById(R.id.syntheticSpread);
        
        //set onclick listener
        View v_ok = findViewById(R.id.ok);
        v_ok.setOnClickListener(this);
        
        View v_cancel = findViewById(R.id.cancel);
        v_cancel.setOnClickListener(this);
        
        //View v_incFontSize = findViewById(R.id.btnIncrease);
        //v_incFontSize.setOnClickListener(this);

        //View v_decFontSize = findViewById(R.id.btnDecrease);
        //v_decFontSize.setOnClickListener(this);

        //put default values
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        //int fontSize = extras.getInt(Constants.SETTING_FONT_SIZE);
        int spreadCount = extras.getInt(Constants.SETTING_SPREAD_COUNT);
        
        //fontSizeText.setText(String.valueOf(fontSize));
        synSpreadChkBox.setChecked(spreadCount==2);
        
        //hide synthetic spread checkbox for vertical writing mode
        String mode = extras.getString(Constants.SETTING_MODE);
        if (mode.equals(Constants.SETTING_MODE_VERTICAL_BOOK)) {
        	synSpreadChkBox.setVisibility(View.INVISIBLE);
        }
    }

	@Override
	public void onClick(View v) {
		switch (v.getId())
		{
			case R.id.ok:
				returnSettings();
				break;
			case R.id.cancel:
				finish();
				break;
			/*
			case R.id.btnDecrease:
				decreaseFontSize();
				break;
			case R.id.btnIncrease:
				increaseFontSize();
				break;
			*/
		}
		
	}	
	
	private void returnSettings()
	{
		Intent i = new Intent();
		Bundle b = new Bundle();
		//b.putInt(Constants.SETTING_FONT_SIZE, Integer.parseInt(fontSizeText.getText().toString()));
		b.putInt(Constants.SETTING_SPREAD_COUNT, synSpreadChkBox.isChecked()?2:1);
		i.putExtras(b);
		setResult(RESULT_OK, i);
		finish();
	}
	
	/*
	private void increaseFontSize()
	{
		int fontSize = parseString(fontSizeText.getText().toString(), 100);
		fontSizeText.setText(String.valueOf(fontSize+20));
	}
	private void decreaseFontSize()
	{
		
		int fontSize = parseString(fontSizeText.getText().toString(), 100);
		if (fontSize>20) fontSizeText.setText(String.valueOf(fontSize-20));	
	}

	private int parseString(String s, int defaultValue) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			Log.e(TAG, ""+e.getMessage(), e);
		}
		return defaultValue;
	}
	*/
}
