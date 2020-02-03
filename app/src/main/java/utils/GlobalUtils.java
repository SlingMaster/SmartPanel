/*
 * Copyright (c) 2019.
 * Jeneral Samopal Company
 * Programming by Alex Uchitel
 * Design and Programming by Alex Dovby
 */

package utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class GlobalUtils {
    // private static final String hostExtractorRegexString = "(?:https?://)?(?:www\\.)?(.+\\.)(com|au\\.uk|co\\.in|be|in|uk|org\\.in|org|net|edu|gov|mil|ua)";
    //    private static final Pattern hostExtractorRegexPattern = Pattern.compile(hostExtractorRegexString);
    private static Boolean isTabletModeDetermined = false;
    private static Boolean isTabletMode = false;

    // ========================================
    public static String getIP() {
        // Device ip address ----------
        String ipStr = "N/A";
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {

                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress ip = (InetAddress) ee.nextElement();
//                    System.out.println(ip.getHostAddress());
                    ipStr = ip.getHostAddress();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ipStr;
    }

    // ========================================
    public static boolean isTablet(Context paramContext) {
        if (!isTabletModeDetermined) {
            if (paramContext.getResources().getConfiguration().smallestScreenWidthDp >= 600)
                isTabletMode = true;
            isTabletModeDetermined = true;
        }
        return isTabletMode;
    }

    // ========================================
//    public static void setDefaultOrientation(Activity activity) {
//        if (isTablet(activity.getApplicationContext())) {
//            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//        } else {
//            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        }
//    }


    // ========================================
//    public static Spanned getBoldString(String text) {
//        return Html.fromHtml(text);
//    }

    // ========================================
//    public static String getDomainName(String url) {
//        if (url == null) return null;
//        url = url.trim();
//        Matcher m = hostExtractorRegexPattern.matcher(url);
//        if (m.find() && m.groupCount() == 2) {
//            return m.group(1) + m.group(2);
//        } else {
//            return null;
//        }
//    }


    // ====================================================
//    public static int getCommandID(final String data) {
//        if (data != null) {
//            return Integer.parseInt(data.substring(4, 6), 16);
//        }
//        return 0;
//    }

    // ====================================================
//    public static Boolean isEven(int val) {
//        return (val % 2 == 0);
//    }

    // ====================================================
//    static String GetToday() {
//        Date presentTime_Date = Calendar.getInstance().getTime();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy | HH:mm:ss", Locale.US);
//        // dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
//        return dateFormat.format(presentTime_Date);
//    }

    // ===================================================
    public static boolean isConnectingToInternet(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] networks = connectivity.getAllNetworkInfo();

            if (networks != null) {
                for (NetworkInfo networkInfo : networks) {
                    if (networkInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static String getString(@NonNull Context context, @StringRes int resource) {
        return context.getResources().getString(resource);
    }

    public static void hideSystemUI(@NonNull View view) {
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
}
