package com.example.snapcalorie.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.snapcalorie.ui.theme.*

@Composable
fun OnboardingScreen(
    currentPage: Int,
    title: String,
    description: String,
    image: Int,
    showSkipButton: Boolean = true,
    showStartButton: Boolean = false,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Background image
        Image(
            painter = painterResource(id = image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Content container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(344.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Stepper dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(if (currentPage == index) 32.dp else 12.dp)
                                .background(
                                    color = if (currentPage == index) Green50 else Green20,
                                    shape = RoundedCornerShape(20.dp)
                                )
                        )
                    }
                }

                // Text content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Base90,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Base90,
                        textAlign = TextAlign.Center
                    )
                }

                // Buttons
                if (showStartButton) {
                    Button(
                        onClick = onNext,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green50,
                            contentColor = Base0
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 24.dp,
                            vertical = 10.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Начать",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showSkipButton) {
                            TextButton(
                                onClick = onSkip,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Base90
                                ),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text(
                                    text = "Пропустить",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        FilledIconButton(
                            onClick = onNext,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Green50,
                                contentColor = Base0
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Next"
                            )
                        }
                    }
                }
            }
        }
    }
} 