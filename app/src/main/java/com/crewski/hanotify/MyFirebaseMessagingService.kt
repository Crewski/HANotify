package com.crewski.hanotify

import android.app.Notification
import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationManager
import android.support.v4.app.NotificationCompat
import android.media.RingtoneManager
import android.app.PendingIntent
import android.content.Context
import android.app.NotificationChannel
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.util.Log
import android.util.Log.d
import android.webkit.URLUtil
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.*


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // ...
        // Check if message contains a data payload.


        var title = ""
        var message = ""
        var color = ""
        var tag = (353..37930).random()
        var actions: JSONArray = JSONArray()
        var image = ""
        var icon = ""

        val dataJSON = JSONObject(remoteMessage!!.data);

        Log.d("Message", dataJSON.toString())

        if (dataJSON.has("title")) {
            title = dataJSON.getString("title")// handler
        }
        if (dataJSON.has("body")) {
            message = dataJSON.getString("body")// handler
        }
        if (dataJSON.has("color")) {
            color = dataJSON.getString("color")// handler
        }
        if (dataJSON.has("actions")) {
            actions = JSONArray(dataJSON.getString("actions"))// handler
        }
        if (dataJSON.has("tag")) {
            try {
                tag = dataJSON.getInt("tag")
            } catch (e: JSONException) {
                // Oops
            }

            if (dataJSON.has("dismiss")) {
                try {
                    if (dataJSON.getBoolean("dismiss") == true) {
                        val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(tag)
                        return
                    }
                } catch (e: JSONException) {
                    // Oops
                }

            }
        }
        if (dataJSON.has("image")) {
            try {
                image = dataJSON.getString("image")
            } catch (e: JSONException) {
                Log.d("Exception", e.toString())
            }
        }

        if (dataJSON.has("icon")) {
            try {
                icon = dataJSON.getString("icon")
            } catch (e: JSONException) {
                Log.d("Exception", e.toString())
            }
        }


        sendNotification(message, title, color, actions, tag, image, icon)

    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(messageBody: String, title: String, color: String, actions: JSONArray, tag: Int, image_url: String, icon_url: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

//        val notification_id = (353..37930).random()
        val channelId = "HomeAssistant"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT < 26) {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
                    .setContentTitle(title)
                    .setContentText(messageBody)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setColor(Color.parseColor(color))
            if (URLUtil.isValidUrl(image_url)) {
                val image = getBitmapFromURL(image_url)
                if (image != null) {
                    notificationBuilder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(image).bigLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.blank_icon)))
                    notificationBuilder.setLargeIcon(image)
                }
            }
            for (i in 0 until 3) {
                try {
                    val iObject = actions.getJSONObject(i)
                    val ititle = iObject.getString("title")
                    val action = iObject.getString("action")
                    val broadcastIntent = Intent(this, ResponseReceiver::class.java)
                    broadcastIntent.putExtra("id", tag)
                    broadcastIntent.putExtra("action", action)
                    val actionIntent = PendingIntent.getBroadcast(this, (353..37930).random(), broadcastIntent, 0)
                    notificationBuilder.addAction(R.drawable.notification_icon, ititle, actionIntent)

                } catch (e: JSONException) {
                    Log.d("Exception", e.toString())
                }
            }
            notificationManager.notify(tag /* ID of notification */, notificationBuilder.build())

        } else {
            val notificationBuilder = Notification.Builder(this, channelId)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setStyle(Notification.BigTextStyle().bigText(messageBody))
                    .setContentTitle(title)
                    .setContentText(messageBody)
                    .setAutoCancel(true)
                    .setColor(Color.parseColor(color))
            if (URLUtil.isValidUrl(image_url)) {
                val image = getBitmapFromURL(image_url)
                if (image != null) {
                    notificationBuilder.setStyle(Notification.BigPictureStyle().bigPicture(image).bigLargeIcon(Icon.createWithResource(this, R.drawable.blank_icon)))
                    notificationBuilder.setLargeIcon(image)
                }
            }
            if (URLUtil.isValidUrl(icon_url)) {
                val icon = getBitmapFromURL(icon_url)
                if (icon != null) {
                    notificationBuilder.setSmallIcon(Icon.createWithBitmap(icon))
                }
            }
            for (i in 0 until 3) {
                try {
                    val iObject = actions.getJSONObject(i)
                    val ititle = iObject.getString("title")
                    val action = iObject.getString("action")
                    val broadcastIntent = Intent(this, ResponseReceiver::class.java)
                    broadcastIntent.putExtra("id", tag)
                    broadcastIntent.putExtra("action", action)
                    val actionIntent = PendingIntent.getBroadcast(this, (353..37930).random(), broadcastIntent, 0)
                    val notificationAction = Notification.Action.Builder(Icon.createWithResource(this, R.drawable.notification_icon), ititle, actionIntent).build()
                    notificationBuilder.addAction(notificationAction)

                } catch (e: JSONException) {
                    Log.d("Exception", e.toString())
                }
            }

            notificationManager.notify(tag /* ID of notification */, notificationBuilder.build())
        }

    }

    fun ClosedRange<Int>.random() =
            Random().nextInt((endInclusive + 1) - start) + start

    fun getBitmapFromURL(image_url: String): Bitmap? {
        try {
            val url = URL(image_url)
            val connection = url.openConnection()
            connection.doInput = true
            connection.connect()
            val input = connection.getInputStream()
            return BitmapFactory.decodeStream(input);
        } catch (e: IOException) {
            return null
        }
    }
}


