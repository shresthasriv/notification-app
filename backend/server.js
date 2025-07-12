const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const path = require('path');
const fs = require('fs');
const admin = require('firebase-admin');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(bodyParser.json());

// Initialize Firebase Admin SDK
let messaging;
let isFirebaseInitialized = false;

try {
  const serviceAccountPath = path.join(__dirname, 'service-account-key.json');
  if (fs.existsSync(serviceAccountPath)) {
    const serviceAccount = require('./service-account-key.json');
    
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
    
    messaging = admin.messaging();
    isFirebaseInitialized = true;
    console.log('Firebase Admin SDK initialized successfully');
    console.log('Project ID:', serviceAccount.project_id);
  } else {
    console.log('Service account key file not found at:', serviceAccountPath);
    console.log('Please make sure service-account-key.json is in the backend directory');
  }
} catch (error) {
  console.log('Firebase Admin SDK initialization failed:', error.message);
}

const sendFCMNotification = async (token, title, body, data = {}) => {
  if (!isFirebaseInitialized || !messaging) {
    throw new Error('Firebase not initialized. Please ensure service-account-key.json is properly configured.');
  }

  const stringifiedData = {};
  Object.keys(data).forEach(key => {
    stringifiedData[key] = String(data[key]);
  });

  const message = {
    token: token,
    notification: {
      title: title,
      body: body,
    },
    data: stringifiedData,
    android: {
      notification: {
        channelId: 'default',
        priority: 'high',
        defaultSound: true,
        color: '#25D366'
      }
    }
  };

  try {
    const response = await messaging.send(message);
    console.log('Successfully sent message:', response);
    return { success: 1, message_id: response };
  } catch (error) {
    console.error('Error sending message:', error);
    throw new Error(`FCM Error: ${error.message}`);
  }
};

app.get('/', (req, res) => {
  res.json({
    message: 'Notification Backend API',
    status: 'running',
    firebase: 'connected',
    endpoints: {
      'GET /health': 'Health check',
      'POST /send-message': 'Send message notification'
    }
  });
});

app.get('/health', (req, res) => {
  res.json({ 
    status: 'healthy', 
    timestamp: new Date().toISOString(),
    firebase: isFirebaseInitialized
  });
});

app.post('/send-message', async (req, res) => {
  try {
    const { token, sender, message, timestamp } = req.body;

    if (!token || !sender || !message) {
      return res.status(400).json({ error: 'Token, sender, and message are required' });
    }

    const notification = {
      token: token,
      notification: {
        title: sender,
        body: message
      },
      data: {
        type: 'message',
        sender: sender,
        message: message,
        timestamp: timestamp || Date.now().toString(),
        chatId: `chat_${sender.toLowerCase().replace(/\s+/g, '_')}`
      },
      android: {
        notification: {
          channelId: 'messages',
          priority: 'high',
          defaultSound: true,
          color: '#25D366',
          icon: 'ic_notification'
        }
      }
    };

    const response = await sendFCMNotification(token, sender, message, notification.data);
    res.json({ success: true, messageId: response.message_id, message: 'Message notification sent successfully' });
  } catch (error) {
    console.error('Error sending message notification:', error);
    res.status(500).json({ error: 'Failed to send message notification', details: error.message });
  }
});

app.use((err, req, res, next) => {
  console.error('Server error:', err);
  res.status(500).json({ error: 'Internal server error' });
});

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
  console.log(`API Documentation: http://localhost:${PORT}/`);
}); 