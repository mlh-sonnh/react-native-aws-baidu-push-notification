// import { NativeModules } from 'react-native';

// const { AwsBaiduPushNotification } = NativeModules;

// export default AwsBaiduPushNotification;

import Amplify, { ConsoleLogger as Logger } from '@aws-amplify/core';
import NotificationClass from './PushNotification';

const _instance = new NotificationClass(null);
const PushNotification = _instance;

Amplify.register(PushNotification);
export default PushNotification;
