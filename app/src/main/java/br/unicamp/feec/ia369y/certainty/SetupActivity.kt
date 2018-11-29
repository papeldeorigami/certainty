package br.unicamp.feec.ia369y.certainty

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import kotlinx.android.synthetic.main.activity_setup.*

class SetupActivity : AppCompatActivity() {

    val logTag = "SetupActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //serverEditText.setText(Constants.DEFAULT_BACKEND_URL)
        setContentView(R.layout.activity_setup)
        connectButton.setOnClickListener {
            connectButton.isEnabled = false
            connect()
        }
    }

    fun connect() {
        val server = serverEditText.text.toString()
        Log.w(logTag, server)
        Fuel.get(server + Constants.STATS_ENDPOINT)
            .responseString { request, response, result ->
                if (response.statusCode == 200) {
                    val intent = Intent(this, MainActivity::class.java)

                    val b = Bundle()
                    b.putString("server", server)
                    intent.putExtras(b)

                    startActivity(intent)
                } else {
                    Toast.makeText(this@SetupActivity, "Could not connect to server (" + response.statusCode + ")", Toast.LENGTH_SHORT).show()
                    Log.i(logTag, response.toString())
                    connectButton.isEnabled = true
                }
            }

    }
}
