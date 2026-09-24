package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SalimBlue
import com.example.ui.theme.SalimRed

@Composable
fun HiddenVaultAuthDialog(
    hasExistingPassword: Boolean,
    correctPassword: String?,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onSetNewPassword: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = SalimBlue,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (hasExistingPassword) "Unlock Hidden Album" else "Set Hidden Album PIN",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (hasExistingPassword) "Enter your PIN to view hidden photos and videos." else "Create a 4-digit PIN to protect your hidden photos and videos.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 8) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("vault_pin_input")
                )

                if (!hasExistingPassword) {
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            if (it.length <= 8) {
                                confirmPin = it
                                errorMessage = null
                            }
                        },
                        label = { Text("Confirm PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("vault_confirm_pin_input")
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = SalimRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hasExistingPassword) {
                        if (pin == correctPassword) {
                            onSuccess()
                        } else {
                            errorMessage = "Incorrect PIN. Please try again."
                        }
                    } else {
                        if (pin.length < 4) {
                            errorMessage = "PIN must be at least 4 digits."
                        } else if (pin != confirmPin) {
                            errorMessage = "PINs do not match."
                        } else {
                            onSetNewPassword(pin)
                            onSuccess()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SalimBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("vault_confirm_button")
            ) {
                Text(if (hasExistingPassword) "Unlock" else "Set PIN")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("vault_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
