package com.mochyy.mochymusic.downloader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.mochyy.mochymusic.constants.ProfileCardNumberKey
import com.mochyy.mochymusic.constants.ProfileJoinedEpochDayKey
import com.mochyy.mochymusic.constants.ProfilePicUriKey
import com.mochyy.mochymusic.constants.UsernameKey
import com.mochyy.mochymusic.utils.rememberPreference
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/** The profile card survives the pivot, now with its Mochy gyro tilt, flip and extend motion. */
@Composable
fun DownloaderUserCard(modifier: Modifier = Modifier) {
    val (username) = rememberPreference(UsernameKey, "")
    val (photo) = rememberPreference(ProfilePicUriKey, "")
    val (cardNumber, setCardNumber) = rememberPreference(ProfileCardNumberKey, "")
    val (joinedEpochDay, setJoinedEpochDay) = rememberPreference(ProfileJoinedEpochDayKey, -1L)
    val (cardTilt) = rememberPreference(DownloaderPrefs.CardTiltKey, true)

    LaunchedEffect(cardNumber, joinedEpochDay) {
        if (cardNumber.isBlank()) {
            setCardNumber((10_000_000_000L + Random.nextLong(89_999_999_999L)).toString())
        }
        if (joinedEpochDay < 0L) setJoinedEpochDay(LocalDate.now().toEpochDay())
    }

    val joinedText = if (joinedEpochDay >= 0L) {
        LocalDate.ofEpochDay(joinedEpochDay).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    } else {
        "—"
    }

    DownloaderAnimatedCard(
        username = username.ifBlank { "AMz user" },
        photo = photo.takeIf { it.isNotBlank() },
        cardNumber = cardNumber.ifBlank { "00000000000" },
        joinedText = joinedText,
        modifier = modifier,
        enableTilt = cardTilt,
        showEditHint = true,
    )
}
