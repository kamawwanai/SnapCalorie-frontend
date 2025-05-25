package com.example.snapcalorie.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.snapcalorie.ui.theme.Base40
import com.example.snapcalorie.ui.theme.Base90
import com.example.snapcalorie.ui.theme.Orange50
import com.example.snapcalorie.ui.theme.Green50
import com.example.snapcalorie.ui.theme.montserratFamily

@Composable
fun OnboardingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    validate: ((String) -> Boolean)? = null
) {
    var isTouched by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var currentErrorMessage by remember { mutableStateOf<String?>(null) }

    // Валидация при изменении значения
    LaunchedEffect(value) {
        if (isTouched && validate != null) {
            hasError = !validate(value)
            currentErrorMessage = if (hasError) errorMessage else null
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = { newValue -> 
            if (!isTouched) isTouched = true
            onValueChange(newValue)
        },
        label = { 
            Text(
                text = label,
                fontFamily = montserratFamily
            )
        },
        placeholder = { 
            Text(
                text = placeholder,
                fontFamily = montserratFamily,
                color = Base40
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        isError = hasError && isTouched,
        supportingText = if (hasError && isTouched && currentErrorMessage != null) {
            { 
                Text(
                    text = currentErrorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = montserratFamily
                )
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Base90,
            unfocusedTextColor = Base90,
            focusedBorderColor = if (hasError && isTouched) MaterialTheme.colorScheme.error else Green50,
            unfocusedBorderColor = if (hasError && isTouched) MaterialTheme.colorScheme.error else Base40,
            focusedLabelColor = if (hasError && isTouched) MaterialTheme.colorScheme.error else Green50,
            unfocusedLabelColor = if (hasError && isTouched) MaterialTheme.colorScheme.error else Base40,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorLabelColor = MaterialTheme.colorScheme.error,
            errorSupportingTextColor = MaterialTheme.colorScheme.error,
            cursorColor = if (hasError && isTouched) MaterialTheme.colorScheme.error else Green50,
            errorCursorColor = MaterialTheme.colorScheme.error,
            focusedPlaceholderColor = Base40,
            unfocusedPlaceholderColor = Base40,
            errorContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    )
} 