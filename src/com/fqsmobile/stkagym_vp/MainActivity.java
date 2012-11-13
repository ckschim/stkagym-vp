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

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
	String identifier;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		/* Anwenden der Einstellungen beim Start */
		applySettings();
		final Button button = (Button) findViewById(R.id.button_refresh); // Refresh-Button
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				new getData().execute();
			}
		});
	}

	private class getData extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar1);
			bar.setVisibility(View.VISIBLE);

		}

		String res = "";
		String date = "";

		@Override
		protected Void doInBackground(Void... arg0) {
			
			if(!isOnline()) {
				res = "Keine Internetverbindung";
				return null;
			}

			String data = getHttpText("http://www.gymnasium-kamen.de/pages/vp.html");
			if (data.length() <= 1)
				return null;

			String pattern = "\\<FONT FACE\\=\"Arial\"\\>\\<H3\\>\\<CENTER\\>Vertretungsplan f&uuml;r (.*)\\<\\/CENTER\\>";
			Pattern datePattern = Pattern.compile(pattern);
			Matcher dateMatcher = datePattern.matcher(data);

			if (dateMatcher.find()) {
				date = dateMatcher.group(1);
			} else {
				res = "Fehler: Unvollständige Daten";
				return null;
			}
			
			String[] toplevelsplitting = data.split("<FONT FACE=\"Arial\"><H3><CENTER>Ersatzraumplan f&uuml;r (.*)</CENTER></H3></FONT>");

			// Vertretungen
			String[] v = toplevelsplitting[0].split("<TD COLSPAN=5 BGCOLOR=\"#[0-9A-Z]{6}\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">" + identifier
					+ "</FONT></B></CENTER></TD>");

			if (v.length > 1) {
				res += "<b>Vertretungen:</b><br /><br />";
				String[] v_dataset = v[1].split("<TD COLSPAN=5 BGCOLOR=\"#[0-9A-Z]{6}\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">");
				v_dataset = v_dataset[0].split("</TR>");
				res += formatDataset(v_dataset);
			}

			// Klausuren

			String[] k = toplevelsplitting[0].split("<TD COLSPAN=5 BGCOLOR=\"#[0-9A-Z]{6}\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">K" + identifier
					+ "[a-z]</FONT></B></CENTER></TD>");

			if (k.length > 1) {
				res += "<b>Klausuren:</b><br /><br />";
				for(int i = 1; i < k.length; i++) {
					String[] buf = k[i].split("<TD COLSPAN=5 BGCOLOR=\"#[0-9A-Z]{6}\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">");
					String[] set = buf[0].split("</TR>");
					res += formatDataset(set);
				}
			}
			
			// Ersatzraumplan
			
			String[] e = toplevelsplitting[1].split("<TD COLSPAN=5 BGCOLOR=\"#[0-9A-Z]{6}\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">" + identifier
					+ "</FONT></B></CENTER></TD>");
			if(e.length > 1) {
				res += "<b>Ersatzraumplan:</b><br /><br />";
				String[] e_dataset = e[1].split("<TD COLSPAN=5 BGCOLOR=\"#[0-9A-Z]{6}\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">");
				e_dataset = e_dataset[0].split("</TR>");
				res += formatDataset(e_dataset);
			}

			if (res == "") {
				if (identifier != "") {
					res = "Es gibt aktuell keine Änderungen";
				} else {
					res = "Bitte erst Einstellungen vornehmen.\nMenü → Einstellungen\n";
				}
			} else {
				res = res.replaceAll("&auml;", "ä");
				res = res.replaceAll("&ouml;", "ö");
				res = res.replaceAll("&uuml;", "ü");
				res = res.replaceAll("&szlig;", "ß");
			}

			return null;
		}

		private String formatDataset(String[] pDataset) {
			String res = "";
			for (int i = 1; i < pDataset.length - 1; i++) {
				String d = pDataset[i].replaceAll("<TD><CENTER><FONT FACE=\"Arial\" SIZE=\"0\">", "");
				d = d.replaceAll("</FONT></CENTER></TD>", "");
				d = d.replaceAll("<TR>", "");
				d = d.replaceAll("\n", "");
				d = d.replaceAll("----- ", "");
				d = d.replaceAll("AUFS ", "");
				d = d.replaceAll("aufs ", "");
				d = d.replaceAll("==&gt;", "→<br />\t\t");
				res += d + "<br /><br />";
			}
			return res;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}

		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			TextView substTextView = (TextView) findViewById(R.id.substitution_data);
			TextView dateTextView = (TextView) findViewById(R.id.substDate);
			substTextView.setText(Html.fromHtml(res));
			substTextView.setMovementMethod(new ScrollingMovementMethod());
			dateTextView.setText(date);
			ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar1);
			bar.setVisibility(View.INVISIBLE);
		}

	}

	public void applySettings() {
		// (Geänderte) Einstellungen anwenden
		String grade = getGradePrefs();
		String subgrade = getSubgradePrefs();

		identifier = grade.concat(subgrade);
		TextView gradeTextView = (TextView) findViewById(R.id.grade);
		gradeTextView.setText(identifier);
		new getData().execute();
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
	
	public boolean isOnline() {
	    ConnectivityManager cm =
	        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}

}
