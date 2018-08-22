package com.crewski.hanotify

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.webkit.URLUtil
import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sharedPref = getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE)
        val url = sharedPref.getString(getString(R.string.home_assistant_url), "")
        ha_url.setText(url)
        val api_password = sharedPref.getString(getString(R.string.api_password), "")
        ha_passwrod.setText(api_password)

    register_btn.setOnClickListener{
        var url = ha_url.text.toString()
        val password = ha_passwrod.text.toString()

        if (!URLUtil.isValidUrl(url)){
            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }
        url = url.removeSuffix("/")

        with (sharedPref.edit()) {
            putString(getString(R.string.home_assistant_url), url)
            putString(getString(R.string.api_password), password)
            commit()
        }
        val token = FirebaseInstanceId.getInstance().token

        val dataJSON = JSONObject()
        dataJSON.put("token", token)
        val que = Volley.newRequestQueue(this)
        val url_suffix = getString(R.string.url_suffix)
        val req = object : JsonObjectRequest(Request.Method.POST, "$url$url_suffix", dataJSON,
                Response.Listener { response ->
                    val resJSON = JSONObject(response.toString())
                    Toast.makeText(this, resJSON.getString("message"), Toast.LENGTH_SHORT).show()

                }, Response.ErrorListener { error ->  Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()

        }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Content-Type", "application/json");
                headers.put("x-ha-access", password)
                return headers
            }
        }

        que.add(req)
    }


    }



    fun btnConnectHandler(){

    }
}
