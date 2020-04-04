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
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jsc.smartpanel.html.JSConstants;
import com.jsc.utils.GlobalUtils;
import com.jsc.utils.SysUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class FullscreenActivity extends AppCompatActivity {

    CommunicationServer communicationServer;
    public static SharedPreferences preference;
    Timer timer;
    TimerTask swapTimerTask;

//    @Nullable
//    CustomWebView webView;
//    @Nullable
//    CustomWebView nextWebView;
//    ViewGroup webContainer;

    private long back_pressed;
    private int cur_screen = 0;
    private int lastCMD = 0;
    private String nextApp;
    private Boolean nextKill;
    private String currentApp;
    private int test_cycles = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        preference = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = getIntent();
        System.out.println("trace | MAIN | onCreate");
        if (intent != null) {
//            Uri data = getIntent().getData();
//            if (data != null) {
//                String host = data.getHost();
//                String path = data.getPath();
//                String param = data.getQueryParameter("key1");
//                // String fragment = data.getFragment();
//                System.out.println("trace | Host:" + host + " | path:" + path + " | param:" + param);
//                //Log.e("TAG", "fragment:" + fragment);
//            }
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
                // externalCMD(Constants.CMD_LOAD_TIMER);
            }
            // -------------------------------------
            // "next kill --------------------------
            nextKill = intent.getBooleanExtra("next_kill", false);
            // -------------------------------------
            // System.out.println("trace | =============  Must Run App:" + nextApp + " | next_kill " + nextKill);
        }

        // SCREEN_BRIGHT_WAKE_LOCK =================
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_fullscreen);

        // Device set port ---------------------
        communicationServer = new CommunicationServer(this, Constants.SERVER_PORT);
        setupClickListeners();

//        webContainer = findViewById(R.id.web_container);
//        externalCMD(Constants.CMD_LOAD_TIMER);
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
        findViewById(R.id.btn_exit).setOnClickListener(
                view -> System.exit(0)
        );
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
        // findViewById(R.id.btn_smart).setOnClickListener(view -> openWebView(0));
        findViewById(R.id.btn_stats).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_STATS));
        // findViewById(R.id.btn_timer).setOnClickListener(view -> openWebView(2));
        // findViewById(R.id.btn_timer).setOnClickListener(view -> openWebView(2));
        findViewById(R.id.btn_timer).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_TIMER));
        //findViewById(R.id.btn_weather).setOnClickListener(view -> openWebView(3));
        findViewById(R.id.btn_weather).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_WEATHER));
        findViewById(R.id.btn_sling).setOnClickListener(view -> externalCMD(Constants.CMD_SLING));
        findViewById(R.id.btn_wifi).setOnClickListener(view -> externalCMD(Constants.CMD_WIFI_SCANNER));
    }

    // ====================================
    private int getHtmlID(int cmd) {
        // "smarthome.html", "smarthome/statistic.html", "timer.html", "weather.html"
        int id;
        switch (cmd) {
            case Constants.CMD_LOAD_SMART:
                id = 0;
                break;
            case Constants.CMD_LOAD_STATS:
                id = 1;
                break;
            case Constants.CMD_LOAD_WEATHER:
                id = 3;
                break;
            default:
                id = 2;
                break;
        }
        // Toast.makeText(getBaseContext(), "getHtml ID • " + id, Toast.LENGTH_SHORT).show();
        return id;
    }

    // ====================================
    private void openWebView(int app_id) {
        Intent intent = new Intent(this, webActivity.class);
        intent.putExtra("app_id", app_id);
        // intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivityForResult(intent, Constants.REQUEST_ACTIVITY_CODE);
    }

    // ====================================
    private void sendCmdToWebView(String jsonStr) {
        Intent intent = new Intent(this, webActivity.class);
        intent.putExtra("jsonStr", jsonStr);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

//    // ====================================
//    private void sendCmdToWebView(int cmd, String jsonStr) {
//        Intent intent = new Intent(this, webActivity.class);
//        intent.putExtra("cmd", cmd);
//        intent.putExtra("jsonStr", jsonStr);
//        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        // Toast.makeText(this, "sendTo WebView | cmd • " + cmd + " | json:" + jsonStr, Toast.LENGTH_SHORT).show();
//        startActivity(intent);
//        // startActivityForResult(intent, Constants.REQUEST_ACTIVITY_CODE);
//    }

    // ===================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("trace • MAIN | onActivityResult | " + resultCode);
//        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == Constants.REQUEST_ACTIVITY_CODE) {
                String action = data.getStringExtra("action");
                System.out.println("trace • MAIN | onActivityResult | Action • " + action);
                if (action != null) {
                    if (action.equalsIgnoreCase(Constants.SWAP_SCREEN)) {
                        swapScreen();
                    }
                } else {
                    super.onActivityResult(requestCode, resultCode, data);
                }
            }
        } else {
            System.out.println("trace • MAIN | onActivityResult | Wrong result");
            Toast.makeText(this, "Wrong result", Toast.LENGTH_SHORT).show();
        }
    }

    // ===================================
    @Override
    protected void onResume() {
        super.onResume();
        getSupportActionBar().hide();
//        GlobalUtils.hideSystemUI(webContainer);
        nextKill = false;
        if (!GlobalUtils.isConnectingToInternet(getApplicationContext())) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.msg_not_wifi_connection), Toast.LENGTH_LONG).show();
        }
        // start swap timer ------
        // startTimer();
        runKillAllProcess();
        watchDog();
        // openWebView(cur_screen);
//        swapScreen();
    }

    @Override
    protected void onStop() {
        // stopTimer();
        super.onStop();
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        System.out.println("trace | =============  MAIN  ACTIVITY == onPause");
//    }

    @Override
    protected void onDestroy() {
        System.out.println("trace • onDestroy");
//        if (webView != null) {
//            webView.setWebEventsListener(null);
//        }
//        webView = null;
//        nextWebView = null;
        //    stopTimer();
        communicationServer.stop();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        System.out.println("trace • onLowMemory");
        super.onLowMemory();
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
                sendCmdToWebView(jsonStr);
//                decryptCommand(jsonStr);
            }
            if (command > 0 & command < 10) {
                // Transmitted External Command  from Html Application
                System.out.println("trace | html Application command : " + command);
                externalCMD(command);
            }
            if (command > 9) {
                openWebView(command - 10);
            }
        }
    }

    // ===================================
//    private void decryptCommand(String data) {
//        if (data == null) {
//            System.out.println("decryptCommand | data null");
//            return;
//        }
//
//        try {
//            JSONObject clientRequest = new JSONObject(data);
//            if (clientRequest.has("cmd")) {
//                int cmd = clientRequest.optInt("cmd", 0);
//                if (clientRequest.has("json")) {
//                    JSONObject json = clientRequest.optJSONObject("json");
//                    if (json != null) {
//                        String jsonStr = "json:" + json.toString();
//                        externalCMD(cmd, jsonStr);
//                    }
//                } else {
//                    externalCMD(cmd);
//                }
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }

//    // ===================================
//    private void externalCMD(int cmd, String jsonStr) {
//        Toast.makeText(getBaseContext(), "Transmitted External Command  | DEC • " + cmd + " | " + jsonStr, Toast.LENGTH_SHORT).show();
//        switch (cmd) {
//            case Constants.CMD_BACK_LIGHT:
//  //             sendCmdToWebView(cmd, jsonStr);
////                if (json.has("state")) {
////                    SysUtils.setBackLight(this, json.optBoolean("state", true));
////                }
//                break;
//            case Constants.CMD_DEBUG_MODE:
//                break;
//            default:
//                Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_unsupported), Toast.LENGTH_SHORT).show();
//                break;
//        }
//    }
//
//    // ===================================
//    private void externalCMD(int cmd, @NonNull JSONObject json) {
//
//
//        Toast.makeText(getBaseContext(), "Transmitted External Command  | DEC • " + cmd + " | " + json.toString(), Toast.LENGTH_SHORT).show();
//        switch (cmd) {
//            case Constants.CMD_BACK_LIGHT:
//                if (json.has("state")) {
//                    SysUtils.setBackLight(this, json.optBoolean("state", true));
//                }
//                break;
//            case Constants.CMD_DEBUG_MODE:
//                break;
//            default:
//                Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_unsupported), Toast.LENGTH_SHORT).show();
//                break;
//        }
//    }

    // ===================================
    private void externalCMD(int cmd) {
        // TODO debug must remove
        // Toast.makeText(getBaseContext(), "Transmitted External Command • $" + String.format("%X", cmd) + " | DEC • " + cmd, Toast.LENGTH_SHORT).show();
//
//        if (cmd == lastCMD) {
//            View splash = findViewById(R.id.splash);
//            if (splash.getVisibility() == View.VISIBLE) {
//                splash.setVisibility(View.GONE);
//            }
//            return;
//        }
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
                runApplication(cmd);
                break;
            case Constants.CMD_LOAD_STATS:
                runApplication(cmd);
                break;
            // timer ---------------------
            case Constants.CMD_LOAD_TIMER:
                runApplication(cmd);
                break;
//            case Constants.CMD_TIMER_SWAP:
//                //sendCmdToWebView(JSConstants.CMD_SWAP, "{}");
//                //webView.callbackToUI(JSConstants.CMD_SWAP);
//                break;

            // weather forecast ----------
//            case Constants.CMD_LOAD_WEATHER:
//                runApplication(cmd);
//                break;
//            case Constants.CMD_WEATHER_FORECAST:
//                // webView.callbackToUI(JSConstants.CMD_SWAP);
//               //sendCmdToWebView(JSConstants.CMD_SWAP, "{}");
//                break;
//            case Constants.CMD_WEATHER_MAGIC:
//                // webView.callbackToUI(JSConstants.CMD_SEASON_MAGIC);
//                //sendCmdToWebView(JSConstants.CMD_SEASON_MAGIC, "{}");
//                break;
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
    private void runApplication(int cmd) {
        lastCMD = cmd;
        currentApp = Constants.HTML_APPS[getHtmlID(cmd)];
        if (nextKill) {
            SysUtils.restartApp(this, currentApp, true);
            nextKill = false;
        } else {
            // load next html application
            openWebView(getHtmlID(cmd));
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
            Toast.makeText(getBaseContext(),
                    "app IDX • " + app_idx + " | CMD • " + cmd
                    , Toast.LENGTH_SHORT).show();
            // load next html application
            //  loadHtml(Constants.HTML_APPS[app_idx]);
        }
    }

    // ===================================
    private void runExternalApplication(int app_idx, int cmd) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(Constants.PACKAGES[app_idx]);
        if (intent != null) {
            nextKill = true;
            lastCMD = cmd;
            currentApp = Constants.PACKAGES[app_idx];
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
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
            Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_exit),
                    Toast.LENGTH_SHORT).show();
//            System.out.println(" trace | App " + currentApp.substring(nextApp.length() - 5));
//            // main application ------------------
//            if (currentApp.substring(nextApp.length() - 5).equals(".html")) {
//                Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_exit),
//                        Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(getBaseContext(), "I think about it",
//                        Toast.LENGTH_SHORT).show();
//                // backPrevApp(currentApp);
//                // backMain();
//            }
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

        System.out.println("trace • MAIN | Swap Screen | next • " + String.valueOf(cur_screen));
        new Handler().post(new UpdateUIRunnable(cur_screen + 10));
    }

    // ===================================
    private void runKillAllProcess() {
        SysUtils.killAllProcess(this);
        nextKill = false;
        // all processes are already killed ---------------
    }

    // ===================================
    private void watchDog() {
        // restart app if low memory
        long memSize = SysUtils.getFreeMemory(this);
        if (memSize < 300) {
            SysUtils.restartApp(this);
        }

        // ********************************
        // this code must remove after debug
        // ********************************
        // memory leak --------------------
        test_cycles++;
        String msgMemory = "FREE RAM : " + memSize + " Mb | CYCLES : " + test_cycles;
        TextView textInfo = findViewById(R.id.memInfo);
        textInfo.setText(msgMemory);
        // ********************************
    }

    // ===================================
    // Swap Screen Timer
    // ===================================

    // ===================================
//    private void startTimer() {
//        // ***********************************************
//        // work if Auto Swap Screens is enabled
//        // only during the daytime
//        // ***********************************************
//        if (preference.getBoolean("sw_swap", true)) {
//            if (SysUtils.isNight(
//                    preference.getString("start_day", "6"),
//                    preference.getString("start_night", "20"))) {
//                System.out.println("traceSW | Next stopTimer");
//                stopTimer();
//                new Handler().post(new UpdateUIRunnable(Constants.CMD_LOAD_TIMER));
//            } else {
//                if (timer == null) {
//                    // System.out.println("traceSW | startTimer");
//                    String tempNumStr = preference.getString("swap_frequency", "1");
//                    int swap_frequency = Integer.parseInt(tempNumStr) * 60000;
//                    timer = new Timer();
//                    swapTimerTask = new SwapTimerTask();
//                    timer.schedule(swapTimerTask, 60000, swap_frequency);
//                }
//            }
//        }
//    }

//    // ===================================
//    private void stopTimer() {
//        if (timer != null) {
//            timer.cancel();
//            timer = null;
//            swapTimerTask = null;
//        }
//    }
//
//    // ===================================
//    class SwapTimerTask extends TimerTask {
//        @Override
//        public void run() {
//
//            // --------------------------
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    System.out.println("traceSW | Swap Timer Task ");
//                    swapScreen();
//                }
//            });
//        }
//    }
}
