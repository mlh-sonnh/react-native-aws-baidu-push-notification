import {
    NativeModules,
    DeviceEventEmitter,
    AppState,
} from 'react-native';
import AsyncStorage from '@react-native-community/async-storage';
import Amplify, { ConsoleLogger as Logger } from '@aws-amplify/core';

const AwsBaiduPushNotifications = NativeModules.AwsBaiduPushNotifications;
const REMOTE_NOTIFICATION_RECEIVED = 'remoteNotificationReceived';
const REMOTE_TOKEN_RECEIVED = 'remoteTokenReceived';
const REMOTE_NOTIFICATION_OPENED = 'remoteNotificationOpened';
const logger = new Logger('Notification');

export default class PushNotifications {
    private _config;
    private handlers;
    private _currentState;
    private _androidInitialized;
    private _iosInitialized;

    constructor(config) {
        if (config) {
            this.configure(config);
        } else {
            this._config = {};
        }
        this.handlers = [];
        this.updateEndpoint = this.updateEndpoint.bind(this);
        this.handleCampaignPush = this.handleCampaignPush.bind(this);
        this.handleCampaignOpened = this.handleCampaignOpened.bind(this);
        // this._checkIfOpenedByCampaign = this._checkIfOpenedByCampaign.bind(this);
        this._currentState = AppState.currentState;
        this._androidInitialized = false;
        this._iosInitialized = false;
    }

    getModuleName() {
        return 'PushNotificationsBaidu';
    }
    
    configure(config) {
        let conf = config ? config.PushNotification || config : {};

        if (conf['aws_mobile_analytics_app_id']) {
            conf = {
                appId: conf['aws_mobile_analytics_app_id'],
            };
        }

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
    
    async initializeAndroid() {
        this.addEventListenerForAndroid(REMOTE_TOKEN_RECEIVED, this.updateEndpoint);
        this.addEventListenerForAndroid(
            REMOTE_NOTIFICATION_OPENED,
            this.handleCampaignOpened
        );
        this.addEventListenerForAndroid(
            REMOTE_NOTIFICATION_RECEIVED,
            this.handleCampaignPush
        );
        const { apiKey } = this._config;
        AwsBaiduPushNotifications.initialize(apiKey);

        // check if the token is cached properly
        // if (!(await this._registerTokenCached())) {
        //     const { appId } = this._config;
        //     const cacheKey = 'push_token' + appId;
        //     AwsBaiduPushNotifications.getToken(token => {
        //         logger.debug('Get the token from Firebase Service', token);
        //         // resend the token in case it's missing in the Pinpoint service
        //         // the token will also be cached locally
        //         this.updateEndpoint(token);
        //     });
        // }
    }

    async _registerTokenCached(): Promise<boolean> {
        const { appId } = this._config;
        const cacheKey = 'push_token' + appId;
        return AsyncStorage.getItem(cacheKey).then(lastToken => {
            if (lastToken) return true;
            else return false;
        });
    }
    
    addEventListenerForAndroid(event, handler) {
        const that = this;
        const listener = DeviceEventEmitter.addListener(event, data => {
            // for on notification
            if (event === REMOTE_NOTIFICATION_RECEIVED) {
                handler(that.parseMessagefromAndroid(data));
                return;
            }
            if (event === REMOTE_TOKEN_RECEIVED) {
                const dataObj = data.dataJSON ? JSON.parse(data.dataJSON) : {};
                handler(dataObj.refreshToken);
                return;
            }
            if (event === REMOTE_NOTIFICATION_OPENED) {
                handler(that.parseMessagefromAndroid(data, 'opened'));
                return;
            }
        });
    }
    
    updateEndpoint(token) {
        if (!token) {
            logger.debug('no device token recieved on register');
            return;
        }

        const { appId } = this._config;
        const cacheKey = 'push_token' + appId;
        logger.debug('update endpoint in push notification', token);
        AsyncStorage.getItem(cacheKey)
            .then(lastToken => {
                if (!lastToken || lastToken !== token) {
                    logger.debug('refresh the device token with', token);
                    const config = {
                        Address: token,
                        OptOut: 'NONE',
                    };
                    if (
                        Amplify.Analytics &&
                        typeof Amplify.Analytics.updateEndpoint === 'function'
                    ) {
                        Amplify.Analytics.updateEndpoint(config)
                            .then(data => {
                                logger.debug(
                                    'update endpoint success, setting token into cache'
                                );
                                AsyncStorage.setItem(cacheKey, token);
                            })
                            .catch(e => {
                                // ........
                                logger.debug('update endpoint failed', e);
                            });
                    } else {
                        logger.debug('Analytics module is not registered into Amplify');
                    }
                }
            })
            .catch(e => {
                logger.debug('set device token in cache failed', e);
            });
    }
    
    parseMessagefromAndroid(message, from?) {
        let dataObj = null;
        try {
            dataObj = message.dataJSON ? JSON.parse(message.dataJSON) : null;
        } catch (e) {
            logger.debug('Failed to parse the data object', e);
            return;
        }

        if (!dataObj) {
            logger.debug('no notification payload received');
            return dataObj;
        }

        if (from === 'opened') {
            return dataObj;
        }

        let ret = null;
        const dataPayload = dataObj.data || {};
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
    
    handleCampaignOpened(rawMessage) {
        logger.debug('handleCampaignOpened, raw data', rawMessage);
        let campaign = null;

        const data = rawMessage;
        campaign = {
            campaign_id: data['pinpoint.campaign.campaign_id'],
            campaign_activity_id: data['pinpoint.campaign.campaign_activity_id'],
            treatment_id: data['pinpoint.campaign.treatment_id'],
        };
        

        if (!campaign) {
            logger.debug('no message received for campaign opened');
            return;
        }

        const attributes = {
            campaign_activity_id: campaign['campaign_activity_id'],
            treatment_id: campaign['treatment_id'],
            campaign_id: campaign['campaign_id'],
        };

        const eventType = '_campaign.opened_notification';

        if (Amplify.Analytics && typeof Amplify.Analytics.record === 'function') {
            Amplify.Analytics.record({
                name: eventType,
                attributes,
                immediate: true,
            });
        } else {
            logger.debug('Analytics module is not registered into Amplify');
        }
    }

    handleCampaignPush(rawMessage) {
		let message = rawMessage;
		let campaign = null;
        const { data } = rawMessage;
        campaign = {
            campaign_id: data['pinpoint.campaign.campaign_id'],
            campaign_activity_id: data['pinpoint.campaign.campaign_activity_id'],
            treatment_id: data['pinpoint.campaign.treatment_id'],
        };

		if (!campaign) {
			logger.debug('no message received for campaign push');
			return;
		}

		const attributes = {
			campaign_activity_id: campaign['campaign_activity_id'],
			isAppInForeground: message.foreground ? 'true' : 'false',
			treatment_id: campaign['treatment_id'],
			campaign_id: campaign['campaign_id'],
		};

		const eventType = message.foreground
			? '_campaign.received_foreground'
			: '_campaign.received_background';

		if (Amplify.Analytics && typeof Amplify.Analytics.record === 'function') {
			Amplify.Analytics.record({
				name: eventType,
				attributes,
				immediate: true,
			});
		} else {
			logger.debug('Analytics module is not registered into Amplify');
		}
	}
}