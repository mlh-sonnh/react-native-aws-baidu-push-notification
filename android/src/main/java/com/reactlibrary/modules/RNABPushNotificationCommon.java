package com.reactlibrary.modules;

import android.os.Build;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class RNABPushNotificationCommon {
    public RNABPushNotificationCommon() {
        return;
    }

    public static String convertJSON(Bundle bundle) {
        try {
            JSONObject json = convertJSONObject(bundle);
            return json.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    public static JSONObject convertJSONObject(Bundle bundle) throws JSONException {
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
