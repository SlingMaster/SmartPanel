/*
 * Copyright (c) 2020.
 * Jeneral Samopal Company
 * Programming by Alex Uchitel
 * Design and Programming by Alex Dovby
 */

package com.jsc.smartpanel;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import jsinterface.JSConstants;
import jsinterface.JSOut;
import utils.GlobalUtils;

public class FullscreenActivity extends AppCompatActivity {

    CommunicationServer communicationServer;
    public static SharedPreferences preference;
    private View mControlsView;
    WebView webView;

    private long back_pressed;
    public int port;
    private boolean Night = false;
    private int cur_screen = 1;
    private int lastCMD = 0;
    // private boolean sleep_mode = false;
    private String nextApp;
    private Boolean nextKill;
    private String currentApp;
    private boolean mVisible;

    // js interface ------------
    protected JSOut jsOut;
    public static JSONObject uiRequest;
    public static JSONObject sendData;
    // -------------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        preference = PreferenceManager.getDefaultSharedPreferences(this);

        // Device set port ---------------------
        String strPort = preference.getString("server_port", getResources().getString(R.string.def_port));
        port = Integer.valueOf(strPort);
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

        // server tcpip ------------------------------
        communicationServer = new CommunicationServer(this);
        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);

        setupClickListeners();

        // ------------------------------------
        // WebView
        // ------------------------------------
        webView = findViewById(R.id.web_view);
        webView.setOnClickListener(view -> toggle());
        webView.setWebViewClient(new NocWebViewClient());

        // ---------------------------------------------------
        externalCMD(Constants.CMD_LOAD_TIMER);
    }

    // set listeners for all buttons -------------------
    private void setupClickListeners() {
        // show splash activity ------------------------
        findViewById(R.id.btnShowCtrl).setOnClickListener(view -> {
            View splash = findViewById(R.id.splash);
            if (splash.getVisibility() == View.VISIBLE) {
                splash.setVisibility(View.GONE);
            } else {
                splash.setVisibility(View.VISIBLE);
            }
        });
        // show settings activity ------------------------
        findViewById(R.id.icon7).setOnClickListener(view -> {
            Context context = getApplicationContext();
            Intent configIntent = new Intent(context, SettingsActivity.class);
            configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(configIntent);
        });
        // menu list applications ------------------------
        findViewById(R.id.icon1).setOnClickListener(view -> externalCMD(Constants.CMD_RADIO));
        findViewById(R.id.icon2).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_SMART));
        findViewById(R.id.icon3).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_STATS));
        findViewById(R.id.icon4).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_TIMER));
        findViewById(R.id.icon5).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_WEATHER));
        findViewById(R.id.icon6).setOnClickListener(view -> externalCMD(Constants.CMD_SLING));
        findViewById(R.id.icon8).setOnClickListener(view -> externalCMD(Constants.CMD_WIFI_SCANNER));
    }

    // ==============================================
    // Loading a page with a self-signed certificate
    // ==============================================
    private class NocWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            System.out.println("[ trace  ] onPage Finished : " + url);

            // ----------------------------------------
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            View splash = findViewById(R.id.splash);
                            if (splash.getVisibility() == View.VISIBLE) {
                                splash.setVisibility(View.GONE);
                            }
                            killAllProcess();
                        }
                    }, 1000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        GlobalUtils.hideSystemUI(webView);
        nextKill = false;
        if (!GlobalUtils.isConnectingToInternet(getApplicationContext())) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.msg_not_wifi_connection), Toast.LENGTH_LONG).show();
        }
        // performed by a separate request ==================
        // new ReadXmlTask(FullscreenActivity.this).execute();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save state app -----------------
        // outState.putDouble(BILL_TOTAL, currentBillTotal);
    }

    @Override
    protected void onDestroy() {
        System.out.println("trace • onDestroy");
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

    @SuppressLint("addJavascriptInterface")
    // ----------------------------------------
    protected void loadHtml(String url) {
        String root = preference.getBoolean("sw_debug_mode", false)
                ? getResources().getString(R.string.root_debug) :
                getResources().getString(R.string.root);
        WebView webView = findViewById(R.id.web_view);
        boolean clear_cache = preference.getBoolean("sw_clear_cache", false);
        if (clear_cache) {
            webView.clearCache(true);
        }
        webView.loadUrl(root + url);

        // js interface --------------------------------------
        jsOut = new JSOut(webView);
        JSIn jsIn = new JSIn();
        webView.addJavascriptInterface(jsIn, JSConstants.INTERFACE_NAME);
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
    // Client request events
    // ===================================================
    public void webViewEvents(int request, final String jsonString) {
        int external_cmd = 0;
        JSONObject requestContent = new JSONObject();

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
                callbackToUI(JSConstants.EVT_MAIN_TEST, createResponse(requestContent, null));
                break;
            case JSConstants.EVT_READY:
                // AppUI = requestContent.optString("ui");
                callbackToUI(JSConstants.CMD_INIT, createResponse(requestContent, initData(this)));
//                if (AppUI.equalsIgnoreCase("project_weather")) {
//                 } else {
//                    callbackToUI(JSConstants.CMD_INIT, createResponse(requestContent, stateData()));
//                }

                break;
            case JSConstants.EVT_WEATHER:
                String list_url = getResources().getString(R.string.weather_xml);
                new ReadXmlTask(this, list_url).execute();
                // callbackToUI(JSConstants.CMD_WEATHER_DATA, createResponse(requestContent, sendData));
                break;
            case JSConstants.EVT_NEXT:
                if (Night) {
                    return;
                }
                cur_screen++;
                if (cur_screen > Constants.MAX_SCR) {
                    cur_screen = 1;
                }
                if (preference.getBoolean("sw_swap", false)) {
                    switch (cur_screen) {
                        case 1:
                            external_cmd = Constants.CMD_LOAD_TIMER;
                            break;
                        case 2:
                            external_cmd = Constants.CMD_LOAD_WEATHER;
                            break;
                        default:
                            external_cmd = 0;
                            break;
                    }
                }
                break;
            case JSConstants.EVT_BACK_LIGHT:
                if (preference.getBoolean("sw_back_light", false)) {
//                    sleep_mode = requestContent.optBoolean("sleep_mode", false);
//                    setBackLight(sleep_mode);
                    setBackLight(requestContent.optBoolean("sleep_mode", false));
                }
                break;
            case JSConstants.EVT_NIGHT_MODE:
                Night = requestContent.optBoolean("state", false);
                external_cmd = Night ? Constants.CMD_LOAD_TIMER : Constants.CMD_LOAD_WEATHER;
                System.out.println("trace   | Night: " + Night + " CMD : " + external_cmd);
                break;
            case JSConstants.EVT_BACK:
                break;
            case JSConstants.EVT_EXIT:
                break;
            case JSConstants.EVT_EXO_RESPONSE:
                break;
            default:
                external_cmd = 0;
                break;
        }
        System.out.println("trace   | external_cmd = " + external_cmd + " | jsonString:" + jsonString);
        if (external_cmd > 0) {
            new Handler().post(new UpdateUIRunnable(external_cmd));
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
            obj.put("node_bathroom_url", preference.getString("ip_bathroom", GlobalUtils.getString(context, R.string.def_node_weather_url)));
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

    // =========================================================
    // Interface HTML > Application
    // =========================================================
    private class JSIn {

        // @SuppressLint("callNative")
        @JavascriptInterface
        public final void callNative(int request, final String jsonString) {
            // HTML function send data to application -----------------------------
            webViewEvents(request, jsonString);
        }
    }

    // ----------------------------------------
    // command only ---------------------------
    protected void callbackToUI(int target) {
        if (jsOut != null) {
            jsOut.callJavaScript(target, createResponse(null, null));
        } else {
            System.out.println("trace | Error Missing JSInterface");
        }
    }

    // ----------------------------------------
    protected void callbackToUI(int target, JSONObject json) {
        if (jsOut != null) {
            jsOut.callJavaScript(target, json);
        } else {
            System.out.println("trace | Error Missing JSInterface");
        }
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
                sendData = XML.toJSONObject(strXML);
            } catch (JSONException e) {
                System.out.println("Unexpected JSON exception" + e);
            }
            activity.callbackToUI(JSConstants.CMD_WEATHER_DATA, activity.createResponse(uiRequest, sendData));
        }
    }

    // ===================================
    public void updateOnUIThread(String str) {
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
        // пока здесь куча отладочного кода -----------------------
        // почищу когда закончу отладку передачи комманд и значений

        String cmdStr;
        int cmd = 0;
        Boolean flag;
        if (data == null) {
            System.out.println("decryptCommand | data null");
            return;
        }

        cmdStr = "00";
        try {
            JSONObject clientRequest = new JSONObject(data);
            if (clientRequest.has("cmd")) {
                cmdStr = clientRequest.optString("cmd");
                cmd = Integer.parseInt(cmdStr, 16);
                if (clientRequest.has("json")) {
                    JSONObject json = clientRequest.optJSONObject("json");
                    // Toast.makeText(this, "Transmitted External Command • $" + cmdStr + " | jsonStr • " + json.toString(), Toast.LENGTH_SHORT).show();
                    externalCMD(cmd, json);
                } else {
                    externalCMD(cmd);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // TODO debug must remove
        Toast.makeText(getBaseContext(), "Transmitted External Command • $" + cmdStr + " | DEC • " + String.valueOf(cmd), Toast.LENGTH_SHORT).show();
    }

    // ===================================
    private void externalCMD(int cmd, JSONObject json) {
        // Toast.makeText(getBaseContext(), "Transmitted External Command  | DEC • " + String.valueOf(cmd) + " | " + json.toString(), Toast.LENGTH_SHORT).show();
        switch (cmd) {
            case Constants.CMD_BACK_LIGHT:
                if (json.has("state")) {
                    boolean state = json.optBoolean("state", true);
                    setBackLight(state);
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
        if (cmd == lastCMD) {
            return;
        }
        switch (cmd) {
            case Constants.CMD_RESTART:
                lastCMD = cmd;
                restartApp();
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
                callbackToUI(JSConstants.CMD_SWAP);
                break;

            // weather forecast ----------
            case Constants.CMD_LOAD_WEATHER:
                runApplication(3, cmd);
                break;
            case Constants.CMD_WEATHER_FORECAST:
                callbackToUI(JSConstants.CMD_SWAP);
                break;
            case Constants.CMD_WEATHER_MAGIC:
                callbackToUI(JSConstants.CMD_SEASON_MAGIC);
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
            restartApp(Constants.HTML_APPS[app_idx]);
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
    public void setBackLight(Boolean sleep_mode) {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        if (sleep_mode) {
            layout.screenBrightness = 0.2F;
        } else {
            layout.screenBrightness = 0.7F;
        }
        System.out.println("[ trace  ] setBackLight " + layout.screenBrightness);
        getWindow().setAttributes(layout);
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
                // killAllProcess();
                // backMain();
            }
        }
        back_pressed = System.currentTimeMillis();
    }

    private void backMain() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(Constants.PACKAGES[0]);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            // intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            // Bundle is optional --------------
//            Bundle bundle = new Bundle();
//            bundle.putInt("lastCMD", lastCMD);
//            intent.putExtras(bundle);
            //  end Bundle ---------------------
            startActivity(intent);
            View splash = findViewById(R.id.splash);
            if (splash.getVisibility() == View.GONE) {
                splash.setVisibility(View.VISIBLE);
            }
        }
    }

    // ===================================
    private void restartApp(String nextApp) {
        // reboot main application & load next app
        Context context = getApplicationContext();
        Intent mStartActivity = new Intent(context, FullscreenActivity.class);
        mStartActivity.putExtra("next_app", nextApp);
        mStartActivity.putExtra("next_kill", nextKill);
        int mPendingIntentId = PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }

    // ===================================
    private void restartApp() {
        // reboot main application -------
        Context context = getApplicationContext();
        Intent mStartActivity = new Intent(context, FullscreenActivity.class);
        int mPendingIntentId = PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }

    // ===================================
    private void killAllProcess() {
        List<ApplicationInfo> packages;
        PackageManager pm;
        pm = getPackageManager();
        //get a list of installed apps.
        packages = pm.getInstalledApplications(0);

        ActivityManager mActivityManager = (ActivityManager) FullscreenActivity.this.getSystemService(Context.ACTIVITY_SERVICE);
        String myPackage = getApplicationContext().getPackageName();

        for (ApplicationInfo packageInfo : packages) {
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                continue;
            }
            if (packageInfo.packageName.equals(myPackage)) {
                continue;
            }
            mActivityManager.killBackgroundProcesses(packageInfo.packageName);
        }
        nextKill = false;
        // TODO  debug must remove
        // Toast.makeText(getBaseContext(), "Killed All Background Process", Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Killed All Background Process", Toast.LENGTH_SHORT).show();
    }
}
