package com.posiframe.posiframesdk;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.posiframe.skepsfinancing.SkepsInit;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences.Editor SKEPS_financing = getSharedPreferences("dataBinding", MODE_PRIVATE).edit();
        SKEPS_financing.putString("domain", "https://fnbo-dev.skeps.dev");
        SKEPS_financing.putString("merchantID", "LUXMFDFL"); //scheels
//        SKEPS_financing.putString("merchantID", "YT3EE9IN"); //JFJ Dev
        SKEPS_financing.apply();

        View view = this.findViewById(android.R.id.content).getRootView();
        EditText mEdit;

//        mEdit   = (EditText) binding.plainTextInput;

        JSONObject config = new JSONObject();
        try {
            config.put("flowType","checkout");

//            if (mEdit.getText().toString().length() > 1) {
//                config.put("amount", (mEdit.getText().toString()));
//            } else {
//
//            }
            config.put("amount", "230.87");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SkepsInit.SkepsCheckoutHandlerInterface handlers = new SkepsInit.SkepsCheckoutHandlerInterface() {
            @Override
            public void successHandler(String result) {
                TextView t = findViewById(R.id.skeps_callback);
                t.setText("Status: " + result);
            }
            @Override
            public void failureHandler(String result) {
                TextView t = findViewById(R.id.skeps_callback);
                t.setText("Status: " + result);
            }
        };

        SkepsInit SKEPSInitFlow = new SkepsInit(view.getContext());
//        String baseURL = "https://fnbo-dev.skeps.dev";
//        SKEPSInitFlow.baseURL = baseURL;
//        SKEPSInitFlow.fetchBanner(baseURL);
        SKEPSInitFlow.initProcess(config, handlers);
    }
}