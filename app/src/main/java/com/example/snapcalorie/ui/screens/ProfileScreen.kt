package com.example.snapcalorie.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.layout.ContentScale
import com.example.snapcalorie.R
import com.example.snapcalorie.ui.components.NavBar
import com.example.snapcalorie.ui.components.Screen
import com.example.snapcalorie.ui.theme.*
import com.example.snapcalorie.ui.theme.montserratFamily
import com.example.snapcalorie.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onNavigateToScreen: (Screen) -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val userData by viewModel.userData.collectAsState()
    val planData by viewModel.planData.collectAsState()
    val profileData by viewModel.profileData.collectAsState()
    var currentScreen by remember { mutableStateOf(Screen.PROFILE) }

    LaunchedEffect(Unit) {
        viewModel.loadUserData()
        viewModel.loadPlanData()
        viewModel.loadProfileData()
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Выход",
                    style = TextStyle(
                        fontFamily = montserratFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        lineHeight = 20.sp
                    )
                )
            },
            text = {
                Text(
                    text = "Вы уверены, что хотите выйти?",
                    style = TextStyle(
                        fontFamily = montserratFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = onLogout) {
                    Text(
                        text = "Да",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(
                        text = "Нет",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Base0)
    ) {
        // TopBar
        Row(
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "Профиль",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    color = Base90
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showLogoutDialog = true }, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Выйти",
                    tint = Red50,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Information Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // User Info Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Container 1 - Avatar and Username
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_profile_icon),
                                contentDescription = "Profile Icon",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(
                            text = profileData?.username ?: "",
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 18.sp,
                                lineHeight = 22.sp,
                                color = Base90
                            )
                        )
                    }

                    // Container 2 - Email and Registration Date
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Email Container
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "E-mail",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = Base40
                                )
                            )
                            Text(
                                text = userData?.email ?: "",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = Base90
                                )
                            )
                        }
                        // Registration Date Container
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Дата регистрации",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = Base40
                                )
                            )
                            Text(
                                text = userData?.formattedRegistrationDate ?: "",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = Base90
                                )
                            )
                        }
                    }

                    // Container 3 - Age and Gender
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Age Container
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Возраст",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = Base40
                                )
                            )
                            Text(
                                text = "${profileData?.age ?: ""} лет",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = Base90
                                )
                            )
                        }
                        // Gender Container
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Пол",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = Base40
                                )
                            )
                            Text(
                                text = if (profileData?.gender == 1) "Мужской" else "Женский",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = Base90
                                )
                            )
                        }
                    }

                    // Container 4 - Height and Weight
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Height Container
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Рост",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = Base40
                                )
                            )
                            Text(
                                text = "${profileData?.height ?: ""} см",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = Base90
                                )
                            )
                        }
                        // Weight Container
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Вес",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = Base40
                                )
                            )
                            Text(
                                text = "${profileData?.weight ?: ""} кг",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = Base90
                                )
                            )
                        }
                    }

                    // Container 5 - Activity Level
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Уровень моей активности",
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = Base40
                            )
                        )
                        Text(
                            text = when (profileData?.activity_level) {
                                1 -> "Сидячий образ жизни"
                                2 -> "Легкая активность"
                                3 -> "Умеренная активность"
                                4 -> "Высокая активность"
                                5 -> "Очень высокая активность"
                                else -> ""
                            },
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = Base90
                            )
                        )
                    }
                }

                // Goal Info Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Моя цель",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            color = Green50
                        )
                    )
                    Text(
                        text = planData?.smart_goal ?: "",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = Base90
                        )
                    )
                }

                // Plan Info Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "План по достижению цели",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            color = Green50
                        )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Целевая калорийность: ${planData?.calories_per_day ?: ""} ккал/день",
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = Base90
                            )
                        )
                        Text(
                            text = "Белки: ${planData?.protein_g ?: ""} г/день",
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = Base90
                            )
                        )
                        Text(
                            text = "Жиры: ${planData?.fat_g ?: ""} г/день",
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = Base90
                            )
                        )
                        Text(
                            text = "Углеводы: ${planData?.carb_g ?: ""} г/день",
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = Base90
                            )
                        )
                    }
                }
            }

            // Buttons Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* TODO: Implement plan adjustment */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green50,
                        contentColor = Base0
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Скорректировать план",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    )
                }
                
                Button(
                    onClick = { /* TODO: Implement password change */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Base0,
                        contentColor = Green50
                    ),
                    border = ButtonDefaults.outlinedButtonBorder,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Изменить пароль",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }

        // NavBar
        NavBar(
            currentScreen = currentScreen,
            onScreenSelected = onNavigateToScreen
        )
    }
} 