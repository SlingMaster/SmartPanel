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
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import jsinterface.JSConstants;
import jsinterface.JSOut;
import utils.GlobalUtils;
import utils.PackageCreator;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    private ServerSocket serverSocket;
    Handler updateConversationHandler;
    Thread serverThread = null;
    boolean b_exit;

    private static long back_pressed;
    public static int port;
    View splash;
    private TextView responseHeader;
    private TextView responseData;

    //    TextView msg;
    public static Resources mResources;
    public static SharedPreferences preference;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;
    private static boolean Night = false;
    private static boolean debugMode = false;
    private static int cur_screen = 1;
    private static int lastCMD = 0;
    private static boolean sleep_mode = false;
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
//    private static Context mContext;
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    WebView webView;

    // js interface ------------
    protected JSOut jsOut;
    public static JSONObject uiRequest;
    public static JSONObject sendData;
    // -------------------------

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            webView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
//            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View btnShow;
        View btnApp1;
        View btnApp2;
        View btnApp3;
        View btnApp4;
        View btnApp5;
        View btnApp6;
        View btnApp7;
//        View btnApp8;
        mResources = getResources();

        super.onCreate(savedInstanceState);
        preference = PreferenceManager.getDefaultSharedPreferences(this);

        // Device set port ---------------------
        String strPort = preference.getString("server_port", getResources().getString(R.string.def_port));
        if (strPort != null) {
            port = Integer.valueOf(strPort);
        }
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


//        mContext = getApplicationContext();
        // rootApp = getResources().getString(R.string.root);
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
        updateConversationHandler = new Handler();
        serverThread = new Thread(new ServerThread());
        serverThread.start();

        responseHeader = findViewById(R.id.resHeader);
        responseData = findViewById(R.id.resData);
        // -------------------------------------------


        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
//        mContentView = findViewById(R.id.fullscreen_content);
        btnShow = findViewById(R.id.btnShowCtrl);
        // show settings --------------
        btnShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (splash.getVisibility() == View.VISIBLE) {
                    splash.setVisibility(View.GONE);
                } else {
                    splash.setVisibility(View.VISIBLE);
                }
            }
        });
        // run application --------------
        splash = findViewById(R.id.splash);
        btnApp1 = findViewById(R.id.icon1);
        btnApp2 = findViewById(R.id.icon2);
        btnApp3 = findViewById(R.id.icon3);
        btnApp4 = findViewById(R.id.icon4);
        btnApp5 = findViewById(R.id.icon5);
        btnApp6 = findViewById(R.id.icon6);
        btnApp7 = findViewById(R.id.icon7);
//        btnApp8 = findViewById(R.id.icon8);

        btnApp1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                externalCMD(Constants.CMD_RADIO);
                splash.setVisibility(View.GONE);
            }
        });

        btnApp2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                externalCMD(Constants.CMD_LOAD_SMART);
            }
        });

        btnApp3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                externalCMD(Constants.CMD_LOAD_STATS);
            }
        });

        btnApp4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                externalCMD(Constants.CMD_LOAD_TIMER);
            }
        });

        btnApp5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                externalCMD(Constants.CMD_LOAD_WEATHER);
            }
        });

        btnApp6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                externalCMD(Constants.CMD_SLING);
                splash.setVisibility(View.GONE);
            }
        });

        btnApp7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();
                Intent configIntent = new Intent(context, SettingsActivity.class);
                configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(configIntent);
            }
        });

//        btnApp8.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                loadHtml(getResources().getString(R.string.debug_js_interface));
//                splash.setVisibility(View.GONE);
//                // externalCMD(Constants.CMD_DEBUG_MODE);
//                // splash.setVisibility(View.GONE);
//            }
//        });


        //  end run application =======================================================


        // Set up the user interaction to manually show or hide the system UI.
//        mContentView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                toggle();
//            }
//        });

        // ------------------------------------
        // WebView
        // ------------------------------------
        // webView = (WebView) findViewById(R.id.web_view);
        webView = findViewById(R.id.web_view);
        // set WebView Style background and scrollbar ----
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        //webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        //webView.setScrollbarFadingEnabled(false);
        // set default black color ------------
        webView.setBackgroundColor(0);

        // web settings --------------------------------------
        WebSettings webSettings = webView.getSettings();

        // ??? ---------------------------------------
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        // ??? ---------------------------------------

        // включаем поддержку JavaScript
        webView.getSettings().setJavaScriptEnabled(true);
        // определим экземпляр MyWebViewClient.
        // Он может находиться в любом месте после инициализации объекта WebView
        webView.setWebViewClient(new MyWebViewClient());
//        webView.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public void onProgressChanged(WebView view, int newProgress) {
//                //change your progress bar
//                Log.w(TAG, "onProgressChanged : " + newProgress + "%");
//                if (newProgress > 50) {
//                    //progressBar.setVisibility(View.GONE);
//                    webView.setVisibility(View.VISIBLE);
//                }
//            }
//        });

        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setAllowFileAccess(true);

        // set cash app ---------------------------------------
        webSettings.setAppCacheEnabled(true);
        // set wiewport scale ---------------
        // webSettings.setUseWideViewPort(isWideViewPortRequired());
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
        webView.setWebViewClient(new NocWebViewClient());

        // ---------------------------------------------------
        // externalCMD(Constants.CMD_LOAD_SMART);
        externalCMD(Constants.CMD_LOAD_TIMER);
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
    protected void onStart() {
        super.onStart();
//        externalCMD(Constants.CMD_DEBUG_MODE);
        System.out.println("trace • onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!GlobalUtils.isConnectingToInternet(getApplicationContext())) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.msg_not_wifi_connection), Toast.LENGTH_LONG).show();
            return;
        }
        new readXmlTask(FullscreenActivity.this).execute();
    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//        System.out.println("trace • onStop");
//        try {
//            serverSocket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    protected void onDestroy() {
        System.out.println("trace • onDestroy");
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        System.out.println("trace • onLowMemory");
        super.onLowMemory();
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//
//        // Checks the orientation of the screen
//        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
//        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
//            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
//        }
//    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
//        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        webView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    // ----------------------------------------
    private class MyWebViewClient extends WebViewClient {
        //        @TargetApi(19)
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

            view.loadUrl(request.getUrl().toString());
            return true;
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
    public void serviceEvents(int request, final String jsonString) {
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

        // PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");

        switch (request) {
            case JSConstants.EVT_MAIN_TEST:
                callbackToUI(JSConstants.EVT_MAIN_TEST, createResponse(requestContent, null));
                break;
            case JSConstants.EVT_READY:
                // AppUI = requestContent.optString("ui");
                callbackToUI(JSConstants.CMD_INIT, createResponse(requestContent, initData()));
//                if (AppUI.equalsIgnoreCase("project_weather")) {
//                 } else {
//                    callbackToUI(JSConstants.CMD_INIT, createResponse(requestContent, stateData()));
//                }

                break;
            case JSConstants.EVT_WEATHER:
                new readXmlTask(FullscreenActivity.this).execute();
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
            updateConversationHandler.post(new updateUIThread(external_cmd));
        }
    }

    // ----------------------------------------
    public JSONObject initData() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("android_os", android.os.Build.VERSION.SDK_INT);
            obj.put("language", "en");
            obj.put("phone_ui", !GlobalUtils.isTablet(getApplicationContext()));
            obj.put("android_app", true);
            obj.put("node_url", preference.getString("ip_weather", getResources().getString(R.string.def_node_weather_url)));
            obj.put("node_bathroom_url", preference.getString("ip_bathroom", getResources().getString(R.string.def_node_weather_url)));
            obj.put("chip_weather", preference.getString("chip_weather", getResources().getString(R.string.def_node_weather_chip)));
            obj.put("chip_bathroom", preference.getString("chip_bathroom", getResources().getString(R.string.def_node_bathroom_chip)));
            obj.put("auto_start_night_mode", preference.getBoolean("sw_auto_start", false));

            // string to int ---------------------
            String tempNumStr = preference.getString("start_day", "6");
            int num = Integer.parseInt(tempNumStr != null ? tempNumStr : "6");
            obj.put("start_day", num);
            tempNumStr = preference.getString("start_night", "20");
            num = Integer.parseInt(tempNumStr != null ? tempNumStr : "20");
            obj.put("start_night", num);
            tempNumStr = preference.getString("swap_frequency", "1");
            num = Integer.parseInt(tempNumStr != null ? tempNumStr : "1");
            obj.put("swap_frequency", num);
            // -----------------------------------
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // System.out.println("trace initData : " + obj.toString());
        return obj;
    }

    // =========================================================
    // Interface HTML > Application
    // =========================================================
    private class JSIn {

        private JSIn() {
        }

        @JavascriptInterface
        // @SuppressLint("callNative")
        public final void callNative(int request, final String jsonString) {
            // HTML function send data to application -----------------------------
            serviceEvents(request, jsonString);
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
    private static class readXmlTask extends AsyncTask<Void, Void, String> {

        // private class readFirmwareListTask extends AsyncTask<Void, Void, String> {
        // --------------------------------------------------
        private WeakReference<FullscreenActivity> activityReference;

        // only retain a weak reference to the activity
        readXmlTask(FullscreenActivity context) {
            activityReference = new WeakReference<>(context);
        }

        // ----------------------------------------------------
        //preference = PreferenceManager.getDefaultSharedPreferences(MainActivity context);
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String resultXML = "";

        String list_url = mResources.getString(R.string.weather_xml);

        @Override
        protected String doInBackground(Void... params) {
            // System.out.println("trace | list_url : " + list_url);
            try {

                URL url = new URL(list_url);

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

        @Override

        protected void onPostExecute(String strXML) {
            super.onPostExecute(strXML);
//            System.out.println("trace | onPostExecute: " + strXML);
//            JSONObject json;
            // get a reference to the activity if it is still there
            FullscreenActivity activity = activityReference.get();
            try {
                sendData = XML.toJSONObject(strXML);
            } catch (JSONException e) {
                System.out.println("Unexpected JSON exception" + e);
            }
            activity.callbackToUI(JSConstants.CMD_WEATHER_DATA, activity.createResponse(uiRequest, sendData));
            // ----------------------------------------------------
        }
    }

    // server -------------------------------------------
    // ----------------------------------------------------
    class ServerThread implements Runnable {

        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(8080);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = serverSocket.accept();
                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // --------------------------------------------
            if (b_exit) {
                System.out.println("exit TCP Server... n");

//                try {
//                    client.close();
//                } catch (IOException ex) {
//                    System.out.println("client close exception...: " + ex.getMessage() + "n");
//                }

                try {
                    serverSocket.close();
                } catch (IOException ex) {
                    System.out.println("socket close exception...: " + ex.getMessage() + "n");
                }
            }


            // --------------------------------------------
        }
    }

    // ----------------------------------------------------
    class CommunicationThread implements Runnable {

        Socket clientSocket;
        // private BufferedReader input;
        // -----------------------------------
        /* создаем буфер для данных */
        byte[] buffer;
        private InputStream in;
        // -----------------------------------

        public CommunicationThread(Socket clientSocket) {
            // private CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;

            try {
                this.buffer = new byte[16];
                this.in = this.clientSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {

                try {
//                    String read = input.readLine();
                    int countS = in.read(buffer, 0, buffer.length);
                    if (countS > 0) {
                        // decode data header, restore src CO and DB ----
                        byte[] header;
                        header = PackageCreator.decodePackage(PackageCreator.copyPartArray(buffer, 0, countS));
                        String headerStr = PackageCreator.getHeaderStr(header);
                        //System.out.println("========== TcpClient header length: " + header.length + " | headerStr : " + headerStr );
                        updateConversationHandler.post(new updateUIThread(headerStr));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    // ----------------------------------------------------
    class updateUIThread implements Runnable {
        private String msg;
        private int command;

        private updateUIThread(String str) {
            this.msg = str;
        }

        private updateUIThread(int cmd) {
            this.command = cmd;
        }

        @Override
        public void run() {
//            text.setText(text.getText().toString() + "Client Says: " + msg + "\n");
            if (msg == null) {
                // System.out.println("decryptCommand | data null");
            } else {
                // String header = msg.substring(2, 27);
                int body_length = msg.length() - 29;
                // System.out.println("decryptCommand | body size: " + body_length);
                // String body = (body_length > 0) ? msg.substring(27, msg.length() - 2) : "";
                decryptCommand(msg);
                //responseValue.setText(header);
            }
            if (command == 0) {
                // System.out.println("decryptCommand | data null");
            } else {
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
        int cmd = PackageCreator.getCommandID(header);
        externalCMD(cmd);
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
                    super.finish();
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

//    private Boolean isSuccessfully(String response) {
//        return true;
//        //        return sendDataValue.getText().equals(response);
//    }


    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
//            if (b_exit) super.onBackPressed();
//            b_exit = true;
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
//        String installer = pm.getInstallerPackageName(c.getPackageName());
//        if (installer != null && installer.equals("com.jsc.smartpanel"))
//            return true;
//        if (Build.MODEL.equalsIgnoreCase("Kindle Fire"))
//            return true;;

        try {
            if (pm.getPackageInfo(packageName, 0) != null)
                return true;
        } catch (PackageManager.NameNotFoundException e) {
            System.out.println(" trace | App " + packageName + " not instaled");
        }
        return false;
    }
}
