package com.reactlibrary.modules;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

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
        sendEvent("remoteNotificationReceivedBaidu", params);
    }

    public void emitTokenReceived(Bundle bundle) {
        String bundleString = RNABPushNotificationCommon.convertJSON(bundle);

        WritableMap params = Arguments.createMap();
        params.putString("dataJSON", bundleString);
        Log.i("emit", "token registration");
        sendEvent("remoteTokenReceivedBaidu", params);
    }

    public void emitNotificationOpened(Bundle bundle) {
        String bundleString = RNABPushNotificationCommon.convertJSON(bundle);

        WritableMap params = Arguments.createMap();
        params.putString("dataJSON", bundleString);

        Log.i("emit", "notification opened: " + bundle);
        sendEvent("remoteNotificationOpenedBaidu", params);
    }

    public void sendEvent(String eventName, Object params) {
        if (context.hasActiveCatalystInstance()) {
            context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }

    public void notifyRemoteFetch(Bundle bundle) {
        String bundleString = convertJSON(bundle);
        WritableMap params = Arguments.createMap();
        params.putString("dataJSON", bundleString);
        sendEvent("remoteFetchBaidu", params);
    }

    public void notifyNotification(Bundle bundle) {
        String bundleString = convertJSON(bundle);

        WritableMap params = Arguments.createMap();
        params.putString("dataJSON", bundleString);

        sendEvent("remoteNotificationReceivedBaidu", params);
    }

    void notifyNotificationAction(Bundle bundle) {
        String bundleString = convertJSON(bundle);

        WritableMap params = Arguments.createMap();
        params.putString("dataJSON", bundleString);

        sendEvent("notificationActionReceivedBaidu", params);
    }

    String convertJSON(Bundle bundle) {
        try {
            JSONObject json = convertJSONObject(bundle);
            return json.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    // a Bundle is not a map, so we have to convert it explicitly
    JSONObject convertJSONObject(Bundle bundle) throws JSONException {
        JSONObject json = new JSONObject();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            Object value = bundle.get(key);
            if (value instanceof Bundle) {
                json.put(key, convertJSONObject((Bundle)value));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                json.put(key, JSONObject.wrap(value));
            } else {
                json.put(key, value);
            }
        }
        return json;
    }

}
