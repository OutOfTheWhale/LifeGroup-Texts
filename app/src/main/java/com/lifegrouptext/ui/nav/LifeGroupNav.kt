package com.lifegrouptext.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifegrouptext.ui.log.LogScreen
import com.lifegrouptext.ui.message.MessageScreen
import com.lifegrouptext.ui.people.ImportContactsScreen
import com.lifegrouptext.ui.people.PeopleScreen
import com.lifegrouptext.ui.send.SendScreen
import com.lifegrouptext.ui.theme.Hairline
import com.lifegrouptext.ui.theme.Ink
import com.lifegrouptext.ui.theme.Slate

private object Routes {
    const val SEND = "send"
    const val PEOPLE = "people"
    const val PEOPLE_IMPORT = "people_import"
    const val MESSAGE = "message"
    const val LOG = "log"
}

/** Top-level tabs. Text-only labels keep the chrome minimal and on-brand. */
enum class Dest(val route: String, val label: String) {
    Send(Routes.SEND, "Send"),
    People(Routes.PEOPLE, "People"),
    Message(Routes.MESSAGE, "Message"),
    Log(Routes.LOG, "Log"),
}

private fun Dest.owns(route: String?): Boolean = when (this) {
    Dest.Send -> route == Routes.SEND
    Dest.People -> route != null && route.startsWith("people")
    Dest.Message -> route == Routes.MESSAGE
    Dest.Log -> route == Routes.LOG
}

@Composable
fun LifeGroupRoot() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            LightBottomBar(
                selected = { dest -> dest.owns(currentRoute) },
                onSelect = { dest ->
                    // Tapping a tab pops any detail screens and lands on the tab's root.
                    navController.navigate(dest.route) {
                        popUpTo(Routes.SEND) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SEND,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SEND) {
                SendScreen(onEditMessage = { navController.navigate(Routes.MESSAGE) })
            }
            composable(Routes.PEOPLE) {
                PeopleScreen(onImport = { navController.navigate(Routes.PEOPLE_IMPORT) })
            }
            composable(Routes.PEOPLE_IMPORT) {
                ImportContactsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.MESSAGE) { MessageScreen() }
            composable(Routes.LOG) { LogScreen() }
        }
    }
}

@Composable
private fun LightBottomBar(
    selected: (Dest) -> Boolean,
    onSelect: (Dest) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawLine(
                    color = Hairline,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f,
                )
            }
            .height(60.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dest.entries.forEach { dest ->
            val isSelected = selected(dest)
            Text(
                text = dest.label,
                textAlign = TextAlign.Center,
                color = if (isSelected) Ink else Slate,
                style = if (isSelected) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                modifier = Modifier
                    .clickable(onClick = { onSelect(dest) })
                    .padding(horizontal = 10.dp, vertical = 12.dp),
            )
        }
    }
}
