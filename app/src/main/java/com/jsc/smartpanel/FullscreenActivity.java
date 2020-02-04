package com.jsc.smartpanel;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.TextView;
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

import jsinterface.JSConstants;
import jsinterface.JSOut;
import utils.GlobalUtils;
// import utils.PackageCreator;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    CommunicationServer communicationServer;
    private static long back_pressed;
    public static int port;
    private TextView responseHeader;
    private TextView responseData;
    private View splash;
    public static SharedPreferences preference;

    private static boolean Night = false;
    private static boolean debugMode = false;
    private static int cur_screen = 1;
    private static int lastCMD = 0;
    private static boolean sleep_mode = false;
    WebView webView;

    // js interface ------------
    protected JSOut jsOut;
    public static JSONObject uiRequest;
    public static JSONObject sendData;
    // -------------------------

    private View mControlsView;
    private boolean mVisible;

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
        }

        // SCREEN_BRIGHT_WAKE_LOCK
        // PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        mWakeLock = mPowerManager.newWakeLock(mWakeLockState,
//                "UMSE PowerTest");
//        if (mWakeLock != null) {
//            mWakeLock.acquire();
//        }
        setContentView(R.layout.activity_fullscreen);

        // server tcpip ------------------------------
        communicationServer = new CommunicationServer(this);

        responseHeader = findViewById(R.id.resHeader);
        responseData = findViewById(R.id.resData);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);

        setupClickListeners();

        // ------------------------------------
        // WebView
        // ------------------------------------
        webView = findViewById(R.id.web_view);
        webView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
        webView.setWebViewClient(new NocWebViewClient());

        // ---------------------------------------------------
        externalCMD(Constants.CMD_LOAD_TIMER);
    }

    // set listeners for all buttons
    private void setupClickListeners() {
        splash = findViewById(R.id.splash);

        // show settings --------------
        findViewById(R.id.btnShowCtrl).setOnClickListener(view -> {
            if (splash.getVisibility() == View.VISIBLE) {
                splash.setVisibility(View.GONE);
            } else {
                splash.setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.icon1).setOnClickListener(view -> {
            externalCMD(Constants.CMD_RADIO);
            splash.setVisibility(View.GONE);
        });

        findViewById(R.id.icon2).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_SMART));

        findViewById(R.id.icon3).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_STATS));

        findViewById(R.id.icon4).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_TIMER));

        findViewById(R.id.icon5).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_WEATHER));

        findViewById(R.id.icon6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                externalCMD(Constants.CMD_SLING);
                splash.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.icon7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();
                Intent configIntent = new Intent(context, SettingsActivity.class);
                configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(configIntent);
            }
        });
    }

    // ==============================================
    // Loading a page with a self-signed certificate
    // ==============================================
    private class NocWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            System.out.println("[ trace  ] onPage Finished : " + url);

            // progressBar.setVisibility(View.GONE);
            // bugfix text web page -----------------
//            if (err) {
//                // webView.setBackgroundColor(0x66ffffff);
//                webView.setBackgroundColor(Color.WHITE);
//                webView.setBackgroundResource(R.drawable.gradient_splash);
//                p.leftMargin = (int) getResources().getDimension(R.dimen.err_msg_margin);
//                p.rightMargin = (int) getResources().getDimension(R.dimen.err_msg_margin);
//                err = false;
//            } else {
//                webView.setBackgroundColor(Color.BLACK);
//                p.leftMargin = 0;
//                p.rightMargin = 0;
//            }
//            webView.setLayoutParams(p);

            // ==============================================
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            // webView.setVisibility(View.VISIBLE);
                            splash.setVisibility(View.GONE);
                        }
                    }, 500);
            // ==============================================

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        GlobalUtils.hideSystemUI(webView);

        if (!GlobalUtils.isConnectingToInternet(getApplicationContext())) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.msg_not_wifi_connection), Toast.LENGTH_LONG).show();
        }
        // performed by a separate request ==================
        // new ReadXmlTask(FullscreenActivity.this).execute();
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
        WebView webView = findViewById(R.id.web_view);
        boolean clear_cache = preference.getBoolean("sw_clear_cache", false);
        if (clear_cache) {
            webView.clearCache(true);
        }
        webView.loadUrl(url);

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
                    sleep_mode = requestContent.optBoolean("sleep_mode", false);
                    setBackLight(sleep_mode);
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
        // --------------------------------------------------
        private WeakReference<FullscreenActivity> activityReference;
        private final String listUrl;

        // only retain a weak reference to the activity
        ReadXmlTask(FullscreenActivity activity, @NonNull String list_url) {
            activityReference = new WeakReference<>(activity);
            listUrl = list_url;
        }

        HttpURLConnection urlConnection;
        BufferedReader reader;
        String resultXML = "";

        // ----------------------------------------------------
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

        // ----------------------------------------------------
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


    public void updateOnUIThread(String str) {
        runOnUiThread(new UpdateUIRunnable(str));
    }

    // ----------------------------------------------------
    class UpdateUIRunnable implements Runnable {
        private String msg;
        private int command;

        private UpdateUIRunnable(String str) {
            this.msg = str;
        }

        private UpdateUIRunnable(int cmd) {
            this.command = cmd;
        }

        @Override
        public void run() {
            if (msg != null) {
                // String header = msg.substring(2, 27);
                // int body_length = msg.length() - 29;
                // System.out.println("decryptCommand | body size: " + body_length);
                // String body = (body_length > 0) ? msg.substring(27, msg.length() - 2) : "";
                decryptCommand(msg);
                //responseValue.setText(header);
            }
            if (command > 0) {
                // System.out.println("decryptCommand | command : " + command);
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
        String header = data.substring(2, 27);
        int body_length = data.length() - 29;
        // System.out.println("decryptCommand | body size: " + body_length);
        String body = (body_length > 0) ? data.substring(27, data.length() - 2) : "";

//        System.out.println("decryptCommand | header:" + header + " | size: " + data.length());
//        System.out.println("decryptCommand | data:" + data.substring(2, 27) + " | size: " + data.length());
//        System.out.println("decryptCommand | body:" + convertHexToString(body.replaceAll(" ", "")) + " | size: " + body_length);
//
        responseHeader.setText(header);
        responseData.setText(body);
        // int cmd = PackageCreator.getCommandID(header);
        // externalCMD(cmd);
    }

    // ===================================
    private void externalCMD(int cmd) {
        System.out.println(" trace | external CMD : " + cmd);
        Intent intent;
        String root = preference.getBoolean("sw_debug_mode", false) || debugMode
                ? getResources().getString(R.string.root_debug) :
                getResources().getString(R.string.root);
        // responseData.setText(Integer.toHexString(cmd));
        if (cmd == lastCMD) {
            if (splash.getVisibility() == View.VISIBLE) {
                splash.setVisibility(View.GONE);
            }
            return;
        }
        ActivityManager activityMgr = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        try {
            if (lastCMD == Constants.CMD_RADIO) {
                activityMgr.killBackgroundProcesses(getResources().getString(R.string.pkg_radio));
            }
            if (lastCMD == Constants.CMD_SLING) {
                activityMgr.killBackgroundProcesses(getResources().getString(R.string.pkg_sling_player));
            }
        } catch (Exception e) {
//            System.out.println(" trace | App not active");
        }
        switch (cmd) {
            case Constants.CMD_RESTART:
                lastCMD = cmd;
                restartApp();
                break;
            case Constants.CMD_BACK:
                onBackPressed();
                break;
            case Constants.CMD_BACK_LIGHT:
                sleep_mode = !sleep_mode;
                setBackLight(sleep_mode);
                break;
            case Constants.CMD_DEBUG_MODE:
                View debugContainer = findViewById(R.id.debug_panel);
                if (debugContainer.getVisibility() == View.VISIBLE) {
                    debugContainer.setVisibility(View.GONE);
                } else {
                    debugContainer.setVisibility(View.VISIBLE);

                }
                debugMode = debugContainer.getVisibility() == View.VISIBLE;
                break;
            // menu ======================
            case Constants.CMD_RADIO:
                lastCMD = cmd;
                if (detectApp(getApplicationContext(), getResources().getString(R.string.pkg_radio))) {
                    intent = new Intent();
                    intent.setComponent(new ComponentName(getResources().getString(R.string.pkg_radio),
                            getResources().getString(R.string.pkg_radio) + getResources().getString(R.string.app_entry)));
                    // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_app_not_installed), Toast.LENGTH_SHORT).show();
                break;
            case Constants.CMD_LOAD_SMART:
                lastCMD = cmd;
                loadHtml(root + getResources().getString(R.string.html_smarthome));
                break;
            case Constants.CMD_LOAD_STATS:
                lastCMD = cmd;
                loadHtml(root + getResources().getString(R.string.html_stats));
                break;
            // timer ---------------------
            case Constants.CMD_LOAD_TIMER:
                lastCMD = cmd;
                loadHtml(root + getResources().getString(R.string.html_timer));
                break;
            case Constants.CMD_TIMER_SWAP:
                callbackToUI(JSConstants.CMD_SWAP, createResponse(null, null));
                break;

            // weather forecast ----------
            case Constants.CMD_LOAD_WEATHER:
                lastCMD = cmd;
                loadHtml(root + getResources().getString(R.string.html_weather));
                break;
            case Constants.CMD_WEATHER_FORECAST:
                callbackToUI(JSConstants.CMD_SWAP, createResponse(null, null));
                break;
            case Constants.CMD_WEATHER_MAGIC:
                callbackToUI(JSConstants.CMD_SEASON_MAGIC, createResponse(null, null));
                break;
            // --------------------------

            case Constants.CMD_SLING:
                lastCMD = cmd;
                if (detectApp(getApplicationContext(), getResources().getString(R.string.pkg_sling_player))) {
                    intent = new Intent();
                    intent.setComponent(new ComponentName(getResources().getString(R.string.pkg_sling_player),
                            getResources().getString(R.string.pkg_sling_player) + getResources().getString(R.string.app_entry)));
                    startActivity(intent);
                } else
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_app_not_installed), Toast.LENGTH_SHORT).show();
                break;
            // ===========================
            default:
//                responseData.setText(getResources().getString(R.string.msg_not_support_cmd) + " | " + body);
//                responseData.setText(String.format(getResources().getString(R.string.msg_not_support_cmd) + "%d", String.valueOf(cmd)));
//                responseData.setText(getResources().getString(R.string.msg_not_support_cmd));
                break;
        }
    }

    // ===================================
    public void setBackLight(Boolean sleep_mode) {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        if (sleep_mode) {
            layout.screenBrightness = 0.3F;
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
            Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_exit),
                    Toast.LENGTH_SHORT).show();
        }
        back_pressed = System.currentTimeMillis();
    }

    // ===================================
    private void restartApp() {
        // перезагрузка приложением самого себя ----------------
        Context context = getApplicationContext();
        Intent mStartActivity = new Intent(context, FullscreenActivity.class);
        int mPendingIntentId = PendingIntent.FLAG_UPDATE_CURRENT;
        // int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }

    // ===================================
    public static Boolean detectApp(Context c, String packageName) {
        // if (Build.VERSION.SDK_INT < 5) return false;
        PackageManager pm = c.getPackageManager();
        try {
            if (pm.getPackageInfo(packageName, 0) != null)
                return true;
        } catch (PackageManager.NameNotFoundException e) {
            System.out.println(" trace | App " + packageName + " not instaled");
        }
        return false;
    }
}
