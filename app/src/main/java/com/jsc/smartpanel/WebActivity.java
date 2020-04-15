package com.jsc.smartpanel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class WebActivity extends AppCompatActivity {
    public static SharedPreferences preference;
    private String nextApp;
    ViewGroup webContainer;
    Integer app_id;
    // boolean night;
    @Nullable
    CustomWebView newWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preference = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent = getIntent();
        System.out.println("trace | WEB ACTIVITY | onCreate");
        if (intent != null) {
            app_id = intent.getIntExtra("app_id", 2);
            nextApp = Constants.HTML_APPS[app_id];
        }

        setContentView(R.layout.activity_web);
        webContainer = findViewById(R.id.new_web_container);
        findViewById(R.id.btnCtrl).setOnClickListener(view -> onBackPressed());

        // load screen -----------------------------
        loadHtml(nextApp);
        showMemory("onCreate");
        FullscreenActivity.app_state = Constants.INTERNAL;
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showMemory("onNewIntent");
        System.out.println("trace | WEB ACTIVITY | onNewIntent");
        if (intent != null) {
            String jsonStr = intent.getStringExtra("jsonStr");
            // Toast.makeText(getApplicationContext(), "ON NEW INTENT | jsonStr • " + jsonStr, Toast.LENGTH_LONG).show();
            decryptCommand(jsonStr);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Objects.requireNonNull(getSupportActionBar()).hide();
        GlobalUtils.hideSystemUI(webContainer);
        SysUtils.killAllProcess(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("trace | WEB ACTIVITY | • onDestroy");
        webContainer.removeAllViews();
        if (newWebView != null) {
            newWebView.destroy();
            newWebView = null;
        }
    }

    @Override
    public void onLowMemory() {
        System.out.println("trace • onLowMemory");
        super.onLowMemory();
    }

    @NonNull
    private CustomWebView createWebView() {
        CustomWebView view = new CustomWebView(this);
        view.setWebEventsListener(this::webViewEvents);
        // SIGNAL 11 SIGSEGV crash Android
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        return view;
    }

    // ----------------------------------------
    protected void loadHtml(String url) {
        String root = preference.getBoolean("sw_debug_mode", false)
                ? getResources().getString(R.string.root_debug) :
                getResources().getString(R.string.root);
        System.out.println("trace | loadHtml:" + root + url);
        newWebView = createWebView();
        webContainer.addView(newWebView);
        newWebView.loadUrl(root + url);
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
                assert newWebView != null;
                newWebView.callbackToUI(JSConstants.EVT_MAIN_TEST, CustomWebView.createResponse(requestContent, null));
                break;
            case JSConstants.EVT_READY:
                assert newWebView != null;
                newWebView.callbackToUI(JSConstants.CMD_INIT, CustomWebView.createResponse(requestContent, initData(this)));
                break;
            case JSConstants.EVT_WEATHER:
                new WebActivity.ReadXmlTask(this, getResources().getString(R.string.weather_xml)).execute();
                break;
            case JSConstants.EVT_NEXT:
                break;
            case JSConstants.EVT_SYNC:
                //  Toast.makeText(getBaseContext(), "webViewEvents • EVT_SYNC", Toast.LENGTH_SHORT).show();
                // sendNextAction(Constants.SYNC);
                break;
            case JSConstants.EVT_WOKE_UP:
                // LOAD_WEATHER --------
                sendNextAction(Constants.WOKE_UP);
                break;
            case JSConstants.EVT_SHOW_TIME:
                // LOAD_TIMER --------
                sendNextAction(Constants.SHOW_TIME);
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

    // -----------------------------------
    void onPageFinished() {
        Handler handler = new Handler();
//        Toast.makeText(getBaseContext(), "TimeOut • " + (getTimeout() / 60000) + " min", Toast.LENGTH_SHORT).show();
        SysUtils.LogToScr(this, preference, "TimeOut • " + (getTimeout() / 60000) + " min");
        handler.postDelayed(() -> sendNextAction(Constants.SWAP_SCREEN), getTimeout());
    }

    // ----------------------------------------
    @SuppressLint("SetTextI18n")
    protected void showMemory(String pref) {
        // ********************************
        // this code must remove after debug
        // ********************************
        // memory leak --------------------
        String msgMemory = "FREE RAM : " + SysUtils.getFreeMemory(this) + " Mb";
        TextView textInfo = findViewById(R.id.memInfo);
        textInfo.setText(preference.getBoolean("sw_log_screen", false) ? (pref + " • " + msgMemory) + " | test cycles • " + FullscreenActivity.test_cycles : "");
        // ********************************
    }

    // =========================================================
    // Read XML Weather
    // =========================================================
    private static class ReadXmlTask extends AsyncTask<Void, Void, String> {
        //        private WeakReference<FullscreenActivity> activityReference;
        private WeakReference<WebActivity> activityReference;
        private final String listUrl;

        HttpURLConnection urlConnection;
        BufferedReader reader;
        String resultXML = "";

        // ----------------------------
        // only retain a weak reference to the activity
        ReadXmlTask(WebActivity activity, @NonNull String list_url) {
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
            WebActivity activity = activityReference.get();
            if (activity == null) {
                return;
            }
            try {
                JSONObject sendData = XML.toJSONObject(strXML);
                // if (sendData != null) {
                assert activity.newWebView != null;
                activity.newWebView.callbackToUI(JSConstants.CMD_WEATHER_DATA, CustomWebView.createResponse(null, sendData));
                // }
            } catch (JSONException e) {
                System.out.println("Unexpected JSON exception" + e);
            }
        }
    }

    // ===================================
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    // ===================================
    private int getTimeout() {
        // delay 24 hours ------------
        int swap_frequency = 86400000;
        // int swap_frequency = 20000;
        if (preference.getBoolean("sw_swap", true)) {
//            if (!SysUtils.isNight(
//                    preference.getString("start_day", "6"),
//                    preference.getString("start_night", "20"))) {
            if (!SysUtils.isNight(preference)) {
                String tempNumStr = preference.getString("swap_frequency", "1");
                swap_frequency = Integer.parseInt(tempNumStr) * 60000;
            }
        }
        System.out.println("trace | Next return " + swap_frequency + " min");
        return swap_frequency;
    }

    // =========================================================
    private void sendNextAction(int cmd) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("cmd", cmd);
        resultIntent.putExtra("action", Constants.LOAD_SCREEN);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    // =========================================================
    private void sendNextAction(String action) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("action", action);
        setResult(RESULT_OK, resultIntent);
        System.out.println("trace | sendNextAction " + action);
        finish();
    }

    // =========================================================
    // External Command
    // =========================================================
    //==============================================
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
                        runCMD(cmd, json);
                    }
                } else {
                    runCMD(cmd);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //==============================================
    protected void runCMD(int cmd, JSONObject json) {
        switch (cmd) {
            case Constants.CMD_BACK_LIGHT:
                //  night = true;
                if (json.has("state")) {
                    SysUtils.setBackLight(this, json.optBoolean("state", true));
                }
                break;
            case Constants.CMD_DEBUG_MODE:
                break;
            default:
                Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_unsupported) + " | CMD • " + cmd, Toast.LENGTH_SHORT).show();

                break;
        }
    }

    // ==============================================
    protected void runCMD(int cmd) {
        assert newWebView != null;
        SysUtils.LogToScr(this, preference, "WebActivity | runCMD • " + cmd);
        switch (cmd) {
            case Constants.CMD_BACK:
                // onBackPressed();
                finish();
                break;
            case Constants.CMD_TIMER_SWAP:
                newWebView.callbackToUI(JSConstants.CMD_SWAP);
                break;
            case Constants.CMD_DEBUG_MODE:
                break;
            case Constants.CMD_WEATHER_FORECAST:
                newWebView.callbackToUI(JSConstants.CMD_SWAP);
                break;
            case Constants.CMD_WEATHER_MAGIC:
                newWebView.callbackToUI(JSConstants.CMD_SEASON_MAGIC);
                break;
            case Constants.CMD_LOAD_SMART:
            case Constants.CMD_LOAD_STATS:
            case Constants.CMD_LOAD_TIMER:
            case Constants.CMD_LOAD_WEATHER:
                sendNextAction(cmd);
                break;
            default:
                Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_unsupported), Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
