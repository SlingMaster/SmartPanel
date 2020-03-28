package com.jsc.smartpanel;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class webActivity extends AppCompatActivity {
    public static SharedPreferences preference;
    private String nextApp;
    ViewGroup webContainer;
    Integer app_id;
    Timer timer;
    TimerTask swapTimerTask;
    @Nullable
    CustomWebView newWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preference = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent = getIntent();
        System.out.println("trace | onCreate");
        if (intent != null) {
            // -------------------------------------
            // "next app" --------------------------
            //nextApp = intent.getStringExtra("next_app");
            app_id = intent.getIntExtra("app_id", 2);
            nextApp = Constants.HTML_APPS[app_id];
            System.out.println("trace | webActivity =============  Must Run App:" + nextApp);
        }

        // SCREEN_BRIGHT_WAKE_LOCK =================
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_web);
        setupClickListeners();

        webContainer = findViewById(R.id.new_web_container);
        // load screen -----------------------------
        loadHtml(nextApp);
        startTimer();
    }

    // set listeners for all buttons ----------------
    private void setupClickListeners() {
        // show splash ------------------------
        findViewById(R.id.btnCtrl).setOnClickListener(view -> {
           // onBackPressed();
            sendNextAction(Constants.SWAP_SCREEN);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportActionBar().hide();
        GlobalUtils.hideSystemUI(webContainer);

        if (!GlobalUtils.isConnectingToInternet(getApplicationContext())) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.msg_not_wifi_connection), Toast.LENGTH_LONG).show();
        }
        // start swap timer ------
        // startTimer();
    }

    @Override
    protected void onStop() {
        stopTimer();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("trace newWebView • onDestroy");
        webContainer.removeAllViews();
        if (newWebView != null) {
            newWebView.destroy();
            newWebView = null;
        }

        //android.os.Process.killProcess(android.os.Process.myPid());
        stopTimer();
    }

    @Override
    public void onLowMemory() {
        System.out.println("trace • onLowMemory");
        super.onLowMemory();
    }

    @NonNull
    private CustomWebView createWebView() {
//        if (newWebView != null) {
//            ((ViewGroup) newWebView.getParent()).removeView(newWebView);
//            newWebView.removeAllViews();
//            newWebView.destroy();
//            newWebView = null;
//        }

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
                newWebView.callbackToUI(JSConstants.EVT_MAIN_TEST, CustomWebView.createResponse(requestContent, null));
                break;
            case JSConstants.EVT_READY:
                newWebView.callbackToUI(JSConstants.CMD_INIT, CustomWebView.createResponse(requestContent, initData(this)));
                break;
            case JSConstants.EVT_WEATHER:
                new webActivity.ReadXmlTask(this, getResources().getString(R.string.weather_xml)).execute();
                break;
            case JSConstants.EVT_NEXT:
                break;
            case JSConstants.EVT_SYNC:
                // syncData();
                break;
            case JSConstants.EVT_WOKE_UP:
                // updateOnUIThread("{cmd:" + Constants.CMD_LOAD_WEATHER + "}");
                break;
            case JSConstants.EVT_SHOW_TIME:
                // updateOnUIThread("{cmd:" + Constants.CMD_LOAD_TIMER + "}");
                break;
            case JSConstants.EVT_BACK:
                break;
            case JSConstants.EVT_PAGE_FINISHED:
//                onPageFinished();
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

//    void onPageFinished() {
//        new android.os.Handler().postDelayed(
//                new Runnable() {
//                    public void run() {
//                    }
//                }, 4000);
//    }


    // =========================================================
// Read XML Weather
// =========================================================
    private static class ReadXmlTask extends AsyncTask<Void, Void, String> {
        //        private WeakReference<FullscreenActivity> activityReference;
        private WeakReference<webActivity> activityReference;
        private final String listUrl;

        HttpURLConnection urlConnection;
        BufferedReader reader;
        String resultXML = "";

        // ----------------------------
        // only retain a weak reference to the activity
        ReadXmlTask(webActivity activity, @NonNull String list_url) {
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
            webActivity activity = activityReference.get();
            try {
                JSONObject sendData = XML.toJSONObject(strXML);
                if (sendData != null) {
                    activity.newWebView.callbackToUI(JSConstants.CMD_WEATHER_DATA, CustomWebView.createResponse(null, sendData));
                }
            } catch (JSONException e) {
                System.out.println("Unexpected JSON exception" + e);
            }
        }
    }

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
                onBackPressed();
                // new Handler().post(new FullscreenActivity.UpdateUIRunnable(Constants.CMD_LOAD_TIMER));
            } else {
                if (timer == null) {
                    // System.out.println("traceSW | startTimer");
                    String tempNumStr = preference.getString("swap_frequency", "1");
                    int swap_frequency = Integer.parseInt(tempNumStr) * 60000;
                    timer = new Timer();
                    swapTimerTask = new webActivity.SwapTimerTask();
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
                    System.out.println("traceSW | WEB_VIEW ACTIVITY Swap Timer Task ");
//                    swapScreen();
                    // onBackPressed();
//                    sendNextAction(Constants.SWAP_SCREEN);
                }
            });
        }
    }

    // =========================================================
    private void sendNextAction(String action) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("action", action);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    // ===================================
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}