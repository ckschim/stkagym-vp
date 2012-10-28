package com.fqsmobile.stkagym_vp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
	 
    @SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	addPreferencesFromResource(R.xml.preferences);
    }
    
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key){
    	//TODO: Funktioniert nicht. Keine Ahnung warum.
    	if(key.equals("grades_list")) {
    		String grade = sharedPreferences.getString("grades_list", "");
    		if(grade.equals("EF") || grade.equals("Q1") || grade.equals("Q2"))
    			getPreferenceScreen().findPreference("subgrades_list").setEnabled(false);
    		else
    			getPreferenceScreen().findPreference("subgrades_list").setEnabled(true);
    	}
    		
    };
}