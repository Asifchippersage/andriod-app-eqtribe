package eqtribe.chippersage;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private VideoEnabledWebView webView;
    private VideoEnabledWebChromeClient webChromeClient;
    private ArrayList<JSONObject> list;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        //get device imei number......
        String deviceId = getDeviceId();
        list = readJsonFile();
        // Save the web view
        webView = (VideoEnabledWebView)findViewById(R.id.webView);
        if(isValidDevice(deviceId)){
            View nonVideoLayout = findViewById(R.id.nonVideoLayout);
            ViewGroup videoLayout = (ViewGroup)findViewById(R.id.videoLayout);
            View loadingView = getLayoutInflater().inflate(R.layout.view_loading_video, null);
            webChromeClient = new VideoEnabledWebChromeClient(nonVideoLayout, videoLayout, loadingView, webView)
            {
                // Subscribe to standard events, such as onProgressChanged()...
                @Override
                public void onProgressChanged(WebView view, int progress)
                {
                    // Your code...
                }
            };
            webChromeClient.setOnToggledFullscreen(new VideoEnabledWebChromeClient.ToggledFullscreenCallback()
            {
                @Override
                public void toggledFullscreen(boolean fullscreen){
                    if (fullscreen)
                    {
                        WindowManager.LayoutParams attrs = getWindow().getAttributes();
                        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                        attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                        getWindow().setAttributes(attrs);
                        if (android.os.Build.VERSION.SDK_INT >= 14)
                        {
                            //noinspection all
                            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                        }
                    }
                    else
                    {
                        WindowManager.LayoutParams attrs = getWindow().getAttributes();
                        attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                        attrs.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                        getWindow().setAttributes(attrs);
                        if (android.os.Build.VERSION.SDK_INT >= 14)
                        {
                            //noinspection all
                            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                        }
                    }
                }
            });
            webView.setWebChromeClient(webChromeClient);
            // Call private class InsideWebViewClient
            webView.setWebViewClient(new InsideWebViewClient());

            // Navigate anywhere you want, but consider that this classes have only been tested on YouTube's mobile site
            //webView.loadUrl("http://m.youtube.com");
            webView.loadUrl("file:///android_asset/Chippersage/Skills/start.html");
        }else{
            Toast.makeText(getApplicationContext(),"Device is not valid.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private class InsideWebViewClient extends WebViewClient {
        ProgressDialog progressDialog;
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        //Show loader on url load
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            // Then show progress  Dialog
            // in standard case YourActivity.this
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Loading...");
                progressDialog.show();
            }
        }

        // Called when all page resources loaded
        public void onPageFinished(WebView view, String url) {
            try {
                // Close progressDialog
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()){
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private String getDeviceId(){
        String deviceId = null;
        TelephonyManager telephonyManager = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if(telephonyManager != null){
            deviceId = telephonyManager.getDeviceId();
        }
        if(deviceId == null || deviceId .length() == 0) {
            deviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return deviceId;
    }

    private ArrayList<JSONObject> readJsonFile(){
        //String json;
        ArrayList<JSONObject> list = new ArrayList<>();
        try {
            InputStream is = getAssets().open("validDevices.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            //json = new String(buffer, "UTF-8");
            JSONObject jsonObj = new JSONObject(new String(buffer, "UTF-8"));
            JSONArray jsonArray = jsonObj.getJSONArray("devices");
            for(int i = 0;  i < jsonArray.length(); i++){
                JSONObject obj = jsonArray.getJSONObject(i);
                list.add(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private boolean isValidDevice(String deviceId){
        boolean isValid = false;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date currentDate = new Date();
        try {
            for(int i = 0; i < list.size(); i++){
                String imeiNumber = list.get(i).getString("imei");
                Date expiryDate = simpleDateFormat.parse(list.get(i).getString("expiryDate"));
                if(imeiNumber.equals(deviceId) && list.get(i).getString("expiryDate").equals("")){
                    isValid = true;
                }else if(expiryDate.after(currentDate) || expiryDate.compareTo(currentDate) == 0){
                    isValid = true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return isValid;
    }
}
