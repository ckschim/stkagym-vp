package com.fqsmobile.stkagym_vp;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_settings:
            	Intent activity_intent = new Intent();
            	activity_intent.setClass(getApplicationContext(), SettingsActivity.class);
            	startActivity(activity_intent); 
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
   
}
