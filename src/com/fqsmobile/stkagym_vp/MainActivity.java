package com.fqsmobile.stkagym_vp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
	Map<Integer, String> currentStrings;
	List<Map<String, String>> valueList;
	SimpleAdapter adapter;
	SharedPreferences prefs;
	SharedPreferences.Editor prefsEditor;

	@SuppressLint("UseSparseArrays")
	@SuppressWarnings({ "deprecation", "unchecked" })
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		prefsEditor = prefs.edit();

		HashMap<String, Object> data = (HashMap<String, Object>) getLastNonConfigurationInstance();
		if (data == null) {
			valueList = new ArrayList<Map<String, String>>();
			currentStrings = new HashMap<Integer, String>();
			applySettings();
		} else {
			valueList = (ArrayList<Map<String, String>>) data.get("valueList");
			currentStrings = (HashMap<Integer, String>) data.get("currentStrings");
			Set<Integer> stringKeys = currentStrings.keySet();

			for (Integer item : stringKeys) {
				updateTextView(item);
			}
		}

		adapter = new SimpleAdapter(getApplicationContext(), valueList, R.layout.substitutionitem, new String[] { "section", "lesson",
				"col1", "col2" }, new int[] { R.id.section, R.id.lesson, R.id.col1, R.id.col2 }) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				if (valueList.get(position).get("section") != null) {
					((ViewGroup) view).getChildAt(0).setVisibility(View.VISIBLE);
				} else {
					((ViewGroup) view).getChildAt(0).setVisibility(View.GONE);
				}
				return view;
			}
		};

		ListView lv = (ListView) findViewById(R.id.substData);
		lv.setAdapter(adapter);

		final ImageButton button = (ImageButton) findViewById(R.id.button_refresh); // Refresh-Button
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				new getData().execute(true);
			}
		});
	}

	/*
	 * Daten speichern wenn App verlassen wird
	 */
	public Object onRetainNonConfigurationInstance() {
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("valueList", valueList);
		data.put("currentStrings", currentStrings);
		return data;
	}

	private class getData extends AsyncTask<Boolean, Void, Boolean> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar1);
			bar.setVisibility(View.VISIBLE);
		}

		String localDate = "";
		String localMessage = "";
		List<Map<String, String>> localValueList = new ArrayList<Map<String, String>>();;

		/*
		 * Gibt false zurück, wenn nicht neu gerendert werden muss.
		 */
		@Override
		protected Boolean doInBackground(Boolean... isAlreadyRendered) {

			if (!isOnline()) {
				localMessage = "Keine Internetverbindung";
				return true;
			}

			String data = downloadData(true);

			if (data.equals("304") && isAlreadyRendered[0])
				return false;
			else if (data.equals("304"))
				data = prefs.getString("cached_page", "");

			// If all the caching caching goes wrong
			if (data.length() == 0)
				data = downloadData(false);

			String pattern = "\\<FONT FACE\\=\"Arial\"\\>\\<H3\\>\\<CENTER\\>Vertretungsplan f&uuml;r (.*)\\<\\/CENTER\\>";
			Pattern datePattern = Pattern.compile(pattern);
			Matcher dateMatcher = datePattern.matcher(data);

			if (dateMatcher.find()) {
				localDate = dateMatcher.group(1);
			} else {
				localMessage = "Fehler: Unvollständige Daten";
				return true;
			}

			String[] toplevelsplitting = data.split("<FONT FACE=\"Arial\"><H3><CENTER>Ersatzraumplan f&uuml;r (.*)</CENTER></H3></FONT>");

			// Vertretungen
			String[] v = toplevelsplitting[0].split("<TD COLSPAN=5 BGCOLOR=\"#[0-9A-Z]{6}\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">"
					+ currentStrings.get(R.id.grade) + "</FONT></B></CENTER></TD>");

			if (v.length > 1) {
				String[] v_dataset = v[1].split("<TD COLSPAN=5 BGCOLOR=\"#[0-9A-Z]{6}\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">");
				v_dataset = v_dataset[0].split("</TR>");
				localValueList = addDataset(v_dataset, "VERTRETUNGEN", true, localValueList);
			}

			// Klausuren

			String[] k = toplevelsplitting[0].split("<TD COLSPAN=5 BGCOLOR=\"#[0-9A-Z]{6}\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">K"
					+ currentStrings.get(R.id.grade) + "[a-z]</FONT></B></CENTER></TD>");

			if (k.length > 1) {
				for (int i = 1; i < k.length; i++) {
					String[] buf = k[i].split("<TD COLSPAN=5 BGCOLOR=\"#[0-9A-Z]{6}\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">");
					String[] set = buf[0].split("</TR>");
					localValueList = addDataset(set, "KLAUSUREN", i == 1 ? true : false, localValueList);
				}
			}

			// Ersatzraumplan

			String[] e = toplevelsplitting[1].split("<TD COLSPAN=5 BGCOLOR=\"#[0-9A-Z]{6}\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">"
					+ currentStrings.get(R.id.grade) + "</FONT></B></CENTER></TD>");
			if (e.length > 1) {
				String[] e_dataset = e[1].split("<TD COLSPAN=5 BGCOLOR=\"#[0-9A-Z]{6}\"><CENTER><B><FONT FACE=\"Arial\" SIZE=\"0\">");
				e_dataset = e_dataset[0].split("</TR>");
				localValueList = addDataset(e_dataset, "ERSATZRAUMPLAN", true, localValueList);
			}

			if (localValueList.size() == 0) {
				localMessage = "Es gibt aktuell keine Änderungen";
			}

			return true;
		}

		private List<Map<String, String>> addDataset(String[] pDataset, String setName, boolean isSection,
				List<Map<String, String>> localValueList) {
			for (int i = 1; i < pDataset.length - 1; i++) {
				// http://cdn.memegenerator.net/instances/400x/30464908.jpg
				pDataset[i] = pDataset[i].replaceAll("</FONT></CENTER></TD>", "");
				pDataset[i] = pDataset[i].replaceAll("-----", "");
				pDataset[i] = pDataset[i].replaceAll("AUFS ", "");
				pDataset[i] = pDataset[i].replaceAll("aufs ", "");
				pDataset[i] = pDataset[i].replaceAll("\n", "");
				pDataset[i] = pDataset[i].replaceAll("&auml;", "ä");
				pDataset[i] = pDataset[i].replaceAll("&ouml;", "ö");
				pDataset[i] = pDataset[i].replaceAll("&uuml;", "ü");
				pDataset[i] = pDataset[i].replaceAll("&szlig;", "ß");
				String[] set = pDataset[i]
						.split("<TD><CENTER><FONT FACE=\"Arial\" SIZE=\"0\">|<TD><CENTER><FONT COLOR=\"#FF0000\" FACE=\"Arial\" SIZE=\"0\">");
				if (set[1].startsWith("0"))
					set[1] = set[1].substring(1, 3);
				else
					set[1] = set[1].substring(0, 3);

				Map<String, String> m = new HashMap<String, String>();
				m.put("lesson", set[1]);
				m.put("col1", set[2]);
				if (set.length == 6)
					m.put("col2", set[4] + set[5]);
				else
					m.put("col2", set[4]);

				if (i == 1 && isSection) {
					m.put("section", setName);
				}

				localValueList.add(m);
			}
			return localValueList;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}

		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar1);
			bar.setVisibility(View.INVISIBLE);

			if (!result)
				return;

			updateTextView(R.id.substDate, localDate);
			updateTextView(R.id.message, localMessage);

			valueList.clear();
			valueList.addAll(localValueList);
			adapter.notifyDataSetChanged();
		}

	}

	/*
	 * (Geänderte) Einstellungen anwenden
	 */
	public void applySettings() {
		String grade = getGradePrefs();
		String subgrade = getSubgradePrefs();
		ImageButton btn = (ImageButton) findViewById(R.id.button_refresh);

		if (grade.equals("")) {
			updateTextView(R.id.message, "Bitte erst Einstellungen vornehmen.\nMenü → Einstellungen\n");
			btn.setEnabled(false);
			return;
		}

		if (!(grade.equals("EF") || grade.equals("Q1") || grade.equals("Q2")) && subgrade.equals("")) {

			updateTextView(R.id.message,
					"Du hast eine Stufe ausgewählt, aber keine Klasse. Bitte gehe zurück in die Einstellungen und stelle die Klasse ein.");
			btn.setEnabled(false);
			return;
		}

		btn.setEnabled(true);
		String identifier = grade.concat(subgrade);
		updateTextView(R.id.grade, identifier);

		new getData().execute(false);
	}

	/*
	 * TextView updaten und String speichern
	 */
	private void updateTextView(int id, String text) {
		TextView view = (TextView) findViewById(id);
		view.setText(text);
		currentStrings.put(id, text);
	}

	/*
	 * TextView aus dem Speicher updaten
	 */
	private void updateTextView(int id) {
		TextView view = (TextView) findViewById(id);
		view.setText(currentStrings.get(id));
	}

	/*
	 * Wrapper für sharedPreferences. Gibt jeweils einen gespeicherten String
	 * zurück (eigentlich überflüssig)
	 */

	public String getGradePrefs() {
		return prefs.getString("grades_list", "");

	}

	public String getSubgradePrefs() {
		return prefs.getString("subgrades_list", "");
	}

	public String getEtag() {
		return prefs.getString("etag", "");
	}

	public String getCachedPage() {
		return prefs.getString("cached_page", "");
	}

	/*
	 * Erstellung des Menu-Button Menüs
	 */
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

	/*
	 * Lädt Daten herunter. Wenn der Parameter wahr ist, wird im Falle eine
	 * Cache-Hits "304" zurückgegeben.
	 */
	public String downloadData(boolean allowCache) {
		String result = "";
		HttpClient httpclient = new DefaultHttpClient();

		HttpGet httpget = new HttpGet("http://www.gymnasium-kamen.de/pages/vp.html");

		if (allowCache)
			httpget.addHeader("If-None-Match", getEtag());

		HttpResponse response;
		try {
			response = httpclient.execute(httpget);

			// Check if cached data is up-to-date
			if (response.getStatusLine().getStatusCode() == 304) {
				return "304";
			}

			HttpEntity entity = response.getEntity();

			if (entity != null) {
				InputStream instream = entity.getContent();
				result = convertStreamToString(instream);
				instream.close();

				prefsEditor.putString("etag", response.getHeaders("Etag")[0].getValue());
				prefsEditor.putString("cached_page", result);
				prefsEditor.commit();
			}

		} catch (Exception e) {
			return e.toString();
		}

		return result;
	}

	/*
	 * Konvertiert einen Stream zu einem String
	 */
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

	/*
	 * Überprüft ob das Gerät Internetzugriff hat
	 */
	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

}
