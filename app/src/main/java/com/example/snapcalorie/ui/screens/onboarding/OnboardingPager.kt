package com.example.snapcalorie.ui.screens.onboarding

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPager(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp,
            userScrollEnabled = false // Отключаем стандартное листание
        ) { page ->
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        // Вычисляем прогресс анимации для текущей страницы
                        val pageOffset = (
                            (pagerState.currentPage - page) + pagerState
                                .currentPageOffsetFraction
                        ).absoluteValue

                        // Анимация исчезновения
                        alpha = 1f - pageOffset.coerceIn(0f, 1f)
                        
                        // Анимация сдвига
                        translationX = pageOffset * size.width * 0.5f
                    }
                    .pointerInput(Unit) {
                        var dragStart = Offset.Zero
                        var dragAmount = Offset.Zero
                        val velocityTracker = VelocityTracker()

                        detectHorizontalDragGestures(
                            onDragStart = { dragStart = it },
                            onDragEnd = {
                                val velocity = velocityTracker.calculateVelocity().x
                                when {
                                    dragAmount.x > 50 && page > 0 -> {
                                        // Свайп вправо (назад)
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(page - 1)
                                        }
                                    }
                                }
                                dragAmount = Offset.Zero
                            },
                            onDragCancel = { dragAmount = Offset.Zero },
                            onHorizontalDrag = { change, dragAmountX ->
                                change.consume()
                                dragAmount = Offset(dragAmountX, 0f)
                                velocityTracker.addPosition(
                                    change.uptimeMillis,
                                    change.position
                                )
                            }
                        )
                    }
            ) {
                OnboardingScreen(
                    currentPage = page,
                    title = onboardingPages[page].title,
                    description = onboardingPages[page].description,
                    image = onboardingPages[page].image,
                    showSkipButton = page < onboardingPages.size - 1,
                    showStartButton = page == onboardingPages.size - 1,
                    onSkip = {
                        Log.d("OnboardingPager", "Skip button clicked")
                        onFinish()
                    },
                    onNext = {
                        if (page < onboardingPages.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    page = page + 1,
                                    animationSpec = tween(
                                        durationMillis = 500,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        } else {
                            Log.d("OnboardingPager", "Onboarding completed")
                            onFinish()
                        }
                    }
                )
            }
        }
    }
} 