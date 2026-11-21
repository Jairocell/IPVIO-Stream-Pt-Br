package com.ipvio.stream

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AddPlaylistScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    val canCancel = PlaylistStore.lists.isNotEmpty() || PlaylistStore.channels.isNotEmpty()
    BackHandler(enabled = canCancel) {
        PlaylistStore.clearPending(ctx)
        onDone()
    }

    if (PlaylistStore.loading) {
        PlaylistLoadingOverlay()
        return
    }

    val startFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { startFocus.requestFocus() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(lerp(Bg, Accent1, 0.16f), Bg), radius = 1400f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .clip(RoundedCornerShape(DS.RadiusXl))
                .background(Surf)
                .border(2.dp, Surf2, RoundedCornerShape(DS.RadiusXl))
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BrandMark(Modifier.size(44.dp))
                Column {
                    Text("IPVIO STREAM", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, raw = true)
                    Text("Acesso Multi-Servidor", color = Accent2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, raw = true)
                }
            }

            Text(
                "Digite seu usuário e senha para carregar automaticamente os 4 servidores (OLYMPUS, HORIZON, OBA W2 e OCTANE).",
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                raw = true
            )

            Field(
                value = user,
                onChange = { user = it },
                hint = "Usuário",
                inner = Modifier.focusRequester(startFocus)
            )

            Field(
                value = pass,
                onChange = { pass = it },
                hint = "Senha",
                password = !showPass,
                onDone = {
                    if (!PlaylistStore.loading) {
                        scope.launch {
                            if (PlaylistStore.loginMultiServer(ctx, user, pass)) onDone()
                        }
                    }
                }
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FocusButton(
                    text = if (showPass) "Ocultar senha" else "Mostrar senha",
                    compact = true
                ) { showPass = !showPass }

                FocusButton(
                    text = if (PlaylistStore.loading) "Conectando…" else "Entrar",
                    primary = true,
                    compact = false
                ) {
                    if (PlaylistStore.loading) return@FocusButton
                    scope.launch {
                        if (PlaylistStore.loginMultiServer(ctx, user, pass)) onDone()
                    }
                }
            }

            PlaylistStore.error?.let {
                Text(it, color = DS.Danger, fontSize = 14.sp, lineHeight = 18.sp, raw = true)
            }

            if (canCancel) {
                FocusButton("Voltar", compact = true) {
                    PlaylistStore.clearPending(ctx)
                    onDone()
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
val InstantSpec = object : androidx.compose.foundation.gestures.BringIntoViewSpec {
    override val scrollAnimationSpec: androidx.compose.animation.core.AnimationSpec<Float> = androidx.compose.animation.core.snap()
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val peek = 40f
        val leading = offset
        val trailing = offset + size
        return when {
            leading >= 0f && trailing <= containerSize -> 0f
            leading < 0f && trailing > containerSize -> 0f
            leading < 0f -> leading - peek
            else -> trailing - containerSize + peek
        }
    }
}

internal const val BACKUP_HINT = "Se o servidor principal falhar, o aplicativo alterna automaticamente para um servidor de backup."
internal const val BACKUP_INVALID = "Servidor de backup inválido."
internal fun backupsValid(b: List<String>): Boolean = b.all { it.isBlank() || HostFailoverLogic.normalize(it) != null }
internal fun backupList(saved: List<String>): androidx.compose.runtime.snapshots.SnapshotStateList<String> =
    mutableStateListOf<String>().apply { addAll(saved); if (isEmpty()) add("") }

@Composable
internal fun BackupFields(list: androidx.compose.runtime.snapshots.SnapshotStateList<String>) {
    for (i in list.indices) {
        Field(list[i], { list[i] = it }, "Backup ${i + 1} (opcional)")
    }
}

@Composable
internal fun HostHealthLine(addr: String) {}
