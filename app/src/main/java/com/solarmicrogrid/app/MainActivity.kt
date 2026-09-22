package com.solarmicrogrid.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// launcher screen, becomes the login screen once M-2 is built
class MainActivity : AppCompatActivity() {

    // loads the layout for this screen
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
