import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Alert,
  TouchableOpacity,
  ScrollView,
  SafeAreaView,
  NativeModules,
  PermissionsAndroid,
  Platform,
} from 'react-native';
import messaging from '@react-native-firebase/messaging';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import AsyncStorage from '@react-native-async-storage/async-storage';

const { NotificationModule } = NativeModules;
const Stack = createStackNavigator();

interface NotificationData {
  id: number;
  title: string;
  body: string;
  data: any;
  timestamp: string;
}

const HomeScreen = ({ navigation }: { navigation: any }) => {
  const [fcmToken, setFcmToken] = useState('');
  const [notifications, setNotifications] = useState<NotificationData[]>([]);

  useEffect(() => {
    requestUserPermission();
    const unsubscribe = setupNotificationListeners();
    createNotificationChannel();
    loadStoredNotifications();
    checkInitialNotification();
    
    return unsubscribe;
  }, []);

  const checkInitialNotification = async () => {
    try {
      // Check if app was opened from a notification
      const initialNotification = await NotificationModule.getInitialNotification();
      if (initialNotification) {
        console.log('App opened from notification:', initialNotification);
        
        // Create notification object
        const notification: NotificationData = {
          id: Date.now(),
          title: initialNotification.sender || initialNotification.title || 'New Message',
          body: initialNotification.message || initialNotification.body || 'You have a new message',
          data: initialNotification,
          timestamp: initialNotification.timestamp || new Date().toISOString(),
        };
        
        // Add to notifications list
        addNotification(notification);
        
        // Navigate to notification detail
        setTimeout(() => {
          navigation.navigate('NotificationDetail', { notification });
        }, 500);
      }
    } catch (error) {
      console.error('Error checking initial notification:', error);
    }
  };

  const requestUserPermission = async () => {
    if (Platform.OS === 'android' && Platform.Version >= 33) {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
      );
      if (granted !== PermissionsAndroid.RESULTS.GRANTED) {
        Alert.alert('Permission denied', 'Notification permission is required');
        return;
      }
    }

    const authStatus = await messaging().requestPermission();
    const enabled =
      authStatus === messaging.AuthorizationStatus.AUTHORIZED ||
      authStatus === messaging.AuthorizationStatus.PROVISIONAL;

    if (enabled) {
      getFCMToken();
    }
  };

  const getFCMToken = async () => {
    try {
      const token = await NotificationModule.getFCMToken();
      setFcmToken(token);
      console.log('FCM Token:', token);
      await AsyncStorage.setItem('fcm_token', token);
    } catch (error) {
      console.error('Error getting FCM token:', error);
    }
  };

  const createNotificationChannel = () => {
    NotificationModule.createNotificationChannel();
  };

  const createNotificationFromRemoteMessage = (remoteMessage: any): NotificationData => {
    return {
      id: Date.now(),
      title: remoteMessage.notification?.title || 'New Message',
      body: remoteMessage.notification?.body || 'You have a new message',
      data: remoteMessage.data,
      timestamp: new Date().toISOString(),
    };
  };

  const setupNotificationListeners = () => {
    const unsubscribeForeground = messaging().onMessage(async remoteMessage => {
      console.log('Foreground message received:', remoteMessage);
      const notification = createNotificationFromRemoteMessage(remoteMessage);
      
      addNotification(notification);
      
      Alert.alert(
        notification.title,
        notification.body,
        [
          { text: 'Dismiss', style: 'cancel' },
          { text: 'View', onPress: () => navigation.navigate('NotificationDetail', { notification }) },
        ]
      );
    });

    const unsubscribeOpened = messaging().onNotificationOpenedApp(remoteMessage => {
      console.log('Notification opened app:', remoteMessage);
      if (remoteMessage) {
        const notification = createNotificationFromRemoteMessage(remoteMessage);
        navigation.navigate('NotificationDetail', { notification });
      }
    });

    return () => {
      unsubscribeForeground();
      unsubscribeOpened();
    };
  };

  const addNotification = async (notification: NotificationData) => {
    const updatedNotifications = [notification, ...notifications];
    setNotifications(updatedNotifications);
    await AsyncStorage.setItem('notifications', JSON.stringify(updatedNotifications));
  };

  const loadStoredNotifications = async () => {
    try {
      const stored = await AsyncStorage.getItem('notifications');
      if (stored) {
        setNotifications(JSON.parse(stored));
      }
    } catch (error) {
      console.error('Error loading notifications:', error);
    }
  };

  const sendTestNotification = async () => {
    try {
      await NotificationModule.showCustomNotification(
        'Test Notification',
        'This is a test notification from the app',
        { screen: 'NotificationDetail', testData: 'hello' }
      );
    } catch (error) {
      console.error('Error sending test notification:', error);
    }
  };

  const clearAllNotifications = async () => {
    try {
      await NotificationModule.clearAllNotifications();
      setNotifications([]);
      await AsyncStorage.removeItem('notifications');
      Alert.alert('Success', 'All notifications cleared');
    } catch (error) {
      console.error('Error clearing notifications:', error);
    }
  };

  const simulateBackendNotification = async () => {
    try {
      if (!fcmToken) {
        Alert.alert('Error', 'FCM token not available');
        return;
      }

      const response = await fetch('http://10.0.2.2:3000/send-message', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          token: fcmToken,
          sender: 'John Doe',
          message: 'Hey! This is a simulated message from the backend 📱',
          timestamp: Date.now().toString()
        }),
      });

      const result = await response.json();
      
      if (result.success) {
        Alert.alert('Success', 'Backend notification sent successfully!');
      } else {
        Alert.alert('Error', result.error || 'Failed to send notification');
      }
    } catch (error) {
      console.error('Error calling backend:', error);
      Alert.alert('Error', 'Failed to connect to backend. Make sure server is running on port 3000');
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.scrollView}>
        <View style={styles.header}>
          <View style={styles.headerContent}>
            <View style={styles.headerText}>
              <Text style={styles.title}>Notification App</Text>
              <Text style={styles.subtitle}>Real-time Push Notifications</Text>
            </View>
            <TouchableOpacity style={styles.simulateButton} onPress={simulateBackendNotification}>
              <Text style={styles.simulateButtonText}>Simulate</Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Actions</Text>
          <TouchableOpacity style={styles.button} onPress={sendTestNotification}>
            <Text style={styles.buttonText}>Send Test Notification</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={clearAllNotifications}>
            <Text style={styles.buttonText}>Clear All Notifications</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Recent Notifications ({notifications.length})</Text>
          {notifications.length === 0 ? (
            <Text style={styles.emptyText}>No notifications yet</Text>
          ) : (
            notifications.slice(0, 5).map((notification) => (
              <TouchableOpacity
                key={notification.id}
                style={styles.notificationItem}
                onPress={() => navigation.navigate('NotificationDetail', { notification })}
              >
                <Text style={styles.notificationTitle}>{notification.title}</Text>
                <Text style={styles.notificationBody}>{notification.body}</Text>
                <Text style={styles.notificationTime}>
                  {new Date(notification.timestamp).toLocaleTimeString()}
                </Text>
              </TouchableOpacity>
            ))
          )}
          {notifications.length > 5 && (
            <TouchableOpacity
              style={styles.viewAllButton}
              onPress={() => navigation.navigate('NotificationsList')}
            >
              <Text style={styles.viewAllText}>View All Notifications</Text>
            </TouchableOpacity>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const NotificationDetailScreen = ({ route }: { route: any }) => {
  const { notification } = route.params;

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.scrollView}>
        <View style={styles.detailContainer}>
          <Text style={styles.detailTitle}>{notification.title}</Text>
          <Text style={styles.detailBody}>{notification.body}</Text>
          <Text style={styles.detailTime}>
            Received: {new Date(notification.timestamp).toLocaleString()}
          </Text>
          
          {notification.data && Object.keys(notification.data).length > 0 && (
            <View style={styles.dataContainer}>
              <Text style={styles.dataTitle}>Additional Data:</Text>
              {Object.entries(notification.data).map(([key, value]) => (
                <Text key={key} style={styles.dataItem}>
                  {key}: {String(value)}
                </Text>
              ))}
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const NotificationsListScreen = ({ navigation }: { navigation: any }) => {
  const [notifications, setNotifications] = useState<NotificationData[]>([]);

  useEffect(() => {
    loadStoredNotifications();
  }, []);

  const loadStoredNotifications = async () => {
    try {
      const stored = await AsyncStorage.getItem('notifications');
      if (stored) {
        setNotifications(JSON.parse(stored));
      }
    } catch (error) {
      console.error('Error loading notifications:', error);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.scrollView}>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>All Notifications</Text>
          {notifications.length === 0 ? (
            <Text style={styles.emptyText}>No notifications found</Text>
          ) : (
            notifications.map((notification) => (
              <TouchableOpacity
                key={notification.id}
                style={styles.notificationItem}
                onPress={() => navigation.navigate('NotificationDetail', { notification })}
              >
                <Text style={styles.notificationTitle}>{notification.title}</Text>
                <Text style={styles.notificationBody}>{notification.body}</Text>
                <Text style={styles.notificationTime}>
                  {new Date(notification.timestamp).toLocaleString()}
                </Text>
              </TouchableOpacity>
            ))
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const App = () => {
  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="Home">
        <Stack.Screen 
          name="Home" 
          component={HomeScreen} 
          options={{ title: 'Notifications' }}
        />
        <Stack.Screen 
          name="NotificationDetail" 
          component={NotificationDetailScreen} 
          options={{ title: 'Notification Details' }}
        />
        <Stack.Screen 
          name="NotificationsList" 
          component={NotificationsListScreen} 
          options={{ title: 'All Notifications' }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  scrollView: {
    flex: 1,
  },
  header: {
    backgroundColor: '#25D366',
    padding: 20,
    alignItems: 'center',
  },
  headerContent: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    width: '100%',
  },
  headerText: {
    flex: 1,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: 'white',
  },
  subtitle: {
    fontSize: 16,
    color: 'white',
    marginTop: 5,
  },
  simulateButton: {
    backgroundColor: '#25D366',
    padding: 10,
    borderRadius: 8,
  },
  simulateButtonText: {
    color: 'white',
    fontSize: 14,
    fontWeight: 'bold',
  },
  section: {
    backgroundColor: 'white',
    margin: 10,
    padding: 15,
    borderRadius: 10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#333',
  },
  button: {
    backgroundColor: '#25D366',
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 10,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
  notificationItem: {
    backgroundColor: '#f9f9f9',
    padding: 15,
    borderRadius: 8,
    marginBottom: 10,
    borderLeftWidth: 4,
    borderLeftColor: '#25D366',
  },
  notificationTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
  },
  notificationBody: {
    fontSize: 14,
    color: '#666',
    marginTop: 5,
  },
  notificationTime: {
    fontSize: 12,
    color: '#999',
    marginTop: 5,
  },
  emptyText: {
    textAlign: 'center',
    color: '#999',
    fontStyle: 'italic',
    padding: 20,
  },
  viewAllButton: {
    alignItems: 'center',
    padding: 10,
  },
  viewAllText: {
    color: '#25D366',
    fontSize: 16,
    fontWeight: 'bold',
  },
  detailContainer: {
    backgroundColor: 'white',
    margin: 10,
    padding: 20,
    borderRadius: 10,
  },
  detailTitle: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 10,
  },
  detailBody: {
    fontSize: 16,
    color: '#666',
    lineHeight: 24,
    marginBottom: 15,
  },
  detailTime: {
    fontSize: 14,
    color: '#999',
    marginBottom: 20,
  },
  dataContainer: {
    backgroundColor: '#f0f0f0',
    padding: 15,
    borderRadius: 8,
  },
  dataTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 10,
  },
  dataItem: {
    fontSize: 14,
    color: '#666',
    marginBottom: 5,
  },
});

export default App;
