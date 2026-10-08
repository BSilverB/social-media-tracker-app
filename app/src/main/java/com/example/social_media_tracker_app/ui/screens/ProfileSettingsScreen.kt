package com.example.social_media_tracker_app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.social_media_tracker_app.data.repository.StatsRepository

@Deprecated("Đã tách riêng thành PetScreen cho Linh vật và SettingsScreen cho Cài đặt")
@Composable
fun ProfileSettingsScreen(
    repository: StatsRepository,
    onOpenWardrobe: () -> Unit = {},
    onOpenDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    SettingsScreen(
        repository = repository,
        onBack = onOpenDashboard,
        modifier = modifier
    )
}
