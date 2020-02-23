/*
 * Copyright (c) 2019.
 * Jeneral Samopal Company
 * Design and Programming by Alex Dovby
 */

package com.jsc.smartpanel;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import com.jsc.utils.GlobalUtils;

/**
 * A {@link android.preference.PreferenceActivity} which implements and proxies the necessary calls
 * to be used with AppCompat.
 */
public abstract class AppCompatPreferenceActivity extends PreferenceActivity {
    public static String curVersion;
    public static String ipStr;
    private AppCompatDelegate mDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        curVersion = getVersionApp();
        ipStr = getIP();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

//    public void setSupportActionBar(@Nullable Toolbar toolbar) {
//        getDelegate().setSupportActionBar(toolbar);
//    }

    @Override
    @NonNull
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetPreferencesDefault();
        getDelegate().onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // return main activity ---------
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
//        switch (item.getItemId()) {
//            case android.R.id.home:
//               onBackPressed();
//                return true;
//            default:
//                break;
//        }
        return super.onOptionsItemSelected(item);
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    // ----------------------------------------
    private void resetPreferencesDefault() {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        boolean reset_default = preference.getBoolean("sw_reset_default", false);
        if (reset_default) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.pref_title_reset), Toast.LENGTH_LONG).show();
            // reset default settings
            preference.edit().clear().apply();
            // System.out.println("resetPreferencesDefault");
            // reset last project options ---------
            SharedPreferences.Editor editor = preference.edit();
            editor.putBoolean("develop_mode", false);
            // settings general -------------------
            editor.putString("ip_weather", getResources().getString(R.string.def_node_weather_url));
            editor.putString("chip_weather", getResources().getString(R.string.def_node_weather_chip));
            editor.putString("ip_bathroom", getResources().getString(R.string.def_node_bathroom_url));
            editor.putString("chip_bathroom", getResources().getString(R.string.def_node_bathroom_chip));
            // settings advanced -------------------
            editor.putBoolean("sw_debug_mode", false);
            editor.putBoolean("sw_clear_cache", false);
            // settings schedule -------------------
            editor.putBoolean("sw_auto_start", true);
            editor.putBoolean("sw_back_light", true);
            editor.putString("start_day", getResources().getString(R.string.pref_schedule_def_start_day));
            editor.putString("start_night", getResources().getString(R.string.pref_schedule_def_start_night));
            editor.putBoolean("sw_swap", false);
            editor.putString("swap_frequency", getResources().getString(R.string.pref_schedule_def_frequency));
            editor.apply();
        }
    }


    // ----------------------------------------
    private String getIP() {
        String ipAddress;
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        ipAddress = GlobalUtils.getIP() + " : " + Constants.SERVER_PORT;
        return ipAddress;
    }

    // ----------------------------------------
    private String getVersionApp() {
        String versionName = "***";
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }
}
