package jsinterface;

import android.util.Log;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class JSOut {
    private static final String TAG = JSOut.class.getSimpleName();
    private WeakReference<WebView> _webView;

    // ----------------------------------------
    public JSOut(WebView appView) {
        _webView = new WeakReference<WebView>(appView);
    }

    //    private final WebView getWebView() {
//        return _webView.get();
//    }
    private WebView getWebView() {
        return _webView.get();
    }

    // Send String ----------------------------
    public void callJavaScript(int target, final String data) {
        final String s = "javascript:JavaScriptCallback(" + target + ", " + data + " );";
        final WebView wv = getWebView();
        wv.post(new Runnable() {
            @Override
            public void run() {
                wv.loadUrl(s);
            }
        });
        Log.d(TAG, "JavaScriptCallback String | " + s);
    }

    // Send JSON Object -----------------------
    public void callJavaScript(int target, final JSONObject json) {
        JSONObject json_data = json;
        if (json_data == null)
            json_data = new JSONObject();

        final String s = "javascript:JavaScriptCallback(" + target + ", " + json_data.toString() + " );";
        final WebView wv = getWebView();
        wv.post(new Runnable() {
            @Override
            public void run() {
                wv.loadUrl(s);
            }
        });
        // Log.d(TAG, " [trace] >>>>>>>>>> JavaScriptCallback JSONObject | " + s);
    }

    // Send JSON Array ------------------------
    public void callJavaScript(int target, final JSONArray json) {
        String jsonString = "{}";
        if (json != null)
            jsonString = json.toString();
        final String s = "javascript:JavaScriptCallback(" + target + ", " + jsonString + " );";
        final WebView wv = getWebView();
        wv.post(new Runnable() {
            @Override
            public void run() {
                wv.loadUrl(s);
            }
        });
        // Log.d(TAG, "JavaScriptCallback JSONArray | " + s);
    }

}