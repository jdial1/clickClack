package com.ampersand.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ampersand.model.BrickColor
import com.ampersand.model.ChatMessage
import com.ampersand.model.CoupleState
import com.ampersand.model.Reaction
import com.ampersand.ui.brick.glass
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.brick.structureSlug
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.AmpField
import com.ampersand.ui.components.IconAction
import com.ampersand.ui.components.UiIcon
import com.ampersand.ui.components.drawable
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play

/**
 * A conversation between the two of you: lines and emotes, as many as you like.
 * [seed] shows anything said before conversations existed (a line back, a mood reply).
 */
@Composable
fun ChatScreen(
    state: CoupleState,
    title: String,
    subtitle: String?,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSend: (text: String?, emote: Reaction?) -> Unit,
    modifier: Modifier = Modifier,
    seed: List<ChatMessage> = emptyList(),
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val all = seed + messages
    val list = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(all.size) { if (all.isNotEmpty()) list.animateScrollToItem(all.size - 1) }

    fun send(text: String?, emote: Reaction?) {
        sounds.play(Sfx.PLACE)
        feel.play(Feel.PLACE)
        onSend(text, emote)
    }

    Column(modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconAction(UiIcon.BACK, "Back", onClick = onBack)
        }
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxWidth().glass(inkOnBrick(structureSlug()), corner = 12.dp).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, maxLines = 2) }
        }

        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            if (all.isEmpty()) {
                item {
                    Text(
                        "Say anything. It stays between you two.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.glass(inkOnBrick(structureSlug()), corner = 12.dp).padding(12.dp),
                    )
                }
            }
            items(all, key = { it.id }) { m -> Bubble(state, m) }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // Emotes send straight away; text needs Send.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Reaction.entries.forEach { r ->
                Image(
                    painterResource(r.drawable()),
                    contentDescription = r.label,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { send(null, r) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AmpField(
                value = draft,
                onValueChange = { draft = it.take(500) },
                color = BrickColor.of(state.you.colorArgb),
                placeholder = "Write back",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        AmpButton(label = "Send", slug = null, enabled = draft.isNotBlank(), icon = UiIcon.NEXT) {
            send(draft.trim(), null)
            draft = ""
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun Bubble(state: CoupleState, m: ChatMessage) {
    val mine = m.byUid == state.you.uid
    val who = if (mine) state.you else state.them
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier
                .widthIn(max = 280.dp)
                .glass(inkOnBrick(structureSlug()), corner = 14.dp, strength = 1.4f)
                .border(2.dp, Color(who.colorArgb), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        ) {
            m.emote?.let {
                Image(painterResource(it.drawable()), contentDescription = it.label, modifier = Modifier.size(48.dp))
            }
            m.text?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground) }
        }
    }
}
