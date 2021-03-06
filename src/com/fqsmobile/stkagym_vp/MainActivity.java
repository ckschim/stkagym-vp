package com.fqsmobile.stkagym_vp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.*;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    Map<Integer, String> currentStrings;
    List<Map<String, String>> valueList;
    Date date;
    SimpleAdapter adapter;
    SharedPreferences prefs;
    SharedPreferences.Editor prefsEditor;
    long lessonMins[];

    @SuppressLint("UseSparseArrays")
    @SuppressWarnings({"deprecation", "unchecked"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		/* @formatter:off */
        lessonMins = new long[]{
                8 * 60 + 25,
                9 * 60 + 10,
                10 * 60 + 15,
                11 * 60,
                12 * 60 + 10,
                13 * 60,
                13 * 60 + 50,
                14 * 60 + 45,
                15 * 60 + 30, //Ab dem nächsten geraten
                16 * 60 + 20,
                17 * 60 + 30,
                18 * 60 + 30};
        /* @formatter:on */

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

            date = (Date) data.get("date");
        }

        adapter = new SimpleAdapter(getApplicationContext(), valueList, R.layout.substitutionitem, new String[]{"section", "lesson",
                "col1", "col2"}, new int[]{R.id.section, R.id.lesson, R.id.col1, R.id.col2}) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                ViewGroup view = (ViewGroup) super.getView(position, convertView, parent);
                if (valueList.get(position).get("section") != null) {
                    view.getChildAt(0).setVisibility(View.VISIBLE);
                } else {
                    view.getChildAt(0).setVisibility(View.GONE);
                }

                TextView t1 = (TextView) ((ViewGroup) view.getChildAt(1)).getChildAt(0);
                TextView t2 = (TextView) ((ViewGroup) view.getChildAt(1)).getChildAt(1);
                TextView t3 = (TextView) ((ViewGroup) view.getChildAt(1)).getChildAt(3);
                ImageView i1 = (ImageView) ((ViewGroup) view.getChildAt(1)).getChildAt(2);

				if (isPast((int) (Float.parseFloat(valueList.get(position).get("lesson") + "0")))) {
                    t1.setTextColor(t1.getTextColors().withAlpha(77));
					t2.setTextColor(t1.getTextColors().withAlpha(77));
					t3.setTextColor(t1.getTextColors().withAlpha(77));
					i1.setAlpha(77);
				} else {
					t1.setTextColor(t1.getTextColors().withAlpha(255));
					t2.setTextColor(t1.getTextColors().withAlpha(255));
					t3.setTextColor(t1.getTextColors().withAlpha(255));
					i1.setAlpha(255);
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

        final ImageView warn = (ImageView) findViewById(R.id.cache_warning);
        warn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this).setMessage(
                        "Es besteht keine aktive Internetverbindung oder ein Problem mit der Website. Die angezeigten Daten sind möglicherweise nicht aktuell.").show();
            }
        });
    }

    /*
     * Daten speichern wenn App verlassen wird
     */
    @Override
    @SuppressWarnings("deprecation")
    public Object onRetainNonConfigurationInstance() {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("valueList", valueList);
        data.put("currentStrings", currentStrings);
        data.put("date", date);
        return data;
    }

    private class getData extends AsyncTask<Boolean, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar1);
            bar.setVisibility(View.VISIBLE);
        }

        String localDateString = "";
        String localMessage = "";
        List<Map<String, String>> localValueList = new ArrayList<Map<String, String>>();
        Date localDate;
        boolean isOfflineCached;

        /*
         * Gibt false zurück, wenn nicht neu gerendert werden muss.
         */
        @Override
        protected Boolean doInBackground(Boolean... isAlreadyRendered) {
            String data = "";
            if (isOnline()) {
                data = downloadData(true);
                if (data.equals("304") && isAlreadyRendered[0]) {
                    isOfflineCached = false;
                    return false;
                } else if (data.equals("304"))
                    data = prefs.getString("cached_page", "");

                // If all the caching caching goes wrong
                if (data.length() == 0)
                    data = downloadData(false);
                isOfflineCached = false;
            }

            if (!isOnline() || data.length() == 0) {
                String cacheData = prefs.getString("cached_page", "");
                if (prefs.getString("cached_page", "").length() > 0) {
                    data = cacheData;
                    isOfflineCached = true;
                } else {
                    localMessage = "Keine Internetverbindung";
                    isOfflineCached = false;
                    return true;
                }
            }

            try {
                JSONObject jsonObject = new JSONObject(data);
                String[] dataSet;

                JSONObject gradeObject, lessonObject;
                JSONArray substitutionArray, roomArray, examArray, dataArray;
                String lesson, from, to;
                boolean isTitle = false;

                if (!jsonObject.isNull("message"))
                {
                    localMessage = jsonObject.getString("message");
                    return true;
                }

                long eDate = jsonObject.getLong("date");
                localDate = new Date(eDate);
                SimpleDateFormat dt1 = new SimpleDateFormat("EEEE, d.M.y", Locale.GERMANY);
                dt1.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
                localDateString = dt1.format(localDate);

                substitutionArray = jsonObject.getJSONArray("substitution");
                roomArray = jsonObject.getJSONArray("rooms");
                examArray = jsonObject.getJSONArray("exams");

                for (int i = 0; i < substitutionArray.length(); i++) {
                    gradeObject = substitutionArray.getJSONObject(i);
                    if (gradeObject.getString("grade").equals(getGradePrefs() + getSubgradePrefs())) {
                        dataArray = gradeObject.getJSONArray("data");
                        for (int j = 0; j < dataArray.length(); j++) {
                            lessonObject = dataArray.getJSONObject(j);
                            lesson = lessonObject.getString("lesson");
                            from = lessonObject.getString("from");
                            to = lessonObject.getString("to");
                            dataSet = new String[]{lesson, from, to};
                            if (j == 0)
                                isTitle = true;
                            else
                                isTitle = false;
                            localValueList = addDataset(dataSet, "VERTRETUNGEN", isTitle, localValueList);
                        }
                    }
                }

                for (int i = 0; i < roomArray.length(); i++) {
                    gradeObject = roomArray.getJSONObject(i);
                    if (gradeObject.getString("grade").equals(getGradePrefs())) {
                        dataArray = gradeObject.getJSONArray("data");
                        for (int j = 0; j < dataArray.length(); j++) {
                            lessonObject = dataArray.getJSONObject(j);
                            lesson = lessonObject.getString("lesson");
                            from = lessonObject.getString("from");
                            to = lessonObject.getString("to");
                            dataSet = new String[]{lesson, from, to};
                            if (j == 0)
                                isTitle = true;
                            else
                                isTitle = false;
                            localValueList = addDataset(dataSet, "ERSATZRAUMPLAN", isTitle, localValueList);
                        }
                    }
                }

                for (int i = 0; i < examArray.length(); i++) {
                    gradeObject = examArray.getJSONObject(i);
                    if (gradeObject.getString("grade").equals(getGradePrefs())) {
                        dataArray = gradeObject.getJSONArray("data");
                        for (int j = 0; j < dataArray.length(); j++) {
                            lessonObject = dataArray.getJSONObject(j);
                            lesson = lessonObject.getString("lesson");
                            from = lessonObject.getString("from");
                            to = lessonObject.getString("to");
                            dataSet = new String[]{lesson, from, to};
                            if (j == 0)
                                isTitle = true;
                            else
                                isTitle = false;
                            localValueList = addDataset(dataSet, "KLAUSUREN", isTitle, localValueList);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                localMessage = "Fehler.";
                return true;
            }


            if (localValueList.size() == 0) {
                localMessage = "Es gibt aktuell keine Änderungen";
            }

            return true;
        }

        private List<Map<String, String>> addDataset(String[] pDataset, String setName, boolean isSection,
                                                     List<Map<String, String>> localValueList) {
            String[] set = new String[]{pDataset[0], pDataset[1], pDataset[2]};
            Map<String, String> m = new HashMap<String, String>();
            m.put("lesson", set[0]);
            m.put("col1", set[1]);
            if (set.length == 6)
                m.put("col2", set[2] + set[3]);
            else
                m.put("col2", set[2]);

            if (isSection) {
                m.put("section", setName);
            }

            localValueList.add(m);

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

            ImageView warn = (ImageView) findViewById(R.id.cache_warning);
            if (isOfflineCached)
                warn.setVisibility(View.VISIBLE);
            else
                warn.setVisibility(View.INVISIBLE);

            if (!result)
                return;

            updateTextView(R.id.date, localDateString);
            updateTextView(R.id.message, localMessage);

            date = localDate;

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

	private boolean isPast(int l) {
		Date now = new Date();

		/* DEBUG @formatter:off */
		/*SimpleDateFormat format = new SimpleDateFormat("HH:mm dd. MMM yyyy", Locale.GERMAN);
        format.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
		String date_str = "11:14 16. Okt 2013";
		try {
			now = format.parse(date_str);
		} catch (ParseException e) {
			e.printStackTrace();
		}*/

		/* DEBUG END @formatter:on */
		if (lessonMins.length < l)
			return false;
		long diff = now.getTime() - date.getTime() - lessonMins[l - 1] * 60 * 1000;
		return diff >= 0;
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


    /* Lädt Daten herunter. Wenn der Parameter wahr ist, wird im Falle eine
    * Cache-Hits "304" zurückgegeben.
    *
    * ---- Funktioniert nur wenn Etags vom Server unterstützt werden. ----*/
    public String downloadData(boolean allowCache) {
        String result = "";
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
                "VP-App/" + this.getString(R.string.settings_version_number) + " " + getGradePrefs()+getSubgradePrefs());
        HttpGet httpget = new HttpGet("http://stkagymvp.no-ip.biz:8080");

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

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
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
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

}