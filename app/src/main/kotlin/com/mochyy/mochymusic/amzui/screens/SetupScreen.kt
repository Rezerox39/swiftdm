package com.mochyy.mochymusic.amzui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.amzui.components.Avatar
import com.mochyy.mochymusic.amzui.components.IdCard
import com.mochyy.mochymusic.amzui.components.PrimaryButton
import com.mochyy.mochymusic.amzui.data.AmzPrefs
import com.mochyy.mochymusic.amzui.theme.AmzColors
import com.mochyy.mochymusic.amzui.theme.AmzDimens
import com.mochyy.mochymusic.amzui.theme.AmzShapes
import com.mochyy.mochymusic.amzui.theme.AmzType
import com.mochyy.mochymusic.amzui.theme.amzEnter
import com.mochyy.mochymusic.utils.rememberPreference

@Composable
fun SetupScreen(onFinished: () -> Unit) {
    val onboardingState = rememberPreference(AmzPrefs.OnboardingDone, false)
    val nameState = rememberPreference(AmzPrefs.ProfileName, "")
    val photoState = rememberPreference(AmzPrefs.ProfilePhotoUri, "")
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) photoState.value = uri.toString() }
    val step = if (nameState.value.isBlank()) 1 else 2

    AmzScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AmzDimens.screenX),
        ) {
            Text(
                text = "Step $step of 2",
                style = AmzType.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .amzEnter(kind = com.mochyy.mochymusic.amzui.theme.AmzEnterKind.Drop),
            )
            Spacer(Modifier.height(AmzDimens.setupTitleTop))
            Text(
                text = "Make it yours",
                style = AmzType.onboardingTitle,
                modifier = Modifier.amzEnter(),
            )
            Spacer(Modifier.height(AmzDimens.childGap))
            Text(
                text = "Your downloads, your profile.",
                style = AmzType.secondary,
                textAlign = TextAlign.Start,
            )
            Spacer(Modifier.height(24.dp))
            IdCard(
                name = nameState.value.ifBlank { "A" },
                photoUri = photoState.value.takeIf { it.isNotBlank() },
                interactiveAvatar = true,
                onAvatarClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.amzEnter(),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Your name",
                style = AmzType.statLabel,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AmzDimens.setupLabelTop))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AmzDimens.fieldHeight)
                    .clip(AmzShapes.field)
                    .background(AmzColors.Surface2)
                    .border(1.dp, AmzColors.White10, AmzShapes.field)
                    .padding(horizontal = AmzDimens.statPadX),
            ) {
                BasicTextField(
                    value = nameState.value,
                    onValueChange = { nameState.value = it },
                    singleLine = true,
                    textStyle = AmzType.body,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (nameState.value.isEmpty()) {
                            Text("Enter your name", style = AmzType.secondary)
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.height(AmzDimens.setupPictureLabelTop))
            Text(
                text = "Add a profile photo",
                style = AmzType.statLabel,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AmzDimens.smallGap))
            Avatar(
                name = nameState.value.ifBlank { "A" },
                photoUri = photoState.value.takeIf { it.isNotBlank() },
                size = AmzDimens.avatarTile,
                interactive = true,
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                label = "Start Downloading",
                onClick = {
                    onboardingState.value = true
                    onFinished()
                },
                enabled = nameState.value.isNotBlank(),
            )
            Spacer(Modifier.height(AmzDimens.screenX))
        }
    }
}
