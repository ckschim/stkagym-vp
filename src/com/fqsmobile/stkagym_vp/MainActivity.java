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
		/* Anwenden der Einstellungen beim Start */
		applySettings();

		final Button button = (Button) findViewById(R.id.button_refresh); // Refresh-Button
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				refreshData();
			}
		});
	}

	private void refreshData() {
		// Daten auslesen
	}

	public void applySettings() {
		// (Geänderte) Einstellungen anwenden
		String grade = getGradePrefs();
		String subgrade = getSubgradePrefs();

		String identifier = grade.concat(subgrade);
		TextView gradeTextView = (TextView) findViewById(R.id.grade);
		gradeTextView.setText(identifier);
		refreshData();
	}

	/*
	 * Hier holt der sich einfach nur den gewählten String der Stufenauswahl.
	 * Selbsterkärend, denke ich.
	 */

	public String getGradePrefs() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		return prefs.getString("grades_list", "");

	}

	public String getSubgradePrefs() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		return prefs.getString("subgrades_list", "");
	}

	/* Erstellung des Menu-Button Menüs */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	/*
	 * Verhalten bei Auswahl eines Items des Menüs
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent settings_intent = new Intent();
			settings_intent.setClass(getApplicationContext(), SettingsActivity.class);
			startActivity(settings_intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public boolean isInitialized() {
		// TODO Auto-generated method stub
		return false;
	}

}
