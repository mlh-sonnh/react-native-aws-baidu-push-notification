package com.reactlibrary;

import android.app.ActivityManager;
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

import java.util.List;

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
        getPinpointManager(context);
        pinpointManager.getNotificationClient().registerDeviceToken(userId, channelId);

        final String duypx = pinpointManager.getNotificationClient().getDeviceToken();

        Context applicationContext = context.getApplicationContext();
        handleEvent(applicationContext, new ReactContextInitListener() {
            @Override
            public void contextInitialized(ReactApplicationContext context) {
                WritableMap params = Arguments.createMap();
                params.putString("token", duypx);
                RNABPushNotificationJsDelivery jsDelivery = new RNABPushNotificationJsDelivery(context);
                jsDelivery.sendEvent("remoteTokenReceived", params);
            }
        });
    }

    @Override
    public void onMessage(Context context, String message, String customContentString) {
        Log.d(LOG_TAG, "onMessage");
        final NotificationDetails details = NotificationDetails.builder()
                .message(message)
                .intentAction(NotificationClient.BAIDU_INTENT_ACTION)
                .build();

        pinpointManager.getNotificationClient().handleCampaignPush(details);

        final Boolean isForeground = isApplicationInForeground(context);
        Bundle bundle = details.getBundle();
        bundle.putBoolean("foreground", isForeground);
        handleEvent(context, new ReactContextInitListener() {
            @Override
            public void contextInitialized(ReactApplicationContext context) {
                RNABPushNotificationJsDelivery jsDelivery = new RNABPushNotificationJsDelivery(context);
                jsDelivery.emitNotificationReceived(details.getBundle(), isForeground);
            }
        });
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
