// MainActivity.kt (rename from NewMainActivity.kt)
package com.mana.SyncMart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mana.SyncMart.ui.SyncMartApp
import com.mana.SyncMart.ui.theme.SyncMartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SyncMartTheme {
                SyncMartApp()
            }
        }
    }
}