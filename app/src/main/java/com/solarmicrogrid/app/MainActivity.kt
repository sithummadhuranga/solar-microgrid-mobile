package com.solarmicrogrid.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

// launcher screen, becomes the login screen once M-2 is built
class MainActivity : AppCompatActivity() {

    // loads the layout and opens the map from the temporary button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // temporary entry point, moves to the prosumer dashboard later (member 4)
        findViewById<Button>(R.id.open_map_button).setOnClickListener {
            startActivity(Intent(this, StationMapActivity::class.java))
        }
    }
}
