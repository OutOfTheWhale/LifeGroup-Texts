package com.lifegrouptext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lifegrouptext.ui.nav.LifeGroupRoot
import com.lifegrouptext.ui.theme.LifeGroupTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifeGroupTheme {
                LifeGroupRoot()
            }
        }
    }
}
