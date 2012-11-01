package com.fqsmobile.stkagym_vp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
	String identifier;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (android.os.Build.VERSION.SDK_INT >= 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
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
		// Daten auslesen. 1337 h4xx02 57y13. <--- :D
		TextView substTextView = (TextView) findViewById(R.id.substitution_data);
		TextView dateTextView = (TextView) findViewById(R.id.substDate);
		String res = "";
		String date = "";
		String pattern = "\\<FONT FACE\\=\"Arial\"\\>\\<H3\\>\\<CENTER\\>V(.*)\\<\\/CENTER\\>";
		Pattern datePattern = Pattern.compile(pattern);
		String data = getHttpText("http://www.gymnasium-kamen.de/pages/vp.html");
		Matcher dateMatcher = datePattern.matcher(data);

		if (data.length() <= 1)
			return;

		if (dateMatcher.find()) {
			date = dateMatcher.group();
			date = date.replace("<FONT FACE=\"Arial\"><H3><CENTER>Vertretungsplan f&uuml;r", "");
			date = date.replace("</CENTER>", "");
		}

		String[] split = data.split("<TD COLSPAN=5 BGCOLOR=\"#028015\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">" + identifier
				+ "</FONT></B></CENTER></TD>");

		if (split.length <= 1)
			if (identifier != "") {
				res = "Es gibt aktuell keine Änderungen";
			} else {
				res = getString(R.string.placeholder_data);
			}
		else {
			split = split[1].split("<TD COLSPAN=5 BGCOLOR=\"#028015\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">");
			String[] dataset = split[0].split("<TR>");
			for (int i = 1; i < dataset.length - 1; i++) {
				String d = dataset[i].replaceAll("<TD><CENTER><FONT FACE=\"Arial\" SIZE=\"0\">", "");
				d = d.replaceAll("</FONT></CENTER></TD>", "");
				d = d.replaceAll("</TR>", "");
				d = d.replaceAll("\n", "");
				d = d.replaceAll("----- ", "");
				d = d.replaceAll("==&gt;", "→\n\t\t");
				res += d + "\n\n";
			}
		}

		substTextView.setText(res);
		substTextView.setMovementMethod(new ScrollingMovementMethod());
		dateTextView.setText(date);

	}

	public void applySettings() {
		// (Geänderte) Einstellungen anwenden
		String grade = getGradePrefs();
		String subgrade = getSubgradePrefs();

		identifier = grade.concat(subgrade);
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
			this.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/* Von stackoverflow */
	public static String getHttpText(String url) {
		String result = "";
		HttpClient httpclient = new DefaultHttpClient();

		// Prepare a request object
		HttpGet httpget = new HttpGet(url);
		// Execute the request
		HttpResponse response;
		try {
			// int i = (Integer)null;int j = i+1;
			response = httpclient.execute(httpget);

			// Get hold of the response entity
			HttpEntity entity = response.getEntity();
			// If the response does not enclose an entity, there is no need
			// to worry about connection release
			if (entity != null) {
				// A Simple JSON Response Read
				InputStream instream = entity.getContent();
				result = convertStreamToString(instream);
				// now you have the string representation of the HTML request
				instream.close();
			}

		} catch (Exception e) {
			return e.toString();
		}

		return result;
	}

	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the
		 * BufferedReader.readLine() method. We iterate until the BufferedReader
		 * return null which means there's no more data to read. Each line will
		 * appended to a StringBuilder and returned as String.
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

}
