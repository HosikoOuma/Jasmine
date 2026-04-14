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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
            title = "Playback",
            subtitle = "Crossfade, progress bar style",
            icon = Icons.Rounded.PlayCircle,
            onClick = { navController.navigate(Screen.SettingsPlayback.route) }
        )

        SettingsCategoryItem(
            title = "Appearance",
            subtitle = "Theme, AMOLED, fonts, colors",
            icon = Icons.Rounded.Palette,
            onClick = { navController.navigate(Screen.SettingsAppearance.route) }
        )

        SettingsCategoryItem(
            title = "Library",
            subtitle = "Sorting, filtering, blacklisted folders",
            icon = Icons.Rounded.LibraryMusic,
            onClick = { navController.navigate(Screen.SettingsLibrary.route) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        ListItem(
            headlineContent = { Text("Version") },
            supportingContent = { Text("1.0.0 (Jasmine)") },
            leadingContent = { Icon(Icons.Rounded.Info, null) }
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
