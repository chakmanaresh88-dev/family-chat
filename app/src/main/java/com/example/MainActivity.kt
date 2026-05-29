package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CallState
import com.example.ui.FamilyViewModel
import com.example.ui.LoginState
import com.example.ui.screens.CallingScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Main surface matching theme background color
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val viewModel: FamilyViewModel = viewModel()
                    val loginState by viewModel.loginState.collectAsState()
                    val partner by viewModel.selectedChatPartner.collectAsState()
                    val callState by viewModel.callState.collectAsState()

                    when {
                        // 1. Auth states routing
                        loginState !is LoginState.Success -> {
                            LoginScreen(viewModel = viewModel)
                        }
                        // 2. Calling HUD state routing
                        callState !is CallState.Idle -> {
                            CallingScreen(viewModel = viewModel)
                        }
                        // 3. Chat Room state routing
                        partner != null -> {
                            ChatScreen(viewModel = viewModel)
                        }
                        // 4. Default Home Dashboard
                        else -> {
                            MainDashboard(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
