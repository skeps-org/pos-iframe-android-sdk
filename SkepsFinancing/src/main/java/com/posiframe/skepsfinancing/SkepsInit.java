package com.posiframe.skepsfinancing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class SkepsInit extends  LinearLayout{

    LinearLayout layout = null;
    TextView mainTextView = null;
    Context mContext = null;
    Intent i;
    public String baseURL;
    public SkepsInit(Context context) {
        super(context);
        mContext = context;
    }

    public SkepsInit(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SkepsInit);
        String opportunityAmount = a.getString(R.styleable.SkepsInit_opportunityAmount);
        String promotionType = a.getString(R.styleable.SkepsInit_promotionType);
        String service = Context.LAYOUT_INFLATER_SERVICE;
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(service);

        layout = (LinearLayout) li.inflate(R.layout.skepsinit, this, true);

        mainTextView = (TextView) layout.findViewById(R.id.main_text);
//        fetchBanner("");
        mainTextView.setText(getBanner(opportunityAmount));
        mainTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i;
                i = new Intent(v.getContext(), SkepsFinancing.class);
                i.putExtra("flowType", "check-eligibility");
                i.putExtra("amount", "230");
                mContext.startActivity(i);
            }
        });

        a.recycle();
    }


    public interface SkepsCheckoutHandlerInterface {
        void successHandler(String result);
        void failureHandler(String result);
    }


    public void initProcess(JSONObject config, SkepsCheckoutHandlerInterface callback) {
        i = new Intent(mContext, SkepsFinancing.class);
        String flowType;
        String amount;
        try {
            flowType = ((JSONObject) config).getString("flowType");
            amount = ((JSONObject) config).getString("amount");

            i.putExtra("flowType",flowType);
            i.putExtra("amount", amount);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        i.putExtra(SkepsFinancing.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {
                    String val = resultData.getString("data");
                    callback.successHandler(val);
                } else if (resultCode == Activity.RESULT_CANCELED){
                    // failure case
                    String error = resultData.getString("data");
                    callback.failureHandler(error);
                } else {
                }
            }
        });
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }

    public static String getBanner(String amount) {
        return "5 interest-free payments of $"+calculateEMI(Integer.parseInt(amount))+" info with Slice by FNBO";
    }

    public static int calculateEMI(int amount) {
        int A = amount * 100;
        Number B = Math.floor(A / 5);
        Number C = A % 5;
        return  (B.intValue() + C.intValue()) / 100;
    }
}