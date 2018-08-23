# HANotify
Android actionable notifications for Home Assistant.

## Setup
1.  Copy the fcm-android.py file into your /custom_components/notify/ folder (create it if you don't already have it)
2.  In your configuration.yaml file, add the following to initialize the components:

```
notify:    
  - name: android
    platform: fcm-android
```

3.  Reboot Home Assistant.
4.  Install the HANotify.apk file onto your android device (or compile it in Android Studio).
5.  Enter your Home Assistant URL and API PASSWORD into the app, then click Register
      *NOTE - This components requires an API PASSWORD to be set.
6.  Send some notifications!

## Usage
####Sending the notification
The android actionable notifications are set up the same as the html5 notifications.  The following parameters can be sent:
```
title -> if not provided, Home Assistant will be used
target -> if not provided, will send to all registered devices
message
data:
  actions -> an array of actions (up to 3) that have an action and a title.  The action will be returned in the callback
  color -> the color is in hex format, so something like #FF00FF would work.  If nothing is provided, Home Assistant blue is used
```

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
When you clicked register on the app, it sends a firebase token back to Home Assistant.  That token is saved into the fcm-android-registrations.conf file.  This token is what is used to identify what devices to send the notification to.  If the notifications involve actions, the token for the device is included in the callback.  Before the callback is process, the toekn is checked against the fcm-android-registrations.conf file for validity.
