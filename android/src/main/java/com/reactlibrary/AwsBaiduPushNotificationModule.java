package com.reactlibrary;

import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class AwsBaiduPushNotificationModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private static final String LOG_TAG = "PushNotificationModule";
    private boolean receiverRegistered;

    public AwsBaiduPushNotificationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.receiverRegistered = false;
    }

    @Override
    public String getName() {
        return "AwsBaiduPushNotification";
    }

    @ReactMethod
    public void initialize(String apiKey) {
        ReactApplicationContext context = getReactApplicationContext();
        Log.i(LOG_TAG, "initializing RNPushNotificationModule");
        if (!this.receiverRegistered) {
            this.receiverRegistered = true;
            Log.i(LOG_TAG, "registering receiver");
            // Application applicationContext = (Application) context.getApplicationContext();
            // RNABPushNotificationBroadcastReceiver receiver = new RNABPushNotificationBroadcastReceiver();
            // IntentFilter intentFilter = new IntentFilter("com.amazonaws.amplify.pushnotification.NOTIFICATION_OPENED");
            // applicationContext.registerReceiver(receiver, intentFilter);
            // PushManager.startWork(this.reactContext, PushConstants.LOGIN_TYPE_API_KEY,
            //         apiKey);
        }
    }

}
