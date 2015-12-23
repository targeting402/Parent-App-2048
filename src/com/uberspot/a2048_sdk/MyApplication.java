package com.uberspot.a2048_sdk;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.vk.sdk.VKSdk;

/**
 * Created by Greg Petrov on 06.10.2015.
 */
public class MyApplication
        extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        VKSdk.initialize(
                getApplicationContext()
        );

        FacebookSdk.sdkInitialize(
                getApplicationContext()
        );
    }
}
