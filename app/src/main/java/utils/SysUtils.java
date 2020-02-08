/*
 * Copyright (c) 2020.
 * Jeneral Samopal Company
 * Design and Programming by Alex Dovby
 */

package utils;

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

import java.util.List;

public class SysUtils {
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
    public static boolean killAllProcess(Activity mainActivity) {
        List<ApplicationInfo> packages;
        Context context = mainActivity.getApplicationContext();
        PackageManager pm;
        pm = context.getPackageManager();

        //get a list of installed apps ---------------
        packages = pm.getInstalledApplications(0);

        ActivityManager mActivityManager = (ActivityManager) mainActivity.getSystemService(Context.ACTIVITY_SERVICE);
        String myPackage = context.getApplicationContext().getPackageName();

        for (ApplicationInfo packageInfo : packages) {
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                continue;
            }
            if (packageInfo.packageName.equals(myPackage)) {
                continue;
            }
            mActivityManager.killBackgroundProcesses(packageInfo.packageName);
        }
        Toast.makeText(mainActivity, "Killed All Background Process", Toast.LENGTH_SHORT).show();
        return true;
    }

    // ===================================
    public static void restartApp(Activity mainActivity) {
        // reboot main application -------
        Context context = mainActivity.getApplicationContext();
        Intent mStartActivity = new Intent(context, FullscreenActivity.class);
        int mPendingIntentId = PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
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
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
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
}
