package com.posiframe.posiframesdk;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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
        SKEPS_financing.putString("domain", "https://pos.test.skeps.com");
//        SKEPS_financing.putString("merchantID", "YKVABNVB"); //scheels
        SKEPS_financing.putString("merchantID", "YT3EE9IN"); //JFJ
        String  hashURL = "https://pos.test.skeps.com/application/initiate/checkout?hash=ue7uS3DA9EDiqKWwMTXHe%2B8aqiXGYSR%2Fv%2FwuEzyc5HKEijQZIy%2FhOKPhvPuT1LF5%2FeJDVDaGfFyKCfKTYO6VlOAyUOvyaIPaGQCwLT3ZXojZegOOdakbnQW0O%2FWMcWndwph4zOb4ArP5clq%2FzUKIooTDQY08zSkeelM8sI2qRuQ%3D&_=1680159593671&order_amount=235";

        SKEPS_financing.apply();

        View view = this.findViewById(android.R.id.content).getRootView();

        JSONObject config = new JSONObject();
        try {
            config.put("flowType","checkout");
            config.put("amount", "231");
            config.put("hashURL", hashURL);
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
        SKEPSInitFlow.initProcess(config, handlers);
    }
}