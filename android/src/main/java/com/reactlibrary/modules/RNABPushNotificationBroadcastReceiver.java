package com.reactlibrary.modules;

import java.util.HashMap;
import java.util.Map;
import android.content.BroadcastReceiver;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;

import com.reactlibrary.modules.RNABPushNotificationJsDelivery;

public class RNABPushNotificationBroadcastReceiver extends BroadcastReceiver {
    private final static String LOG_TAG = "RNABBroadcastReceiver";

    private Class getMainActivityClass(Context context) {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openApp(Context context) {
        Class intentClass = getMainActivityClass(context);
        Intent launchIntent = new Intent(context, intentClass);
        if (launchIntent == null) {
            Log.e(LOG_TAG, "Couldn't get app launch intent for campaign notification.");
            return;
        }

        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        launchIntent.setPackage(null);
        context.startActivity(launchIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(LOG_TAG, "broadcaster received");

        // send the message to device emitter
        // Construct and load our normal React JS code bundle
        ReactInstanceManager mReactInstanceManager = ((ReactApplication) context.getApplicationContext()).getReactNativeHost().getReactInstanceManager();
        ReactContext reactContext = mReactInstanceManager.getCurrentReactContext();
        RNABPushNotificationJsDelivery jsDelivery = new RNABPushNotificationJsDelivery((ReactApplicationContext) reactContext);
        jsDelivery.emitNotificationOpened(intent.getBundleExtra("notification"));

        openApp(context);
    }
}
