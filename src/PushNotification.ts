import {
    NativeModules,
    DeviceEventEmitter,
} from 'react-native';

const AwsBaiduPushNotification = NativeModules.AwsBaiduPushNotification;
const REMOTE_NOTIFICATION_RECEIVED = 'remoteNotificationReceived';
const REMOTE_TOKEN_RECEIVED = 'remoteTokenReceived';
const REMOTE_NOTIFICATION_OPENED = 'remoteNotificationOpened';

export default class PushNotifications {
    private _config;
    private _androidInitialized;

    constructor(config) {
        if (config) {
            this.configure(config);
        } else {
            this._config = {};
        }
        this._androidInitialized = false;
    }

    getModuleName() {
        return 'PushNotificationsBaidu';
    }
    
    configure(config) {
        let conf = config ? config.PushNotification || config : {};

        this._config = Object.assign(
            this._config,
            conf
        );

        if (!this._androidInitialized) {
            this.initializeAndroid();
            this._androidInitialized = true;
        }
    }

    onRegister(handler) {
        if (typeof handler === 'function') {
            this.addEventListenerForAndroid(REMOTE_TOKEN_RECEIVED, handler);
        }
    }
    
    onNotificationReceived(handler) {
        if (typeof handler === 'function') {
            this.addEventListenerForAndroid(REMOTE_NOTIFICATION_RECEIVED, handler);
        }
    }
    
    async initializeAndroid() {
        const { apiKey } = this._config;
        console.log('ab', this._config, apiKey);
        AwsBaiduPushNotification.initialize(this._config.apiKey);
    }
    
    addEventListenerForAndroid(event, handler) {
        const that = this;
        console.log(event, handler);
        const listener = DeviceEventEmitter.addListener(event, data => {
            // for on notification
            if (event === REMOTE_NOTIFICATION_RECEIVED) {
                console.log(that.parseMessagefromAndroid(data));
                handler(that.parseMessagefromAndroid(data));
                return;
            }
            if (event === REMOTE_TOKEN_RECEIVED) {
                handler(data);
                return;
            }
            if (event === REMOTE_NOTIFICATION_OPENED) {
                handler(that.parseMessagefromAndroid(data, 'opened'));
                return;
            }
        });
        console.log(listener);
    }
    
    parseMessagefromAndroid(message, from?) {
        let dataObj = null;
        try {
            dataObj = message.dataJSON ? JSON.parse(message.dataJSON) : null;
        } catch (e) {
            console.debug('Failed to parse the data object', e);
            return;
        }

        if (!dataObj) {
            console.debug('no notification payload received');
            return dataObj;
        }

        if (from === 'opened') {
            return dataObj;
        }

        let ret = null;
        const dataPayload = dataObj || {};
        if (dataPayload['pinpoint.campaign.campaign_id']) {
            ret = {
                title: dataPayload['pinpoint.notification.title'],
                body: dataPayload['pinpoint.notification.body'],
                data: dataPayload,
                foreground: dataObj.foreground,
            };
        }
        return ret;
    }
}