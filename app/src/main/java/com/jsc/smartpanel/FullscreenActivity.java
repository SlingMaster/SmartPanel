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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jsc.utils.GlobalUtils;
import com.jsc.utils.SysUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;

public class FullscreenActivity extends AppCompatActivity {

    CommunicationServer communicationServer;
    public static SharedPreferences preference;
    private long back_pressed;
    private int cur_screen = 0;
    public static int app_state = Constants.MAIN;

    public static int test_cycles = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preference = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = getIntent();

        if (intent != null) {
            // "next app" --------------------------
            String nextApp = intent.getStringExtra("next_app");
            if (nextApp != null) {
                int slash = nextApp.lastIndexOf('/');
                if (slash >= 0) {
                    nextApp = nextApp.substring(slash);
                }
            } else {
                // first start -------------------------
                nextApp = "timer.html";
                Handler handler = new Handler();
                handler.postDelayed(() -> externalCMD(Constants.CMD_LOAD_TIMER), 2000);
            }
            // -----------------------------------------
            SysUtils.LogToScr(this, preference, "Run App:" + nextApp);
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
        findViewById(R.id.btn_radio).setOnClickListener(view -> runExternalApplication(1));
        findViewById(R.id.btn_smart).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_SMART));
        findViewById(R.id.btn_stats).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_STATS));
        findViewById(R.id.btn_timer).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_TIMER));
        findViewById(R.id.btn_weather).setOnClickListener(view -> externalCMD(Constants.CMD_LOAD_WEATHER));
        findViewById(R.id.btn_sling).setOnClickListener(view -> runExternalApplication(2));
        findViewById(R.id.btn_wifi).setOnClickListener(view -> runExternalApplication(3));
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
        return id;
    }

    // ====================================
    private void openWebView(int app_id) {
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra("app_id", app_id);
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
                            //Toast.makeText(this, "Sync Data", Toast.LENGTH_SHORT).show();
                            break;
                        case Constants.WOKE_UP:
                            runApplication(Constants.CMD_LOAD_WEATHER);
                            break;
                        case Constants.SHOW_TIME:
                            runApplication(Constants.CMD_LOAD_TIMER);
                            break;
                        case Constants.LOAD_SCREEN:
                            runApplication(cmd);
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
        }
    }

    // ===================================
    @Override
    protected void onResume() {
        super.onResume();
        Objects.requireNonNull(getSupportActionBar()).hide();
        // GlobalUtils.hideSystemUI(webContainer);
        if (!GlobalUtils.isConnectedToInternet(getApplicationContext())) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.msg_not_wifi_connection), Toast.LENGTH_LONG).show();
        }
        watchDog();
        app_state = Constants.MAIN;
        showDebugIcon(preference);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
            // Transmitted External Command  from Desktop Client
            if (jsonStr != null) {
                AnalyzerRemoteCMD(jsonStr);
            } else {
                externalCMD(command);
            }
        }
    }

    // ===================================
    private void AnalyzerRemoteCMD(String jsonStr) {

        JSONObject json;
        int cmd = 0;
        boolean state = false;
        boolean is_state = false;
        try {
            json = new JSONObject(jsonStr);

            if (json.has("cmd")) {
                cmd = json.optInt("cmd", 0);
            }
            is_state = json.has("state");
            if (is_state) {
                state = json.getBoolean("state");
            }
            // SysUtils.LogToScr(this, preference, "AnalyzerRemote jsonStr : " + jsonStr);
            SysUtils.LogToScr(this, preference, "AnalyzerRemoteCMD • " + jsonStr + " | CMD • " + cmd);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        switch (cmd) {
            case Constants.CMD_RESTART:
                SysUtils.restartApp(this);
                break;
            case Constants.CMD_EXIT:
                finish();
                break;
            case Constants.CMD_HOME:
                if (app_state == Constants.INTERNAL) {
                    sendCmdToWebView("{cmd:" + Constants.CMD_BACK + "}");
                }
                if (app_state == Constants.EXTERNAL) {
                    SysUtils.restartApp(this);
                }
                break;
            case Constants.CMD_BACK:
                if (app_state == Constants.INTERNAL) {
                    sendCmdToWebView(jsonStr);
                } else {
                    if (app_state == Constants.MAIN) {
                        onBackPressed();
                    } else {
                        SysUtils.restartApp(this);
                    }
                }
                break;
            case Constants.CMD_DEBUG_MODE:
                if (is_state) {
                    setSwitch(preference, "sw_log_screen", state);
                } else {
                    setSwitch(preference, "sw_log_screen");
                }
                showDebugIcon(preference);
                break;
            case Constants.CMD_AUTO_SWAP:
                if (is_state) {
                    setSwitch(preference, "sw_swap", state);
                } else {
                    setSwitch(preference, "sw_swap");
                }
                break;
            // ===========================
            // external app --------------
            case Constants.CMD_RADIO:
                runExternalApplication(1);
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
                if (app_state == Constants.INTERNAL) {
                    sendCmdToWebView(jsonStr);
                } else {
                    // load html application
                    openWebView(getHtmlID(cmd));
                }
                break;
        }

    }

    // ===================================
    private void setSwitch(SharedPreferences preference, String key) {
        boolean flag = preference.getBoolean(key, false);
        SharedPreferences.Editor editor = preference.edit();
        editor.putBoolean(key, !flag);
        editor.apply();
        SysUtils.LogToScr(this, preference, "Switch " + key + " • " + (!flag ? "ON" : "OFF"));
    }

    // ===================================
    private void setSwitch(SharedPreferences preference, String key, boolean flag) {
        SharedPreferences.Editor editor = preference.edit();
        editor.putBoolean(key, flag);
        editor.apply();
        SysUtils.LogToScr(this, preference, "Switch " + key + " • " + (!flag ? "ON" : "OFF"));
    }

    // ===================================
    private void showDebugIcon(SharedPreferences preference) {
        View ico = findViewById(R.id.debug_ico);
        if (preference.getBoolean("sw_log_screen", false)) {
            ico.setVisibility(View.VISIBLE);
        } else {
            ico.setVisibility(View.GONE);
        }
    }

    // ===================================
    private void externalCMD(int cmd) {
        // TODO debug must remove
        SysUtils.LogToScr(this, preference, "Transmitted External Command • $" + String.format("%X", cmd) + " | DEC • " + cmd);
        switch (cmd) {
            case Constants.CMD_BACK:
                onBackPressed();
                break;

            // ===========================
            // Menu ======================
            // ===========================
            // html app ------------------
            case Constants.CMD_LOAD_SMART:
            case Constants.CMD_LOAD_STATS:
            case Constants.CMD_LOAD_TIMER:
            case Constants.CMD_LOAD_WEATHER:
                if (app_state == Constants.EXTERNAL) {
                    SysUtils.restartApp(this);
                    return;
                }
                if (app_state == Constants.INTERNAL) {
                    sendCmdToWebView("{cmd:" + Constants.CMD_BACK + "}");
                } else {
                    runApplication(cmd);
                }
                // set BackLight after load html app
                setBackLight();
                break;
            // ===========================
            default:
                SysUtils.LogToScr(this, preference, getResources().getString(R.string.msg_unsupported));
                break;
        }
    }

    // ===================================
    private void setBackLight() {
        boolean night = SysUtils.isNight(preference);
        Handler handler = new Handler();
        handler.postDelayed(() -> sendCmdToWebView(
                "{cmd:" + Constants.CMD_BACK_LIGHT + ",json:{state:" + night + "}}"
        ), 5000);
    }

    // ===================================
    private void runApplication(int cmd) {
        if (app_state == Constants.EXTERNAL) {
            SysUtils.restartApp(this, Constants.HTML_APPS[getHtmlID(cmd)]);
        } else {
            // load next html application
            openWebView(getHtmlID(cmd));
        }
    }

    // ===================================
    private void runExternalApplication(int app_idx) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(Constants.PACKAGES[app_idx]);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
            app_state = Constants.EXTERNAL;
            startActivity(intent);
        } else {
            app_state = Constants.MAIN;
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
        cur_screen++;
        int length = preference.getBoolean("sw_all_swap", false) ? Constants.SWAP_APPS.length : 2;
        if (cur_screen >= length || SysUtils.isNight(preference)) {
            cur_screen = 0;
        }
        SysUtils.LogToScr(this, preference, "Swap Screen • " + cur_screen);
        new Handler().post(new UpdateUIRunnable(Constants.SWAP_APPS[cur_screen]));
    }

    // ===================================
    private void watchDog() {
        // restart app if low memory ---------------
        long memSize = SysUtils.getFreeMemory(this);
        if (memSize < 150) {
            SysUtils.restartApp(this);
        } else {
            SysUtils.killAllProcess(this);
        }

        // ********************************
        // this code must remove after debug
        // ********************************
        // memory leak --------------------
        test_cycles++;
        String msgMemory = "FREE RAM : " + memSize + " Mb | CYCLES : " + test_cycles;
        TextView textInfo = findViewById(R.id.memInfo);
        textInfo.setText(preference.getBoolean("sw_log_screen", false) ? msgMemory : "");
        // ********************************
    }
}
