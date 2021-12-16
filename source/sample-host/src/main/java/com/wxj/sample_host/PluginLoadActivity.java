package com.wxj.sample_host;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

public class PluginLoadActivity extends Activity {

    private ViewGroup mViewGroup;

    private Handler mHandler = new Handler();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        mViewGroup = findViewById(R.id.container);

        startPlugin();

    }

    public void startPlugin() {

        PluginHelper.getInstance().singlePool.execute(new Runnable() {
            @Override
            public void run() {

                HostApplication.get

            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }


}
