/*
 * Copyright (c) 2020.
 * Jeneral Samopal Company
 * Design and Programming by Alex Dovby
 */

package com.jsc.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.WindowManager;
import android.widget.Toast;

import com.jsc.smartpanel.FullscreenActivity;

import java.util.Calendar;
import java.util.List;

public class SysUtils {

    // ===================================
    public static long getFreeMemory(Activity mainActivity) {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) mainActivity.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.availMem / 1048576;
        } else {
            return 0;
        }
    }

    // ===================================
    public static void setBackLight(Activity mainActivity, Boolean sleep_mode) {
        WindowManager.LayoutParams layout = mainActivity.getWindow().getAttributes();
        if (sleep_mode) {
            layout.screenBrightness = 0.2F;
        } else {
            layout.screenBrightness = 0.7F;
        }
        System.out.println("[ trace  ] setBackLight " + layout.screenBrightness);
        mainActivity.getWindow().setAttributes(layout);
    }

    // ===================================
    public static void killAllProcess(Activity mainActivity) {
        List<ApplicationInfo> packages;
        Context context = mainActivity.getApplicationContext();
        PackageManager pm;
        pm = context.getPackageManager();

        //get a list of installed apps ---------------
        packages = pm.getInstalledApplications(0);

        ActivityManager activityManager = (ActivityManager) mainActivity.getSystemService(Context.ACTIVITY_SERVICE);
        String myPackage = context.getApplicationContext().getPackageName();

        for (ApplicationInfo packageInfo : packages) {
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                continue;
            }
            if (packageInfo.packageName.equals(myPackage)) {
                continue;
            }
            if (activityManager != null) {
                activityManager.killBackgroundProcesses(packageInfo.packageName);
            }
        }
        Toast.makeText(mainActivity, "Killed All Background Process", Toast.LENGTH_SHORT).show();
    }

    // ===================================
    public static void restartApp(Activity mainActivity) {
        // reboot main application -------
        Context context = mainActivity.getApplicationContext();
        Intent mStartActivity = new Intent(context, FullscreenActivity.class);
        int mPendingIntentId = PendingIntent.FLAG_UPDATE_CURRENT;
//        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            System.exit(0);
        }
    }

    // ===================================
    public static void restartApp(Activity mainActivity, String nextApp, boolean nextKill) {
        // reboot main application & load next app
        Context context = mainActivity.getApplicationContext();
        Intent mStartActivity = new Intent(context, FullscreenActivity.class);
        mStartActivity.putExtra("next_app", nextApp);
        mStartActivity.putExtra("next_kill", nextKill);
        int mPendingIntentId = PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            System.exit(0);
        }
    }

    // ===================================
    public static boolean isNight(String startDay, String startNight) {
//        // get the supported ids for GMT-08:00 (Pacific Standard Time)
//        String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
//        // if no ids were returned, something is wrong. get out.
//        if (ids.length == 0)
//            System.exit(0);
//
//        // begin output
//        System.out.println("Current Time");
//
//        // create a Pacific Standard Time time zone
//        SimpleTimeZone pdt = new SimpleTimeZone(+3 * 60 * 60 * 1000, ids[0]);
//
//        // set up rules for daylight savings time
//        pdt.setStartRule(Calendar.MARCH, 2, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
//        pdt.setEndRule(Calendar.NOVEMBER, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
//
//        // create a GregorianCalendar with the Pacific Daylight time zone
//        // and the current date and time
//        Calendar calendar = new GregorianCalendar(pdt);
//        Date curTime = new Date();
//        calendar.setTime(curTime);
//        calendar.set(Calendar.HOUR_OF_DAY, 24);

        int pm = Calendar.getInstance().get(Calendar.AM_PM);
        int hh = Calendar.getInstance().get(Calendar.HOUR) + pm * 12;
        int start_day = Integer.parseInt(startDay);
        int start_night = Integer.parseInt(startNight);
        return (hh >= start_night || hh < start_day);
    }

    // ===================================
//    private void backPrevApp(String curApp) {
//        Intent intent = getPackageManager().getLaunchIntentForPackage(curApp);
//        if (intent != null) {
//            // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) | Intent.FLAG_ACTIVITY_NEW_TASK;
//            intent = new Intent(Intent.ACTION_MAIN);
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//            startActivity(intent);
//        }
//    }

    // ===================================
//    public static Boolean detectApp(Context c, String packageName) {
//        // if (Build.VERSION.SDK_INT < 5) return false;
//        PackageManager pm = c.getPackageManager();
//        try {
//            if (pm.getPackageInfo(packageName, 0) != null)
//                return true;
//        } catch (PackageManager.NameNotFoundException e) {
//            System.out.println(" trace | App " + packageName + " not instaled");
//        }
//        return false;
//    }

    // ===================================
//    private void backMain() {
//        Intent intent = getPackageManager().getLaunchIntentForPackage(Constants.PACKAGES[0]);
//        if (intent != null) {
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//            // intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//            // Bundle is optional --------------
////            Bundle bundle = new Bundle();
////            bundle.putInt("lastCMD", lastCMD);
////            intent.putExtras(bundle);
//            //  end Bundle ---------------------
//            startActivity(intent);
//            View splash = findViewById(R.id.splash);
//            if (splash.getVisibility() == View.GONE) {
//                splash.setVisibility(View.VISIBLE);
//            }
//        }
//    }
}
