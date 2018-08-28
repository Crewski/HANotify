# Home Assistant Notify
Android actionable notifications for Home Assistant.

## Setup
1.  Copy the fcm-android.py file into your /custom_components/notify/ folder (create it if you don't already have it)
2.  In your configuration.yaml file, add the following to initialize the components:

```
notify:    
  - name: android
    platform: fcm-android
    server_key (OPTIONAL: only if using your own FCM Project): MYSERVERKEY123456
```

3.  Reboot Home Assistant.
4.  Install the HANotify.apk file onto your android device (or compile it in Android Studio).
5.  Enter your Home Assistant URL and API PASSWORD into the app, then click Register
      *NOTE - This components requires an API PASSWORD to be set.
6.  Send some notifications!

## Usage
#### Sending the notification
The android actionable notifications are set up the same as the html5 notifications.  The following parameters can be sent:
```
title
target
message
data:
  actions
  color
  message_type
  tag
  dismiss

```

| Parameter | Required | Description |
| --- | --- | --- |
| message | Required | Body of the notification |
| title | Optional | Title for the notification, defaul value: Home Assistant |
| data | Optional | Extra parameters for the notification |

Parameters for the data section of the notification.  Everything here is optional.
| Parameter | Description |
| --- | --- | 
| color | A hex color such as #FF0000, default value is a blue |
| message_type | 'notification' or 'data', defaults to 'data'.  This is the type of FCM that is sent.  Notification has higher priority, but can't include actions or be dismissed by Home Assistant. |
| tag | Must be an integer.  Tag is the 'id' of the notifications.  Sending a new notification to the same tag will overwrite the current notifcation instead of creating a separate one. |
| dismiss | true or false, requires a tag parameter.  If true, the notification will be dismissed. |
| actions | Array of ojects (up to 3) with an 'action' and 'title'.  The title will be the button text on the notification, the action is what is sent back in the callback |


In order to send a "regular" notification without actions, all you have to do is not include them in the call to the service, such as below.
```
- service: notify.android
  data:
    message: Anne has arrived home
```
 This will send a simple push notification without any action buttons.
 
 
 If you want to include some actions, something like this will work:
```
- service: notify.android
  data:
    message: Anne has arrived home
    data:
      actions:
        - action: open
          title: Open Home Assistant
        - action: open_door
          title: Open door 
```
  This will have two buttons, one that says "Open Door" and one that says "Open Home Assistant".  The action for each button ("open" or "open_door" in this example) will be returned in the callback.
  
#### Handling the callback
The callback is pushed to the event bus.  It can be accessed via fcm_android_notications.clicked.  The "action" of the button that was pressed is included in the event_data.  So an automation would looks something like:
```
- alias: TEST RESPONSE
  trigger:
  - event_data:
      action: open_door
    event_type: fcm_android_notifications.clicked
    platform: event
  condition: []
  action:
  - data:
      entity_id: light.front_door
    service: light.turn_on
```

## How it works
When you clicked register on the app, it sends a firebase token back to Home Assistant.  That token is saved into the fcm-android-registrations.conf file.  This token is what is used to identify what devices to send the notification to.  If the notifications involve actions, the token for the device is included in the callback.  Before the callback is processed, the token is checked against the fcm-android-registrations.conf file for validity.

## Using your own FCM project
If you want to use your own new or existing FCM project, follow these steps.  Basically you just use the `server_key` in the configuration.yaml and a different google-services.json in the app.
1.	Log in to https://console.firebase.google.com 
2.	Create a new project or use an existing one
3.	Click on the settings icon next to Project Overview and select Project settings
4.	Click on the Cloud Messaging tab and find the Legacy Server Key
5.	In your configuration.yaml file, use the Legacy Server Key for the server_key variable:
6.	Back in the Firebase settings page, click on the General tab
7.	Under “your apps” click on the Add app button
8.	Select Add Firebase to your Android App
9.	enter “com.crewski.hanotify” for the Android Package Name and click Register App
a.	If you are planning on changing the package name for the Android app, you would enter that new name here
10.	Download the google-services.json
11.	Replace the google-services.json found in the android app with the one you just downloaded
12.	Compile and install your new app
