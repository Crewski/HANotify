package com.crewski.hanotify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.app.NotificationManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import com.android.volley.AuthFailureError
import com.android.volley.toolbox.Volley
import com.google.firebase.iid.FirebaseInstanceId


class ResponseReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        val id = intent?.getIntExtra("id", 0)
        val action = intent?.getStringExtra("action")

        val notificationManager: NotificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id!!)

        val dataJSON = JSONObject()
        dataJSON.put("action", action)
        dataJSON.put("type", "clicked")
        dataJSON.put("token", FirebaseInstanceId.getInstance().token)


        val sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file), Context.MODE_PRIVATE)
        val url = sharedPref.getString(context.getString(R.string.home_assistant_url), "")
        val url_suffix = context.getString(R.string.url_suffix);
        val url_callback = context.getString(R.string.url_callback)
        val api_password = sharedPref.getString(context.getString(R.string.api_password), "")

        val que = Volley.newRequestQueue(context)
        val req = object : JsonObjectRequest(Request.Method.POST, "$url$url_suffix$url_callback", dataJSON,
                Response.Listener {response ->  Toast.makeText(context, response.toString(), Toast.LENGTH_SHORT).show()

                }, Response.ErrorListener {error ->  Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
            
        }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Content-Type", "application/json");
                headers.put("x-ha-access", api_password)
                return headers
            }
        }

        que.add(req)



    }




}


