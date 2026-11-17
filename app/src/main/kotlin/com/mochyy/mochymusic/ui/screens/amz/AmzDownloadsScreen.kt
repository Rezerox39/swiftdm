package com.mochyy.mochymusic.ui.screens.amz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.ui.component.TopToaster
import com.mochyy.mochymusic.ui.component.revealOnAppear
import com.mochyy.mochymusic.ui.component.rememberRevealSeenState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmzDownloadsScreen(
    navController: NavController,
    viewModel: AmzDownloadsViewModel = hiltViewModel(),
) {
    val url by viewModel.url.collectAsStateWithLifecycle()
    val segments by viewModel.segments.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val revealSeen = rememberRevealSeenState()

    LaunchedEffect(message) {
        if (message != null) {
            TopToaster.error(message.orEmpty())
            viewModel.clearMessage()
        }
    }

    SubSettingsScaffold(
        title = stringResource(R.string.armz_downloads_title),
        onBack = { navController.navigateUp() },
        backLabel = stringResource(R.string.settings),
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = viewModel::setUrl,
            label = { Text(stringResource(R.string.armz_downloads_url)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.armz_downloads_segments),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            listOf(1, 4, 8, 16, 32).forEach { count ->
                FilterChip(
                    selected = segments == count,
                    onClick = { viewModel.setSegments(count) },
                    label = { Text(count.toString()) },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        SettingsGroup(
            items = listOf(
                SettingsItem(
                    title = { Text(stringResource(R.string.armz_downloads_add)) },
                    onClick = viewModel::addDownload,
                ),
            ),
        )

        Spacer(Modifier.height(18.dp))

        if (downloads.isEmpty()) {
            Text(
                text = stringResource(R.string.armz_downloads_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            downloads.forEachIndexed { index, row ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .revealOnAppear(
                            key = "armz-download-${row.id}",
                            seenState = revealSeen,
                            delayMillis = index * 20,
                            cascade = true,
                        ),
                ) {
                    SettingsGroup(
                        items = listOf(
                            SettingsItem(
                                title = {
                                    Text(
                                        text = row.fileName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                description = {
                                    Text(
                                        text = "${row.status} · ${
                                            stringResource(
                                                R.string.armz_downloads_speed,
                                                row.bytesPerSecond,
                                            )
                                        }",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    if (row.errorMessage != null) {
                                        Text(
                                            text = row.errorMessage.orEmpty(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { row.fraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                    )
                                },
                                trailingContent = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Row {
                                            TextButton(onClick = { viewModel.pause(row.id) }) {
                                                Text(
                                                    text = stringResource(R.string.armz_downloads_pause),
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                            TextButton(onClick = { viewModel.resume(row.id) }) {
                                                Text(
                                                    text = stringResource(R.string.armz_downloads_resume),
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                        }
                                        Row {
                                            TextButton(onClick = { viewModel.cancel(row.id) }) {
                                                Text(
                                                    text = stringResource(R.string.armz_downloads_cancel),
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                            TextButton(onClick = { viewModel.remove(row.id) }) {
                                                Text(
                                                    text = stringResource(R.string.armz_downloads_remove),
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                        }
                                    }
                                },
                            ),
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(140.dp))
    }
}
