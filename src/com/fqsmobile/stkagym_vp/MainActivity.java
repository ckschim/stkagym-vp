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
		/* Auslesen der Einstellungen beim Start */
		String grade = getGradePrefs();
		TextView gradeTextView = (TextView) findViewById(R.id.grade);
		gradeTextView.setText(grade);
		String subgrade = getSubgradePrefs();
		TextView subgradeTextView = (TextView) findViewById(R.id.subgrade);
		subgradeTextView.setText(subgrade);

		final Button button = (Button) findViewById(R.id.button_refresh); // Refresh-Button
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				/*
				 * Hier wird werden wiederdie Einstellungen ausgelesen
				 */
				String grade = getGradePrefs();
				TextView gradeTextView = (TextView) findViewById(R.id.grade);
				gradeTextView.setText(grade);
				String subgrade = getSubgradePrefs();
				TextView subgradeTextView = (TextView) findViewById(R.id.subgrade);
				subgradeTextView.setText(subgrade);
			}
		});
	}

	/*
	 * Hier holt der sich einfach nur den gew�hlten String der Stufenauswahl.
	 * Selbsterk�rend, denke ich.
	 */

	public String getGradePrefs() {
		String ListGradePref;
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		ListGradePref = prefs.getString("grades_list", "");
		return ListGradePref;

	}

	public String getSubgradePrefs() {
		String ListSubgradePref;
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		ListSubgradePref = prefs.getString("subgrades_list", "");
		return ListSubgradePref;
	}

	/* Erstellung des Menu-Button Men�s */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	/*
	 * Verhalten bei Auswahl eines Items des Men�s
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent activity_intent = new Intent();
			activity_intent.setClass(getApplicationContext(),
					SettingsActivity.class);
			startActivity(activity_intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
