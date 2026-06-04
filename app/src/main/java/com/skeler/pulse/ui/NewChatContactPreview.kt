package com.skeler.pulse.ui
import android.content.res.Configuration
import android.telephony.PhoneNumberUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skeler.pulse.R
import com.skeler.pulse.contact.normalizeAddressForDisplay
import com.skeler.pulse.design.theme.SerafinaAppTheme
import com.skeler.pulse.design.theme.SerafinaPalette
import com.skeler.pulse.design.theme.SerafinaThemeMode
import com.skeler.pulse.design.theme.SerafinaThemeState


@Preview(
    name = "New Chat Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
internal fun NewChatContactSelectionScreenDarkPreview() {
    SerafinaAppTheme(
        darkTheme = true,
        themeState = SerafinaThemeState(
            dynamicColorEnabled = false,
            selectedPalette = SerafinaPalette.LavenderVolt,
            themeMode = SerafinaThemeMode.Dark,
        ),
    ) {
        NewChatContactSelectionScreen(
            contactGroups = mockContactGroups(),
            loading = false,
            searchQuery = "",
            simOptions = listOf(
                NewChatSimOption(
                    key = "sim_1",
                    subscriptionId = 1,
                    slotLabel = "SIM 1",
                    carrierLabel = "inwi",
                ),
                NewChatSimOption(
                    key = "sim_2",
                    subscriptionId = 2,
                    slotLabel = "SIM 2",
                    carrierLabel = "Orange",
                ),
            ),
            selectedSimKey = "sim_1",
            onContactClick = {},
            onBackClick = {},
            onSearchQueryChange = {},
        )
    }
}

@Preview(
    name = "New Chat Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
internal fun NewChatContactSelectionScreenLightPreview() {
    SerafinaAppTheme(
        darkTheme = false,
        themeState = SerafinaThemeState(
            dynamicColorEnabled = false,
            selectedPalette = SerafinaPalette.LavenderVolt,
            themeMode = SerafinaThemeMode.Light,
        ),
    ) {
        NewChatContactSelectionScreen(
            contactGroups = mockContactGroups(),
            loading = false,
            searchQuery = "",
            simOptions = listOf(
                NewChatSimOption(
                    key = "sim_1",
                    subscriptionId = 1,
                    slotLabel = "SIM 1",
                    carrierLabel = "inwi",
                ),
                NewChatSimOption(
                    key = "sim_2",
                    subscriptionId = 2,
                    slotLabel = "SIM 2",
                    carrierLabel = "Orange",
                ),
            ),
            selectedSimKey = "sim_1",
            onContactClick = {},
            onBackClick = {},
            onSearchQueryChange = {},
        )
    }
}
