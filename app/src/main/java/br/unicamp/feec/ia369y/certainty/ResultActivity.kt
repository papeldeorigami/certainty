package br.unicamp.feec.ia369y.certainty

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;

import kotlinx.android.synthetic.main.activity_result.*
import kotlinx.android.synthetic.main.content_result.*

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        setSupportActionBar(toolbar)

        val b = intent.extras
        var level = 0.0
        var label = ""
        if (b != null) {
            label = b.getString("label")
            level = b.getDouble("level")
        }
        labelTextView.text = label
        levelTextView.text = "%.2f".format(level)
    }

}
