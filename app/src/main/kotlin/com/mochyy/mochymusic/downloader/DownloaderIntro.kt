@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mochyy.mochymusic.downloader

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.ProfileCardNumberKey
import com.mochyy.mochymusic.constants.ProfileJoinedEpochDayKey
import com.mochyy.mochymusic.constants.ProfilePicUriKey
import com.mochyy.mochymusic.constants.UsernameKey
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

private enum class IntroPhase { Splash, Greeting, Setup }

/** First launch: a logo splash, a greeting, then a card the person can put their name and photo on. */
@Composable
fun DownloaderIntroScreen(onDone: () -> Unit) {
    var phase by remember { mutableStateOf(IntroPhase.Splash) }

    val (username, setUsername) = rememberPreference(UsernameKey, "")
    val (photo, setPhoto) = rememberPreference(ProfilePicUriKey, "")
    val (cardNumber, setCardNumber) = rememberPreference(ProfileCardNumberKey, "")
    val (joinedEpochDay, setJoinedEpochDay) = rememberPreference(ProfileJoinedEpochDayKey, -1L)

    val generatedNumber = remember {
        (10_000_000_000L + Random.nextLong(89_999_999_999L)).toString()
    }
    var name by remember { mutableStateOf(username) }
    var photoUri by remember { mutableStateOf(photo) }
    val context = LocalContext.current

    LaunchedEffect(username) {
        if (name.isBlank()) name = username
    }
    LaunchedEffect(photo) {
        if (photoUri.isBlank()) photoUri = photo
    }
    LaunchedEffect(cardNumber, joinedEpochDay, generatedNumber) {
        if (cardNumber.isBlank()) setCardNumber(generatedNumber)
        if (joinedEpochDay < 0L) setJoinedEpochDay(LocalDate.now().toEpochDay())
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        photoUri = uri.toString()
        setPhoto(photoUri)
    }

    val joinedText = if (joinedEpochDay >= 0L) {
        LocalDate.ofEpochDay(joinedEpochDay).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    } else {
        "—"
    }

    fun finish() {
        if (name.isNotBlank()) setUsername(name.trim())
        if (photoUri.isNotBlank()) setPhoto(photoUri)
        if (cardNumber.isBlank()) setCardNumber(generatedNumber)
        if (joinedEpochDay < 0L) setJoinedEpochDay(LocalDate.now().toEpochDay())
        onDone()
    }

    AnimatedContent(
        targetState = phase,
        transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(200)) },
        label = "AMz intro",
    ) { current ->
        when (current) {
            IntroPhase.Splash -> DownloaderSplash(onNext = { phase = IntroPhase.Greeting })

            IntroPhase.Greeting -> DownloaderGreeting(
                name = username,
                onNext = { phase = IntroPhase.Setup },
            )

            IntroPhase.Setup -> DownloaderSetup(
                name = name,
                onNameChange = { name = it },
                photoUri = photoUri,
                cardNumber = cardNumber.ifBlank { generatedNumber },
                joinedText = joinedText,
                onPickPhoto = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onFinish = { finish() },
                onSkip = onDone,
            )
        }
    }
}

@Composable
private fun DownloaderSplash(onNext: () -> Unit) {
    var ready by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (ready) 1f else 0.55f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 260f,
        ),
        label = "splash scale",
    )
    val rotation by animateFloatAsState(
        targetValue = if (ready) 0f else -28f,
        animationSpec = tween(1_000, easing = FastOutSlowInEasing),
        label = "splash rotation",
    )
    val halo by animateFloatAsState(
        targetValue = if (ready) 1f else 0.2f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "splash halo",
    )

    LaunchedEffect(Unit) {
        ready = true
        delay(1_450)
        onNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .graphicsLayer {
                    alpha = halo * 0.16f
                    scaleX = halo
                    scaleY = halo
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Image(
            painter = painterResource(R.drawable.ic_splash_mark),
            contentDescription = "AMz Downloader",
            modifier = Modifier
                .size(112.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                },
        )
        Column(
            modifier = Modifier.offset(y = 104.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "AMz Downloader",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Mochy motion · AMz engine",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun DownloaderGreeting(name: String, onNext: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(620),
        label = "greeting alpha",
    )
    val offsetY by animateDpAsState(
        targetValue = if (shown) 0.dp else 28.dp,
        animationSpec = tween(620, easing = FastOutSlowInEasing),
        label = "greeting slide",
    )

    LaunchedEffect(Unit) { shown = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = 150.dp, y = (-120).dp)
                .graphicsLayer { this.alpha = 0.12f }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-120).dp, y = 420.dp)
                .graphicsLayer { this.alpha = 0.08f }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .offset(y = offsetY)
                .graphicsLayer { this.alpha = alpha },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_splash_mark),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (name.isBlank()) "Hey there." else "Hey, ${name.trim()}.",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Your downloads, your look. Let’s set up the card and make this feel like yours.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(36.dp))
            Button(onClick = onNext) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Make it mine", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun DownloaderSetup(
    name: String,
    onNameChange: (String) -> Unit,
    photoUri: String,
    cardNumber: String,
    joinedText: String,
    onPickPhoto: () -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Make it yours",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "This sits on your profile card. You can change it later in Settings.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )

        DownloaderAnimatedCard(
            username = name.ifBlank { "AMz user" },
            photo = photoUri.takeIf { it.isNotBlank() },
            cardNumber = cardNumber,
            joinedText = joinedText,
            enableTilt = true,
            showEditHint = true,
            onPhotoClick = onPickPhoto,
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Your name") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            },
            shape = RoundedCornerShape(16.dp),
        )

        Button(
            onClick = onPickPhoto,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(if (photoUri.isBlank()) "Add profile picture" else "Change profile picture")
        }

        Text(
            text = "Tip: tap the card to flip it, drag to tilt it, double tap to extend it.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Start downloading")
        }
        TextButton(
            onClick = onSkip,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("Skip for now")
        }
    }
}
