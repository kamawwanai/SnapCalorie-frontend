package com.example.snapcalorie.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snapcalorie.ui.components.NavBar
import com.example.snapcalorie.ui.components.Screen
import com.example.snapcalorie.ui.components.TopBar
import com.example.snapcalorie.ui.theme.*
import com.example.snapcalorie.ui.viewmodel.AuthViewModel

@Composable
fun MainScreen(
    viewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.PROFILE) }
    val user by viewModel.user.observeAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Base0
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar(title = "SnapCalorie")
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (currentScreen) {
                    Screen.PROFILE -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Профиль",
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = montserratFamily,
                                color = Base90
                            )
                            
                            Text(
                                text = user?.email ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = montserratFamily,
                                color = Base90
                            )
                            
                            Button(
                                onClick = {
                                    viewModel.logout()
                                    onLogout()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Green50,
                                    contentColor = Base0
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "Выйти",
                                    fontFamily = montserratFamily,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    else -> {
                        // Other screens will be implemented later
                        Text(
                            text = "В разработке",
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = montserratFamily,
                            color = Base90
                        )
                    }
                }
            }
            
            NavBar(
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    currentScreen = screen
                }
            )
        }
    }
} 