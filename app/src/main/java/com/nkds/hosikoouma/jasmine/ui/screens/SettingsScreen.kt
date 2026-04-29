package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.R
import com.nkds.hosikoouma.jasmine.datamodels.Screen

@Composable
fun SettingsScreen(
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsCategoryItem(
            title = stringResource(R.string.playback),
            subtitle = stringResource(R.string.settings_playback_subtitle),
            icon = Icons.Rounded.PlayCircle,
            onClick = { navController.navigate(Screen.SettingsPlayback.route) }
        )

        SettingsCategoryItem(
            title = stringResource(R.string.appearance),
            subtitle = stringResource(R.string.settings_appearance_subtitle),
            icon = Icons.Rounded.Palette,
            onClick = { navController.navigate(Screen.SettingsAppearance.route) }
        )

        SettingsCategoryItem(
            title = stringResource(R.string.library_settings),
            subtitle = stringResource(R.string.settings_library_subtitle),
            icon = Icons.Rounded.LibraryMusic,
            onClick = { navController.navigate(Screen.SettingsLibrary.route) }
        )
        
        SettingsCategoryItem(
            title = stringResource(R.string.telegram_cloud),
            subtitle = stringResource(R.string.settings_telegram_subtitle),
            icon = Icons.Rounded.Cloud,
            onClick = { navController.navigate(Screen.SettingsTelegram.route) }
        )

        SettingsCategoryItem(
            title = stringResource(R.string.maintenance),
            subtitle = stringResource(R.string.settings_maintenance_subtitle),
            icon = Icons.Rounded.Build,
            onClick = { navController.navigate(Screen.SettingsMaintenance.route) }
        )

        SettingsCategoryItem(
            title = stringResource(R.string.about_jasmine),
            subtitle = stringResource(R.string.settings_about_subtitle),
            icon = Icons.Rounded.Info,
            onClick = { navController.navigate(Screen.About.route) }
        )

        Spacer(modifier = Modifier.height(160.dp))
    }
}

@Composable
private fun SettingsCategoryItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    ) {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
            supportingContent = { Text(subtitle) },
            leadingContent = { 
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                ) 
            },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}
