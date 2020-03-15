package com.reactlibrary;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.pinpoint.PinpointConfiguration;
import com.amazonaws.mobileconnectors.pinpoint.PinpointManager;
import com.amazonaws.mobileconnectors.pinpoint.targeting.notification.NotificationDetails;
import com.amazonaws.services.pinpoint.model.ChannelType;
import com.baidu.android.pushservice.PushMessageReceiver;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.reactlibrary.modules.RNABPushNotificationJsDelivery;
import com.amazonaws.mobileconnectors.pinpoint.targeting.notification.NotificationClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Random;

import static android.content.Context.ACTIVITY_SERVICE;

public class AwsBaiduPushNotificationMessagingService extends PushMessageReceiver {
    private interface ReactContextInitListener {
        void contextInitialized(ReactApplicationContext context);
    }

    private final static String LOG_TAG = "ABMessagingService";

    private static PinpointManager pinpointManager;

    public static PinpointManager getPinpointManager(final Context applicationContext) {
        if (pinpointManager == null) {
            final AWSConfiguration awsConfig = new AWSConfiguration(applicationContext);
            AWSMobileClient.getInstance().initialize(applicationContext, awsConfig, new Callback<UserStateDetails>() {
                @Override
                public void onResult(UserStateDetails userStateDetails) {
                    Log.i("INIT", userStateDetails.getUserState().toString());

                }

                @Override
                public void onError(Exception e) {
                    Log.e("INIT", "Initialization error.", e);
                }
            });

            Log.e("awsconfig", awsConfig.toString());

            PinpointConfiguration pinpointConfig = new PinpointConfiguration(
                    applicationContext,
                    AWSMobileClient.getInstance(),
                    awsConfig)
                    .withChannelType(ChannelType.BAIDU);

            pinpointManager = new PinpointManager(pinpointConfig);

        }
        return pinpointManager;
    }

    @Override
    public void onBind(Context context, int errorCode, String appid, String userId, final String channelId,
                       String requestId) {
        Log.d(LOG_TAG, "onBind");
        Log.d(LOG_TAG, "onBind");
        Log.e("userId", userId);
        Log.e("channelId", channelId);
        getPinpointManager(context);
        pinpointManager.getNotificationClient().registerDeviceToken(userId, channelId);

        final String duypx = pinpointManager.getNotificationClient().getDeviceToken();

        Context applicationContext = context.getApplicationContext();
        handleEvent(applicationContext, new ReactContextInitListener() {
            @Override
            public void contextInitialized(ReactApplicationContext context) {
                WritableMap params = Arguments.createMap();
                params.putString("deviceToken", duypx);
                RNABPushNotificationJsDelivery jsDelivery = new RNABPushNotificationJsDelivery(context);
                jsDelivery.sendEvent("remoteNotificationsRegistered", params);
            }
        });
    }

    @Override
    public void onMessage(Context context, String message, String customContentString) {
        Log.d(LOG_TAG, "onMessage");
        NotificationDetails details = NotificationDetails.builder()
                .message(message)
                .intentAction(NotificationClient.BAIDU_INTENT_ACTION)
                .build();


        pinpointManager.getNotificationClient().handleCampaignPush(details);
//        JSONObject data = getPushData(message);
//        final Bundle bundle = createBundleFromMessage(message);
//        if (data != null) {
//            final int badge = data.optInt("badge", -1);
//            if (badge >= 0) {
//                ApplicationBadgeHelper.INSTANCE.setApplicationIconBadgeNumber(context, badge);
//            }
//        }
//
//        Context applicationContext = context.getApplicationContext();
//
//        Log.v(LOG_TAG, "onMessageReceived: " + bundle);
//
//        handleEvent(applicationContext, new ReactContextInitListener() {
//            @Override
//            public void contextInitialized(ReactApplicationContext context) {
//                handleRemotePushNotification(context, bundle);
//            }
//        });
    }

    private void handleEvent(final Context applicationContext, final ReactContextInitListener reactContextInitListener) {
        // We need to run this on the main thread, as the React code assumes that is true.
        // Namely, DevServerHelper constructs a Handler() without a Looper, which triggers:
        // "Can't create handler inside thread that has not called Looper.prepare()"
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                // Construct and load our normal React JS code bundle
                if (applicationContext instanceof ReactApplication) {
                    ReactInstanceManager mReactInstanceManager = ((ReactApplication) applicationContext).getReactNativeHost().getReactInstanceManager();
                    com.facebook.react.bridge.ReactContext context = mReactInstanceManager.getCurrentReactContext();
                    // If it's constructed, send a notification
                    if (context != null) {
                        reactContextInitListener.contextInitialized((ReactApplicationContext) context);
                    } else {
                        // Otherwise wait for construction, then send the notification
                        mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                            public void onReactContextInitialized(ReactContext context) {
                                reactContextInitListener.contextInitialized((ReactApplicationContext) context);
                            }
                        });
                        if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                            // Construct it in the background
                            mReactInstanceManager.createReactContextInBackground();
                        }
                    }
                }
            }
        });
    }

//    private Bundle createBundleFromMessage(String message) {
//        JSONObject jsonObject;
//        Bundle extras = null;
//        try {
//            jsonObject = new JSONObject(message);
//            extras = new Bundle();
//            extras.putString(BaiduPushConstants.TITLE, jsonObject.optString(BaiduPushConstants.TITLE));
//            extras.putString(BaiduPushConstants.MESSAGE, jsonObject.optString(BaiduPushConstants.DESCRIPTION));
//            JSONObject customContent = jsonObject.optJSONObject(BaiduPushConstants.CUSTOM_CONTENT);
//            if (customContent != null) {
//                extras.putString(BaiduPushConstants.DATA, jsonObject.optJSONObject(BaiduPushConstants.CUSTOM_CONTENT).toString());
//            }
//        } catch (JSONException e) {
//            Log.e(LOG_TAG, e.getMessage());
//        }
//        return extras;
//    }
//
//    private JSONObject getPushData(String dataString) {
//        try {
//            return new JSONObject(dataString);
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    private void handleRemotePushNotification(ReactApplicationContext context, Bundle bundle) {
//
//        // If notification ID is not provided by the user for push notification, generate one at random
//        if (bundle.getString("id") == null) {
//            Random randomNumberGenerator = new Random(System.currentTimeMillis());
//            bundle.putString("id", String.valueOf(randomNumberGenerator.nextInt()));
//        }
//
//        Boolean isForeground = isApplicationInForeground(context);
//
//        RNPushNotificationJsDelivery jsDelivery = new RNPushNotificationJsDelivery(context);
//        bundle.putBoolean("foreground", isForeground);
//        bundle.putBoolean("userInteraction", false);
//        jsDelivery.notifyNotification(bundle);
//
//        // If contentAvailable is set to true, then send out a remote fetch event
//        if (bundle.getString("contentAvailable", "false").equalsIgnoreCase("true")) {
//            jsDelivery.notifyRemoteFetch(bundle);
//        }
//
//        Log.v(LOG_TAG, "sendNotification: " + bundle);
//
//        if (!isForeground) {
//            Application applicationContext = (Application) context.getApplicationContext();
//            RNPushNotificationHelper pushNotificationHelper = new RNPushNotificationHelper(applicationContext);
//            pushNotificationHelper.sendToNotificationCentre(bundle);
//        }
//    }

    private boolean isApplicationInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        String packageName = context.getApplicationContext().getPackageName();
        List<ActivityManager.RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
        if (processInfos != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
                if (processInfo.processName.equals(packageName)) {
                    if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        for (String d : processInfo.pkgList) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onNotificationClicked(Context context, String title, String description, String customContentString) {
        Log.d(LOG_TAG, "onNotificationClicked");
    }

    @Override
    public void onNotificationArrived(Context context, String title, String description, String customContentString) {
        Log.d(LOG_TAG, "onNotificationArrived");
    }

    @Override
    public void onUnbind(Context context, int errorCode, String s) {
        Log.d(LOG_TAG, "onUnbind");
    }

    @Override
    public void onSetTags(Context context, int errorCode, List<String> list, List<String> list1, String s) {
        Log.d(LOG_TAG, "onSetTags");
    }

    @Override
    public void onDelTags(Context context, int errorCode, List<String> list, List<String> list1, String s) {
        Log.d(LOG_TAG, "onDelTags");

    }

    @Override
    public void onListTags(Context context, int i, List<String> list, String s) {
        Log.d(LOG_TAG, "onListTags");
    }
}
