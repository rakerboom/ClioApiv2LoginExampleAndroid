package com.akerboom.clioapiv2loginexampleandroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * http://api-docs.clio.com/v2/#authorization-with-oauth-2-0
 */
public class LoginActivity extends AppCompatActivity implements Callback {

    public static String CLIENT_ID = "";//App Key example: fzaXZvrLWZX747wQQRNuASeVCBxaXpJaPMDi7F96
    public static String CLIENT_SECRET = "";//App Secret example: xVp5wAX05g1oDjV5astg2KZIZ85NX31FKTPV876v
    //Visit the developer portal at https://app.clio.com/settings/developer_applications
    //Click the Add button to create a new application. Enter details about your application
    //These details will be shown to Clio users when they’re asked to authorize your application
    //Add the key and secret to authorize your application with Clio

    public static String HOST = "app.clio.com";
    public static String PATH_AUTHORIZE = "/oauth/authorize";
    public static String PATH_APPROVAL = "/oauth/approval";
    public static String PATH_TOKEN = "/oauth/token";
    public static String RESPONSE_TYPE = "code";
    public static String GRANT_TYPE = "authorization_code";
    public static String REDIRECT_URI = "https://" + HOST + "/oauth/approval";

    private OkHttpClient client = new OkHttpClient();

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if ("".equals(CLIENT_ID) || "".equals(CLIENT_SECRET)) {
            TextView textView = (TextView) findViewById(R.id.text_view);
            textView.setVisibility(View.VISIBLE);
            textView.setText(getString(R.string.missing_key_secret));
        } else {
            WebView myWebView = (WebView) findViewById(R.id.web_view);
            WebSettings webSettings = myWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            myWebView.setWebViewClient(new MyWebViewClient());
            myWebView.loadUrl(buildAuthorizeUrl().toString());
        }
    }

    private static Uri buildAuthorizeUrl() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http").authority(HOST).path(PATH_AUTHORIZE);
        builder.appendQueryParameter("response_type", RESPONSE_TYPE);
        builder.appendQueryParameter("client_id", CLIENT_ID);
        builder.appendQueryParameter("redirect_uri", REDIRECT_URI);
        return builder.build();
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Uri approval = Uri.parse(url);
            if (approval.getHost().equals(HOST) && approval.getPath().equals(PATH_APPROVAL)) {
                String successCode = approval.getQueryParameter("code");
                Log.d("LoginActivity", "Success Code: " + successCode);
                client.newCall(
                        new Request.Builder().url(buildTokenUrl()).post(buildBody(successCode)).build()
                ).enqueue(LoginActivity.this);
            }
        }
    }

    private String buildTokenUrl() {
        return new Uri.Builder().scheme("https").authority(HOST).path(PATH_TOKEN).build().toString();
    }

    private FormBody buildBody(final String successCode) {
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("client_id", CLIENT_ID);
        builder.add("client_secret", CLIENT_SECRET);
        builder.add("grant_type", GRANT_TYPE);
        builder.add("code", successCode);
        builder.add("redirect_uri", REDIRECT_URI);
        return builder.build();
    }

    @Override
    public void onFailure(Call call, IOException e) {
        Log.d("LoginActivity", "onFailure");
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        String accessToken = getAccessToken(response.body().byteStream());
        Log.d("LoginActivity", "access_token: " + accessToken);
        saveAccessToken(accessToken);
        updateUi();
    }

    private void updateUi() {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.web_view).setVisibility(View.GONE);
                TextView textView = (TextView) findViewById(R.id.text_view);
                textView.setVisibility(View.VISIBLE);
                String text = "This is your access token it represents both the user's identity" +
                        " and the application's authorization to act on the user's behalf:";
                textView.setText(text + "\n\n" +
                        getSharedPreferences("access_token", Context.MODE_PRIVATE)
                                .getString("access_token", ""));
            }
        });
    }

    private String getAccessToken(InputStream json) throws IOException {
        String token = null;
        JsonReader reader = new JsonReader(new InputStreamReader(json, "UTF-8"));
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("access_token")) {
                token = reader.nextString();
                break;
            }
        }
        reader.close();
        return token;
    }

    private void saveAccessToken(String accessToken) {
        SharedPreferences.Editor editor = getSharedPreferences("access_token", Context.MODE_PRIVATE).edit();
        editor.putString("access_token", accessToken);
        editor.apply();
    }
}
