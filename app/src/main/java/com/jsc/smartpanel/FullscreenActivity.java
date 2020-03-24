/*
 * Copyright (c) 2020.
 * Jeneral Samopal Company
 * Programming by Alex Uchitel
 * Design and Programming by Alex Dovby
 */

package com.jsc.smartpanel;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.jsc.smartpanel.html.CustomWebView;
import com.jsc.smartpanel.html.JSConstants;
import com.jsc.utils.GlobalUtils;
import com.jsc.utils.SysUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class FullscreenActivity extends AppCompatActivity {

    CommunicationServer communicationServer;
    public static SharedPreferences preference;
    Timer timer;
    TimerTask swapTimerTask;

    private View mControlsView;
    CustomWebView webView;

    private long back_pressed;
    private int cur_screen = 0;
    private int lastCMD = 0;
    private String nextApp;
    private Boolean nextKill;
    private String currentApp;
    private boolean mVisible;
    private int test_cycles = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        preference = PreferenceManager.getDefaultSharedPreferences(this);

        // -------------------------------------

        Intent intent = getIntent();
        System.out.println("trace | onCreate");
        if (intent != null) {
            Uri data = getIntent().getData();
            if (data != null) {
                String host = data.getHost();
                String path = data.getPath();
                String param = data.getQueryParameter("key1");
                // String fragment = data.getFragment();
                System.out.println("trace | Host:" + host + " | path:" + path + " | param:" + param);
                //Log.e("TAG", "fragment:" + fragment);
            }
            // -------------------------------------
            // "next app" --------------------------
            nextApp = intent.getStringExtra("next_app");
            if (nextApp != null) {
                // int ls = nextApp.length();
                String ext = nextApp.substring(nextApp.length() - 5);
                Toast.makeText(getBaseContext(), "Next App Ext : " + ext, Toast.LENGTH_SHORT).show();
            } else {
                // default timer.html -----------
                nextApp = Constants.HTML_APPS[2];
            }
            // -------------------------------------
            // "next kill --------------------------
            nextKill = intent.getBooleanExtra("next_kill", false);
            // -------------------------------------
        }

        // SCREEN_BRIGHT_WAKE_LOCK =================
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_fullscreen);

        // Device set port ---------------------
        communicationServer = new CommunicationServer(this, Constants.SERVER_PORT);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);

        setupClickListeners();

        // ------------------------------------
        // WebView
        // ------------------------------------
        webView = findViewById(R.id.web_view);
        webView.setOnClickListener(view -> toggle());
        webView.setWebEventsListener(this::webViewEvents);
        // SIGNAL 11 SIGSEGV crash Android
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // ------------------------------------
        externalCMD(Constants.CMD_LOAD_TIMER);
    }

    // set listeners for all buttons -------------------
    private void setupClickListeners() {
        // show splash ------------------------
        findViewById(R.id.btnShowCtrl).setOnClickListener(view -> {
            View splash = findViewById(R.id.splash);
            if (splash.getVisibility() == View.VISIBLE) {
                splash.setVisibility(View.GONE);
            } else {
                splash.setVisibility(View.VISIBLE);
            }
        });
        // show settings activity ------------------------
        findViewById(R.id.btn_settings).setOnClickListener(view -> {
            Context context = getApplicationContext();
            Intent configIntent = new Intent(context, SettingsActivity.class);
            configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(configIntent);
        });
        // menu list applications ------------------------
        findViewById(R.id.btn_radio).setOnClickListener(view -> externalCMD(Constants.CMD_RADIO));
        findViewById(R.id.btn_smart).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_SMART));
        findViewById(R.id.btn_stats).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_STATS));
        findViewById(R.id.btn_timer).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_TIMER));
        findViewById(R.id.btn_weather).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_WEATHER));
        findViewById(R.id.btn_sling).setOnClickListener(view -> externalCMD(Constants.CMD_SLING));
        findViewById(R.id.btn_wifi).setOnClickListener(view -> externalCMD(Constants.CMD_WIFI_SCANNER));
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportActionBar().hide();
        GlobalUtils.hideSystemUI(webView);
        nextKill = false;
        if (!GlobalUtils.isConnectingToInternet(getApplicationContext())) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.msg_not_wifi_connection), Toast.LENGTH_LONG).show();
        }
        // start swap timer ------
        startTimer();
        // performed by a separate request ==================
        // new ReadXmlTask(FullscreenActivity.this).execute();
    }

    @Override
    protected void onStop() {
        stopTimer();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        System.out.println("trace • onDestroy");
        webView.setWebEventsListener(null);
        stopTimer();
        communicationServer.stop();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        System.out.println("trace • onLowMemory");
        super.onLowMemory();
    }

    private void toggle() {
        if (mVisible) {
            mControlsView.setVisibility(View.GONE);
            mVisible = false;
        } else {
            mControlsView.setVisibility(View.VISIBLE);
            mVisible = true;
        }
    }

    // ----------------------------------------
    protected void loadHtml(String url) {
        String root = preference.getBoolean("sw_debug_mode", false)
                ? getResources().getString(R.string.root_debug) :
                getResources().getString(R.string.root);
        boolean clear_cache = preference.getBoolean("sw_clear_cache", false);
        if (clear_cache) {
            webView.clearCache(true);
        }

        // ********************************
        // this solved the problem
        // SIGNAL 11 SIGSEGV crash Android
        webView.freeMemory();
        // ********************************

        webView.loadUrl(root + url);

        // ********************************
        // this code must remove after debug
        // ********************************
        // memory leak --------------------
        test_cycles++;
        String msgMemory = "FREE RAM : " + SysUtils.getFreeMemory(this) + " Mb | CYCLES : " + test_cycles;
        TextView textInfo = findViewById(R.id.memInfo);
        textInfo.setText(msgMemory);
        // ********************************
    }

    // ===================================================
    // Create response for HTML UI
    // ===================================================
    protected JSONObject createResponse(JSONObject request, JSONObject response) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(JSConstants.REQUEST, request);
            obj.put(JSConstants.RESPONSE, response);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return obj;
    }

    // ===================================================
    // HTML APP request events
    // ===================================================
    public void webViewEvents(int request, final String jsonString) {
        JSONObject requestContent = new JSONObject();

        JSONObject uiRequest;
        try {
            uiRequest = new JSONObject(jsonString);
            if (uiRequest.has("request")) {
                requestContent = uiRequest.getJSONObject("request");
            } else {
                requestContent = uiRequest;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        switch (request) {
            case JSConstants.EVT_MAIN_TEST:
                webView.callbackToUI(JSConstants.EVT_MAIN_TEST, createResponse(requestContent, null));
                break;
            case JSConstants.EVT_READY:
                webView.callbackToUI(JSConstants.CMD_INIT, createResponse(requestContent, initData(this)));
                break;
            case JSConstants.EVT_WEATHER:
                new ReadXmlTask(this, getResources().getString(R.string.weather_xml)).execute();
                break;
            case JSConstants.EVT_NEXT:
                break;
            case JSConstants.EVT_SYNC:
                syncData();
                break;
            case JSConstants.EVT_WOKE_UP:
                updateOnUIThread("{cmd:" + Constants.CMD_LOAD_WEATHER + "}");
                break;
            case JSConstants.EVT_SHOW_TIME:
                updateOnUIThread("{cmd:" + Constants.CMD_LOAD_TIMER + "}");
                break;
            case JSConstants.EVT_BACK:
                break;
            case JSConstants.EVT_PAGE_FINISHED:
                onPageFinished();
                break;
            default:
                System.out.println("Unsupported command : " + request);
                break;
        }
    }

    // ----------------------------------------
    public static JSONObject initData(@NonNull Context context) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("android_os", android.os.Build.VERSION.SDK_INT);
            obj.put("language", "en");
            obj.put("phone_ui", !GlobalUtils.isTablet(context));
            obj.put("android_app", true);
            obj.put("node_url", preference.getString("ip_weather", GlobalUtils.getString(context, R.string.def_node_weather_url)));
            obj.put("node_bathroom_url", preference.getString("ip_bathroom", GlobalUtils.getString(context, R.string.def_node_bathroom_url)));
            obj.put("chip_weather", preference.getString("chip_weather", GlobalUtils.getString(context, R.string.def_node_weather_chip)));
            obj.put("chip_bathroom", preference.getString("chip_bathroom", GlobalUtils.getString(context, R.string.def_node_bathroom_chip)));
            obj.put("auto_start_night_mode", preference.getBoolean("sw_auto_start", false));

            // string to int ---------------------
            String tempNumStr = preference.getString("start_day", "6");
            int num = Integer.parseInt(tempNumStr);
            obj.put("start_day", num);
            tempNumStr = preference.getString("start_night", "20");
            num = Integer.parseInt(tempNumStr);
            obj.put("start_night", num);
            tempNumStr = preference.getString("swap_frequency", "1");
            num = Integer.parseInt(tempNumStr);
            obj.put("swap_frequency", num);
            // -----------------------------------
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return obj;
    }

    void onPageFinished() {
        // ----------------------------------------
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        View splash = findViewById(R.id.splash);
                        if (splash.getVisibility() == View.VISIBLE) {
                            splash.setVisibility(View.GONE);
                        }
                        runKillAllProcess();
                    }
                }, 1000);
    }


    // =========================================================
    // Read XML Weather
    // =========================================================
    private static class ReadXmlTask extends AsyncTask<Void, Void, String> {
        private WeakReference<FullscreenActivity> activityReference;
        private final String listUrl;

        HttpURLConnection urlConnection;
        BufferedReader reader;
        String resultXML = "";

        // ----------------------------
        // only retain a weak reference to the activity
        ReadXmlTask(FullscreenActivity activity, @NonNull String list_url) {
            activityReference = new WeakReference<>(activity);
            listUrl = list_url;
        }

        // ----------------------------
        @Override
        protected String doInBackground(Void... params) {
            // System.out.println("trace | list_url : " + list_url);
            try {

                URL url = new URL(listUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                // ------------------

                StringBuilder buffer = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                resultXML = buffer.toString();

            } catch (Exception e) {
                e.printStackTrace();
                // System.out.println("Error load list_url : " + resultXML);
                resultXML = "{weather:[]}";
            }

            return resultXML;
        }

        // ----------------------------
        @Override
        protected void onPostExecute(String strXML) {
            super.onPostExecute(strXML);
            // System.out.println("trace | onPostExecute: " + strXML);
            // get a reference to the activity if it is still there
            FullscreenActivity activity = activityReference.get();
            try {
                JSONObject sendData = XML.toJSONObject(strXML);
                if (sendData != null) {
                    activity.webView.callbackToUI(JSConstants.CMD_WEATHER_DATA, activity.createResponse(null, sendData));
                }
            } catch (JSONException e) {
                System.out.println("Unexpected JSON exception" + e);
            }
        }
    }

    // ===================================
    public void updateOnUIThread(String str) {
        Log.d("net message", str);
        runOnUiThread(new UpdateUIRunnable(str));
    }

    // ===================================
    class UpdateUIRunnable implements Runnable {
        private String jsonStr;
        private int command;

        // ----------------------------
        private UpdateUIRunnable(String str) {
            this.jsonStr = str;
        }

        // ----------------------------
        private UpdateUIRunnable(int cmd) {
            this.command = cmd;
        }

        // ----------------------------
        @Override
        public void run() {
            if (jsonStr != null) {
                // Transmitted External Command  from Desktop Client
                decryptCommand(jsonStr);
            }
            if (command > 0) {
                // Transmitted External Command  from Html Application
                System.out.println("trace | html Application command : " + command);
                externalCMD(command);
            }
        }
    }

    // ===================================
    private void decryptCommand(String data) {
        if (data == null) {
            System.out.println("decryptCommand | data null");
            return;
        }

        try {
            JSONObject clientRequest = new JSONObject(data);
            if (clientRequest.has("cmd")) {
                int cmd = clientRequest.optInt("cmd", 0);
                if (clientRequest.has("json")) {
                    JSONObject json = clientRequest.optJSONObject("json");
                    if (json != null) {
                        externalCMD(cmd, json);
                    }
                } else {
                    externalCMD(cmd);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ===================================
    private void externalCMD(int cmd, @NonNull JSONObject json) {
        Toast.makeText(getBaseContext(), "Transmitted External Command  | DEC • " + cmd + " | " + json.toString(), Toast.LENGTH_SHORT).show();
        switch (cmd) {
            case Constants.CMD_BACK_LIGHT:
                if (json.has("state")) {
                    SysUtils.setBackLight(this, json.optBoolean("state", true));
                }
                break;
            case Constants.CMD_DEBUG_MODE:
                break;
            default:
                Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_unsupported), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    // ===================================
    private void externalCMD(int cmd) {
        // TODO debug must remove
        Toast.makeText(getBaseContext(), "Transmitted External Command • $" + String.format("%X", cmd) + " | DEC • " + cmd, Toast.LENGTH_SHORT).show();

        if (cmd == lastCMD) {
            return;
        }
        switch (cmd) {
            case Constants.CMD_RESTART:
                lastCMD = cmd;
                SysUtils.restartApp(this);
                break;
            case Constants.CMD_BACK:
                onBackPressed();
                break;
            case Constants.CMD_DEBUG_MODE:
                break;

            // ===========================
            // Menu ======================
            // ===========================
            case Constants.CMD_RADIO:
                runExternalApplication(1, cmd);
                break;
            case Constants.CMD_LOAD_SMART:
                runApplication(0, cmd);
                break;
            case Constants.CMD_LOAD_STATS:
                runApplication(1, cmd);
                break;
            // timer ---------------------
            case Constants.CMD_LOAD_TIMER:
                runApplication(2, cmd);
                break;
            case Constants.CMD_TIMER_SWAP:
                webView.callbackToUI(JSConstants.CMD_SWAP);
                break;

            // weather forecast ----------
            case Constants.CMD_LOAD_WEATHER:
                runApplication(3, cmd);
                break;
            case Constants.CMD_WEATHER_FORECAST:
                webView.callbackToUI(JSConstants.CMD_SWAP);
                break;
            case Constants.CMD_WEATHER_MAGIC:
                webView.callbackToUI(JSConstants.CMD_SEASON_MAGIC);
                break;
            // --------------------------

            case Constants.CMD_SLING:
                runExternalApplication(2, cmd);
                break;
            // --------------------------

            case Constants.CMD_WIFI_SCANNER:
                runExternalApplication(3, cmd);
                break;
            // ===========================
            default:
                Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_unsupported), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    // ===================================
    private void runApplication(int app_idx, int cmd) {
        lastCMD = cmd;
        currentApp = Constants.HTML_APPS[app_idx];
        if (nextKill) {
            // restartApp(Constants.HTML_APPS[app_idx]);
            SysUtils.restartApp(this, Constants.HTML_APPS[app_idx], true);
            nextKill = false;
        } else {
            loadHtml(Constants.HTML_APPS[app_idx]);
        }
    }

    // ===================================
    private void runExternalApplication(int app_idx, int cmd) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(Constants.PACKAGES[app_idx]);
        if (intent != null) {
            nextKill = true;
            lastCMD = cmd;
            currentApp = Constants.PACKAGES[app_idx];
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_app_not_installed), Toast.LENGTH_SHORT).show();
        }
    }

    // ===================================
    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            System.out.println(" trace | App " + currentApp.substring(nextApp.length() - 5));
            // main application ------------------
            if (currentApp.substring(nextApp.length() - 5).equals(".html")) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_exit),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getBaseContext(), "I think about it",
                        Toast.LENGTH_SHORT).show();
                // backPrevApp(currentApp);
                // backMain();
            }
        }
        back_pressed = System.currentTimeMillis();
    }

    // ===================================
    public void swapScreen() {
        cur_screen++;
        int length = preference.getBoolean("sw_all_swap", false) ? Constants.SWAP_APPS.length : 2;
        if (cur_screen >= length) {
            cur_screen = 0;
        }
        System.out.println("traceSW | Swap Screen | Cur_screen = " + String.valueOf(cur_screen));
        new Handler().post(new UpdateUIRunnable(Constants.SWAP_APPS[cur_screen]));
    }

    // ===================================
    private void runKillAllProcess() {
        SysUtils.killAllProcess(this);
        nextKill = false;
        // all processes are already killed ---------------
    }

    // ===================================
    private void syncData() {
        boolean night = SysUtils.isNight(
                preference.getString("start_day", "6"),
                preference.getString("start_night", "20"));

        if (night) {
            // ***********************************************
            // set backLight should work in a separate thread
            // working if Auto Set Backlight Screen is enabled
            // ***********************************************
            if (preference.getBoolean("sw_back_light", false)) {
                updateOnUIThread("{cmd:4,json:{state:true}}");
            }
        } else {
            startTimer();
            if (preference.getBoolean("sw_back_light", false)) {
                updateOnUIThread("{cmd:4,json:{state:false}}");
            }
        }
    }

    // ===================================
    // Swap Screen Timer
    // ===================================

    // ===================================
    private void startTimer() {
        // ***********************************************
        // work if Auto Swap Screens is enabled
        // only during the daytime 
        // ***********************************************
        if (preference.getBoolean("sw_swap", true)) {
            if (SysUtils.isNight(
                    preference.getString("start_day", "6"),
                    preference.getString("start_night", "20"))) {
                System.out.println("traceSW | Next stopTimer");
                stopTimer();
                new Handler().post(new UpdateUIRunnable(Constants.CMD_LOAD_TIMER));
            } else {
                if (timer == null) {
                    System.out.println("traceSW | startTimer");
                    String tempNumStr = preference.getString("swap_frequency", "1");
                    int swap_frequency = Integer.parseInt(tempNumStr) * 60000;
                    timer = new Timer();
                    swapTimerTask = new SwapTimerTask();
                    timer.schedule(swapTimerTask, 60000, swap_frequency);
                }
            }
        }
    }

    // ===================================
    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            swapTimerTask = null;
        }
    }

    // ===================================
    class SwapTimerTask extends TimerTask {
        @Override
        public void run() {

            // --------------------------
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    swapScreen();
                }
            });
        }
    }
}
