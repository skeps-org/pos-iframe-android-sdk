package com.posiframe.skepsfinancing;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SkepsFinancing extends AppCompatActivity {

    Intent i;
    WebView webView;
    String domain, merchantID, url, hashURL;
    String currency = "USD";
    ResultReceiver receiver;
    String loadCount = "loading";
    Bundle bundle = new Bundle();
    float amount;
    boolean downloadClicked = false;
    public final static String BUNDLED_LISTENER = "listener";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.skepsfinancing);
        super.onCreate(savedInstanceState);

        webView = findViewById(R.id.webView);
        SharedPreferences config = getSharedPreferences("dataBinding", MODE_PRIVATE);
        domain = config.getString("domain", "");
        merchantID = config.getString("merchantID", "");

        Intent intent = getIntent();
        hashURL = intent.getStringExtra("hashURL");
        amount = Float.parseFloat(intent.getStringExtra("amount"));

        receiver = intent.getParcelableExtra(SkepsFinancing.BUNDLED_LISTENER);

        if (hashURL != null && hashURL.contains("checkout?hash")) {
            setInitiateURL(hashURL);
            Uri params = Uri.parse(hashURL);
            amount = Integer.parseInt(params.getQueryParameter("order_amount"));
            loadIframe();
        } else {
            if(intent.getStringExtra("flowType").contains("checkout")) {
                try {
                    BNPLCheckoutFlow(amount);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else {
                try {
                    BNPLCheckEligibilityFlow(amount);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setInitiateURL(String initiateURL) {
        url = initiateURL;
    }

    public String getInitiateURL() {
        return url;
    }

    public void BNPLCheckoutFlow(float amount) throws JSONException {
        RequestQueue requestQueue =  Volley.newRequestQueue(this.getApplicationContext());

        String checkoutAPI = domain + "/application/api/pos/v1/oauth/merchant/generate/checkout/hash?merchantId=" + merchantID;

        JSONObject headers = new JSONObject();
        try {
            headers.put("Content-Type", "application/json");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET,checkoutAPI,headers,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String checkoutHash;
                        try {
                            Object obj = response.getJSONObject("merchantInfo");
                            checkoutHash = ((JSONObject) obj).getString("checkoutLandingUrlPath");
                            long millis = new Date().getTime();
                            String initiateAPI = domain + checkoutHash + "&_="+millis+"&order_amount=" + amount;
                            setInitiateURL(initiateAPI);
                            loadIframe();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("error ############"+error);
                    Toast.makeText(getApplicationContext(), "Something went wrong!", Toast.LENGTH_SHORT)
                            .show();
                // handle error
            }
        });
        requestQueue.add(jsObjRequest);
    }

    public void BNPLCheckEligibilityFlow(float amount) throws JSONException {

        RequestQueue requestQueue =  Volley.newRequestQueue(this.getApplicationContext());

        String checkEligibilityAPI = domain + "/application/api/pos/v1/financing/getEligibleOffersForCIN/";

        JSONObject body = new JSONObject();
        try {
            body.put("currency", currency);
            body.put("amount", amount);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, checkEligibilityAPI,body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String checkEligibilityHash = null;
                        try {
                            checkEligibilityHash = response.getString("landing_url_path");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        long millis = new Date().getTime();
                        String initiateAPI = domain + checkEligibilityHash + "&_="+ millis;
                        setInitiateURL(initiateAPI);
                        loadIframe();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle errors
                        Toast.makeText(getApplicationContext(), "Something went wrong!", Toast.LENGTH_SHORT)
                                .show();
                        String err = null;
                        err = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                        System.out.println(err);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("merchant_id", merchantID);
                return headers;
            }
        };
        requestQueue.add(request);
    }

    public void loadIframe() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(getInitiateURL()); // loading the hash url
        webView.addJavascriptInterface(new SkepsFinancing.JavaScriptInterface(), "JavaScriptInterface"); // binding callback method
        // on load hash url sending the postMessage event to iframe
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (loadCount.equals("loading")) {
                    loadCount = "loaded";
                    super.onPageFinished(view, url);

                    Uri uri = Uri.parse(url);
                    String config;
                    if(url.contains("checkout")) {
                        config = "window.postMessage({event:'initialize',mode: 'inline-full-page', sourceType :'SKEPS_INTEGRATED_MERCHANT',config: {cartAmount: "+amount+",}}, '*')";
                    } else {
                        String accessToken = uri.getQueryParameter("access_token");
                        String orderID = uri.getQueryParameter("order_id");
                        String merchantId = uri.getQueryParameter("merchant_id");
                        config = "window.postMessage({event:'check-eligibility',mode: 'inline-full-page', sourceType :'SKEPS_INTEGRATED_MERCHANT',config: {merchantId:'"+merchantId+"',opportunityId:'"+orderID+"',accessKey:'"+accessToken+"',orderAmount:"+amount+",}}, '*')";
                    }
                    // sending post message
                    webView.evaluateJavascript(config, null);
                }}
        });

        // webView download listeners
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                if (downloadClicked) {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimetype);
                    request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                }
            }
        });
    }

    private class JavaScriptInterface {

        @JavascriptInterface
        public void onSuccess(String data) {
            Toast.makeText(getApplicationContext(), "Order Completed!", Toast.LENGTH_LONG)
                    .show();
            bundle.putString("data", data);
            finish();
            receiver.send(Activity.RESULT_OK, bundle);
        }

        @JavascriptInterface
        public void onFailure(String data) throws JSONException {
            bundle.putString("data", data);
            finish();
            receiver.send(Activity.RESULT_CANCELED, bundle);
        }

        @JavascriptInterface
        public void loaded(String data) {
//            System.out.println(" ################ loaded ################ "+ data);
        }

        @JavascriptInterface
        public void info(String data) {
            if (data.contains("checkEligibilitySuccess")) {
                finish();
                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT)
                        .show();
            }
        }

        @JavascriptInterface
        public void plaidONSuccess(String data) {
            Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT)
                    .show();
        }

        @JavascriptInterface
        public void plaidONExit(String data) {
            Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT)
                    .show();
        }

        @JavascriptInterface
        public void downloadClick(String data) {
            downloadClicked = true;
        }
    }
}