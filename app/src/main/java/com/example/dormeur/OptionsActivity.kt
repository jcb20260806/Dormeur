package com.example.dormeur

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class OptionsActivity : AppCompatActivity() {

    private lateinit var btnRetour: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_options)

        btnRetour = findViewById(R.id.btnRetour)

        btnRetour.setOnClickListener {
            finish()
        }
    }
}

