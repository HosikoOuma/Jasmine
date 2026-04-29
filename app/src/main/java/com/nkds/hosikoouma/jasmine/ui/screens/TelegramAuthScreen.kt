package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.R
import com.nkds.hosikoouma.jasmine.datamodels.Screen
import com.nkds.hosikoouma.jasmine.viewmodels.TelegramViewModel
import org.drinkless.tdlib.TdApi

@Composable
fun TelegramAuthScreen(
    navController: NavController,
    viewModel: TelegramViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        when (val state = authState.state) {
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                PhoneNumberInput(
                    isLoading = authState.isLoading,
                    error = authState.error,
                    onSend = viewModel::sendPhoneNumber
                )
            }
            is TdApi.AuthorizationStateWaitCode -> {
                CodeInput(
                    isLoading = authState.isLoading,
                    error = authState.error,
                    onSend = viewModel::sendCode
                )
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                PasswordInput(
                    isLoading = authState.isLoading,
                    error = authState.error,
                    onSend = viewModel::sendPassword
                )
            }
            is TdApi.AuthorizationStateReady -> {
                LoggedInView(
                    onFindChannels = { navController.navigate(Screen.TelegramCloud.route) },
                    onLogout = viewModel::logout
                )
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun PhoneNumberInput(
    isLoading: Boolean,
    error: String?,
    onSend: (String) -> Unit
) {
    var phone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Cloud,
            null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.tg_welcome),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.tg_welcome_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(stringResource(R.string.tg_phone_label)) },
            placeholder = { Text(stringResource(R.string.tg_phone_hint)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            isError = error != null
        )

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onSend(phone) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading && phone.isNotBlank(),
            shape = MaterialTheme.shapes.large
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.tg_send_code))
            }
        }

        Spacer(Modifier.height(140.dp)) // Отступ от нижних панелей
    }
}

@Composable
fun CodeInput(
    isLoading: Boolean,
    error: String?,
    onSend: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.tg_enter_code),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.tg_code_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text(stringResource(R.string.tg_code_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = error != null
        )

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onSend(code) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading && code.isNotBlank(),
            shape = MaterialTheme.shapes.large
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.verify))
            }
        }

        Spacer(Modifier.height(140.dp)) // Отступ от нижних панелей
    }
}

@Composable
fun PasswordInput(
    isLoading: Boolean,
    error: String?,
    onSend: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.tg_2step_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.tg_2step_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.tg_password_label)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = error != null
        )

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onSend(password) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading && password.isNotBlank(),
            shape = MaterialTheme.shapes.large
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.login))
            }
        }

        Spacer(Modifier.height(140.dp)) // Отступ от нижних панелей
    }
}

@Composable
fun LoggedInView(
    onFindChannels: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(32.dp))

        Icon(
            Icons.Rounded.CheckCircle,
            null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.tg_connected_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.tg_connected_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onFindChannels,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Rounded.Search, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.tg_find_channels))
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.logout))
        }

        Spacer(Modifier.height(160.dp)) // Отступ от нижних панелей (миниплеер + бар)
    }
}
