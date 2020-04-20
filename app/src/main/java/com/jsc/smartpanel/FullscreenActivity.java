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
import android.widget.TextView;
import android.widget.Toast;

import com.jsc.utils.GlobalUtils;
import com.jsc.utils.SysUtils;

import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;

public class FullscreenActivity extends AppCompatActivity {

    CommunicationServer communicationServer;
    public static SharedPreferences preference;
    public boolean isActivityBackground = true;
    private long back_pressed;
    private int cur_screen = 0;

    private Boolean nextKill;
    private int test_cycles = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        runKillAllProcess();
        preference = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = getIntent();
        System.out.println("trace | MAIN | onCreate");
        if (intent != null) {
            // "next app" --------------------------
            String nextApp = intent.getStringExtra("next_app");
            if (nextApp != null) {
                int slash = nextApp.lastIndexOf('/');
                if (slash >= 0)
                    nextApp = nextApp.substring(slash);
                Toast.makeText(getBaseContext(), "Next: " + nextApp, Toast.LENGTH_SHORT).show();
            } else {
                // first start -------------------------
                Handler handler = new Handler();
                handler.postDelayed(() -> externalCMD(Constants.CMD_LOAD_TIMER), 2000);
            }

            // "next app to kill --------------------------
            nextKill = intent.getBooleanExtra("next_kill", false);
        }

        setContentView(R.layout.activity_fullscreen);
        // Device set port ---------------------
        communicationServer = new CommunicationServer(this, Constants.SERVER_PORT);
        setupClickListeners();
    }

    // set listeners for all buttons -------------------
    private void setupClickListeners() {
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
        findViewById(R.id.btn_stats).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_STATS));
        findViewById(R.id.btn_timer).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_TIMER));
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
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra("app_id", app_id);
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Toast.makeText(getBaseContext(), "openWebView ID • " + app_id, Toast.LENGTH_SHORT).show();
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivityForResult(intent, Constants.REQUEST_ACTIVITY_CODE);
    }

    // ====================================
    private void sendCmdToWebView(String jsonStr) {
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra("jsonStr", jsonStr);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    // ===================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("trace • MAIN | onActivityResult | " + resultCode);
//        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == Constants.REQUEST_ACTIVITY_CODE) {
                String action = data.getStringExtra("action");
                int cmd = data.getIntExtra("cmd", 0);
                System.out.println("trace • MAIN | onActivityResult | Action • " + action);
                if (action != null) {
                    switch (action) {
                        case Constants.SWAP_SCREEN:
                            swapScreen();
                            break;
                        case Constants.SYNC:
                            Toast.makeText(this, "Sync Data", Toast.LENGTH_SHORT).show();
                            break;
                        case Constants.WOKE_UP:
                            runApplication(Constants.CMD_LOAD_WEATHER);
                            break;
                        case Constants.SHOW_TIME:
                            runApplication(Constants.CMD_LOAD_TIMER);
                            break;
                        case Constants.LOAD_SCREEN:
                            Toast.makeText(this, "LOAD_SCREEN | CMD • " + cmd, Toast.LENGTH_SHORT).show();
                            if (cmd > 9) {
                                runApplication(cmd);
                            }
                            break;
                        default:
                            break;
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

        Objects.requireNonNull(getSupportActionBar()).hide();
        // GlobalUtils.hideSystemUI(webContainer);
        nextKill = false;
        if (!GlobalUtils.isConnectedToInternet(getApplicationContext())) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.msg_not_wifi_connection), Toast.LENGTH_LONG).show();
        }

        watchDog();
        isActivityBackground = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityBackground = true;
        System.out.println("trace | =============  MAIN  ACTIVITY == onPause");
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
            } else {
                externalCMD(command);
            }
        }
    }

    // ===================================
    private void externalCMD(int cmd) {
        // TODO debug must remove
        Toast.makeText(getBaseContext(), "Transmitted External Command • $" + String.format("%X", cmd) + " | DEC • " + cmd, Toast.LENGTH_SHORT).show();
        switch (cmd) {
            case Constants.CMD_RESTART:
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
                runExternalApplication(1);

                break;
            // html app ------------------
            case Constants.CMD_LOAD_SMART:
            case Constants.CMD_LOAD_STATS:
            case Constants.CMD_LOAD_TIMER:
            case Constants.CMD_LOAD_WEATHER:
                if (isActivityBackground) {
                    sendCmdToWebView("{cmd:" + Constants.CMD_BACK + "}");
                    nextKill = false;
                } else {
                    runApplication(cmd);
                }
                break;
            // --------------------------
            case Constants.CMD_SLING:
                runExternalApplication(2);
                break;
            // --------------------------
            case Constants.CMD_WIFI_SCANNER:
                runExternalApplication(3);
                break;
            // ===========================
            default:
                Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_unsupported), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    // ===================================
    private void runApplication(int cmd) {
        if (nextKill) {
            SysUtils.restartApp(this, Constants.HTML_APPS[getHtmlID(cmd)], true);
            nextKill = false;
        } else {
            // load next html application
            openWebView(getHtmlID(cmd));
        }
    }

    // ===================================
    private void runExternalApplication(int app_idx) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(Constants.PACKAGES[app_idx]);
        if (intent != null) {
            nextKill = true;
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
        }
        back_pressed = System.currentTimeMillis();
    }

    // ===================================
    public void swapScreen() {
        nextKill = false;
        cur_screen++;
        int length = preference.getBoolean("sw_all_swap", false) ? Constants.SWAP_APPS.length : 2;
        if (cur_screen >= length) {
            cur_screen = 0;
        }
        Toast.makeText(getBaseContext(), "Swap Screen • " + cur_screen, Toast.LENGTH_SHORT).show();
        System.out.println("trace • MAIN | Swap Screen | next • " + cur_screen);
        new Handler().post(new UpdateUIRunnable(Constants.SWAP_APPS[cur_screen]));
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
        if (memSize < 150) {
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
