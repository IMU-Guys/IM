package com.imuguys.im;

import android.app.Application;
import android.content.Context;

public class App extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        GlobalConfig.CONTEXT = base;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
