package com.fqsmobile.stkagym_vp;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
 
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String grade = getGradePrefs(); 
        TextView gradeTextView = (TextView)findViewById(R.id.grade);  //Zum Start lädt er dann direkt den String von den Einstellungen
        gradeTextView.setText(grade);
        String subgrade = getSubgradePrefs();
        TextView subgradeTextView = (TextView)findViewById(R.id.subgrade);
        if(grade.equals("Q1") || grade.equals("Eph") || grade.equals("Q2")) //Vermeidung von unerwünschten Zusätzen zur Oberstufe durch alte Einträge :D
        {
        	subgradeTextView.setText("");
        }
        else
        {
        subgradeTextView.setText(subgrade);
        }
        
        final Button button = (Button) findViewById(R.id.button_refresh);  //Refresh-Button
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Intent intent = getIntent();
            	finish();
            	startActivity(intent);
            }
            });
        }
    
    //Hier holt der sich einfach nur den gewählten String der Stufenauswahl. Selbsterkärend, denke ich.
    String ListGradePref;
    public String getGradePrefs()
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	ListGradePref = prefs.getString("grades_list", "");
    	return ListGradePref;
    	
    }
    String ListSubgradePref;
    public String getSubgradePrefs()
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	ListSubgradePref = prefs.getString("subgrades_list", "");
    	return ListSubgradePref;
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
