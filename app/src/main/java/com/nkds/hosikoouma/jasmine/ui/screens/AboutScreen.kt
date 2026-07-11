package com.nkds.hosikoouma.jasmine.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nkds.hosikoouma.jasmine.R

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    
    var clickCount by remember { mutableIntStateOf(0) }
    val showEasterEgg = clickCount >= 5

    val version = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }
        } catch (e: Exception) {
            null
        }
    } ?: stringResource(R.string.unknown)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // App Logo with Easter Egg logic
        Surface(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (clickCount < 5) clickCount++
                },
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
//            Box(contentAlignment = Alignment.Center) {
//                Image(
//                    painter = painterResource(id = if (showEasterEgg) R.drawable.jasmine1 else R.drawable.ison_vec),
//                    contentDescription = stringResource(R.string.app_name),
//                    modifier = Modifier.size(if (showEasterEgg) 120.dp else 80.dp),
//                    contentScale = if (showEasterEgg) ContentScale.Crop else ContentScale.Fit
//                )
//            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = stringResource(R.string.version_template, version),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Developer Section
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.Center,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Image(
//                painter = painterResource(id = R.drawable.neko1),
//                contentDescription = null,
//                modifier = Modifier
//                    .size(100.dp)
//                    .clip(RoundedCornerShape(24.dp)),
//                contentScale = ContentScale.Crop
//            )
//            Spacer(modifier = Modifier.width(16.dp))
//            Image(
//                painter = painterResource(id = R.drawable.neko2),
//                contentDescription = null,
//                modifier = Modifier
//                    .size(100.dp)
//                    .clip(RoundedCornerShape(24.dp)),
//                contentScale = ContentScale.Crop
//            )
            //}

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.developer_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { uriHandler.openUri("https://t.me/NekoDosi") }
        ) {
            Text(
                text = stringResource(R.string.developer_handle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = stringResource(R.string.made_with_heart),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(140.dp))
    }
}
