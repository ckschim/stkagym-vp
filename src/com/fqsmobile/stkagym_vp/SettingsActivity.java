package com.fqsmobile.stkagym_vp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String grade =  prefs.getString("grades_list", "");
		checkGrade(grade);
		
		final ListPreference gradesList = (ListPreference) findPreference("grades_list");

		gradesList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				checkGrade(newValue.toString());
				return true;
			}
		});
	}

	private void checkGrade(String grade) {
		@SuppressWarnings("deprecation")
		final ListPreference subgradesList = (ListPreference) findPreference("subgrades_list");
		subgradesList.setEnabled(!(grade.equals("EF") || grade.equals("Q1") || grade.equals("Q2")));
		subgradesList.setValue(""); // Leeren
	}
}