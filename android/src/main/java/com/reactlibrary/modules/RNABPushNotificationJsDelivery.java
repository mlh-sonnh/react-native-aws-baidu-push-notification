package com.reactlibrary.modules;

import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class RNABPushNotificationJsDelivery {
    private ReactApplicationContext context;

    public RNABPushNotificationJsDelivery(ReactApplicationContext reactContext) {
        context = reactContext;
    }

    public void emitNotificationReceived(Bundle bundle, Boolean isForeground) {
        String bundleString = RNABPushNotificationCommon.convertJSON(bundle);

        WritableMap params = Arguments.createMap();
        params.putString("dataJSON", bundleString);
        Log.i("emit", "notification emit");
        sendEvent("remoteNotificationReceived", params);
    }

    public void emitTokenReceived(Bundle bundle) {
        String bundleString = RNABPushNotificationCommon.convertJSON(bundle);

        WritableMap params = Arguments.createMap();
        params.putString("dataJSON", bundleString);
        Log.i("emit", "token registration");
        sendEvent("remoteTokenReceived", params);
    }

    public void emitNotificationOpened(Bundle bundle) {
        String bundleString = RNABPushNotificationCommon.convertJSON(bundle);

        WritableMap params = Arguments.createMap();
        params.putString("dataJSON", bundleString);

        Log.i("emit", "notification opened: " + bundle);
        sendEvent("remoteNotificationOpened", params);
    }

    public void sendEvent(String eventName, Object params) {
        if (context.hasActiveCatalystInstance()) {
            context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }
}
