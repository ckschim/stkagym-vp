package com.fqsmobile.stkagym_vp;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		final ListPreference gradesList = (ListPreference) findPreference("grades_list");
		final ListPreference subgradesList = (ListPreference) findPreference("subgrades_list");

		gradesList
				.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						final String val = newValue.toString();
						subgradesList.setEnabled(!(val.equals("EF")
								|| val.equals("Q1") || val.equals("Q2")));
						subgradesList.setValue(""); // Leeren
						return true;
					}
				});
	}
}