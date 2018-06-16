package com.yuntech.eb211.airpollutiondetector;

import android.app.Application;
import android.content.Context;

public class GlobalVariable extends Application {
    private Context context;

    public void setContext(Context context){
        this.context=context;
    }

    public Context getContext() {
        return context;
    }
}
