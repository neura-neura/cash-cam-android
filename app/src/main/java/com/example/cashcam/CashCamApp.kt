package com.example.cashcam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun CashCamApp(model: CashCamViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var denied by rememberSaveable { mutableStateOf(false) }
    val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permission = it; denied = !it
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        model.load()
    }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); model.message(null) }
    }
    BackHandler(state.selected.isNotEmpty() || state.gallery) {
        if (state.selected.isNotEmpty()) model.clearSelection() else model.gallery(false)
    }
    CashCamContent(state, snackbar, permission, denied,
        onPermission = { requestCamera.launch(Manifest.permission.CAMERA) },
        onSettings = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) },
        onGallery = model::gallery, onEdit = model::edit, onCaptured = model::captured,
        onRetake = model::retake, onCreate = model::create, onSelect = model::select,
        onSelectAll = model::selectAll, onClear = model::clearSelection, onSave = model::export,
        onShare = { cards ->
            try { context.startActivity(Intent.createChooser(model.repository.shareIntent(cards), "Compartir fichas")) }
            catch (_: android.content.ActivityNotFoundException) { model.message("No hay una app disponible para compartir imágenes") }
        }, onError = { model.message(it) })
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CashCamContent(
    state: CashCamState, snackbar: SnackbarHostState, cameraPermission: Boolean, denied: Boolean,
    onPermission: () -> Unit, onSettings: () -> Unit, onGallery: (Boolean) -> Unit,
    onEdit: (ProductDraft) -> Unit, onCaptured: (File) -> Unit, onRetake: () -> Unit,
    onCreate: () -> Unit, onSelect: (String) -> Unit, onSelectAll: () -> Unit, onClear: () -> Unit,
    onSave: (List<ProductCard>) -> Unit, onShare: (List<ProductCard>) -> Unit, onError: (String) -> Unit
) {
    val selection = state.cards.filter { it.id in state.selected }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(if (selection.isNotEmpty()) "${selection.size} ${if (selection.size == 1) "seleccionada" else "seleccionadas"}" else "CashCam", style = MaterialTheme.typography.headlineMedium)
                    if (selection.isEmpty()) Text(if (state.gallery) "Tus productos, listos para compartir" else "De foto a ficha en segundos", style = MaterialTheme.typography.bodySmall)
                }
            }, actions = {
                if (state.gallery && state.cards.isNotEmpty()) {
                    IconButton(onClick = onSelectAll, enabled = !state.busy) { Icon(Icons.Rounded.SelectAll, "Seleccionar todas") }
                    if (selection.isNotEmpty()) IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, "Quitar selección") }
                }
            })
        },
        bottomBar = {
            Column {
                if (selection.isNotEmpty()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = { onSave(selection) }, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.SaveAlt, null); Spacer(Modifier.width(8.dp)); Text("Guardar")
                            }
                            Button(onClick = { onShare(selection) }, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Share, null); Spacer(Modifier.width(8.dp)); Text("Compartir")
                            }
                        }
                    }
                }
                NavigationBar {
                    NavigationBarItem(selected = !state.gallery, onClick = { onGallery(false) }, enabled = !state.busy,
                        icon = { Icon(Icons.Rounded.PhotoCamera, null) }, label = { Text("Crear") })
                    NavigationBarItem(selected = state.gallery, onClick = { onGallery(true) }, enabled = !state.busy,
                        icon = { Icon(Icons.Rounded.GridView, null) }, label = { Text("Mis fichas") })
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            if (state.gallery) GalleryContent(state.cards, state.selected, state.loaded, !state.busy, onSelect, onGallery, onSave, onShare)
            else CaptureContent(state, cameraPermission, denied, onPermission, onSettings, onEdit, onCaptured, onRetake, onCreate, onError)
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter).semantics { contentDescription = "Procesando fichas" })
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CaptureContent(
    state: CashCamState, permission: Boolean, denied: Boolean, onPermission: () -> Unit,
    onSettings: () -> Unit, onEdit: (ProductDraft) -> Unit, onCaptured: (File) -> Unit,
    onRetake: () -> Unit, onCreate: () -> Unit, onError: (String) -> Unit
) {
    val draft = state.draft
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val previewHeight = minOf(240.dp, maxHeight * .35f)
        val previewWidth = maxWidth * .55f
        Column(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    val layout = cardLayout(draft.ratio)
                    LiveCardPreview(state.photo, draft, Modifier.widthIn(max = previewWidth).heightIn(max = previewHeight)
                        .aspectRatio(layout.aspectRatio, matchHeightConstraintsFirst = true).clip(RoundedCornerShape(12.dp)))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Vista previa", style = MaterialTheme.typography.titleMedium)
                        Text("Se actualiza al escribir", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                item {
                    Column(Modifier.widthIn(max = 600.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("1. Fotografía tu producto", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("16:9" to 16f / 9f, "1:1" to 1f, "9:16" to 9f / 16f).forEach { (label, ratio) ->
                                FilterChip(selected = draft.ratio == ratio, onClick = { onEdit(draft.copy(ratio = ratio)) }, label = { Text(label) }, enabled = !state.busy && state.photo == null)
                            }
                        }
                        if (state.photo != null) {
                            OutlinedButton(onClick = onRetake, enabled = !state.busy) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Volver a tomar") }
                        } else if (permission) CameraPanel(draft.ratio, !state.busy, onCaptured, onError)
                        else {
                            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp)) {
                                Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(40.dp))
                                    Text("Dale luz a tu producto", style = MaterialTheme.typography.headlineSmall)
                                    Text("CashCam necesita la cámara para tomar fotos. Tus imágenes se quedan en el dispositivo.")
                                    Button(onClick = onPermission) { Text("Permitir cámara") }
                                    if (denied) TextButton(onClick = onSettings) { Text("Abrir permisos en Ajustes") }
                                }
                            }
                        }
                    }
                }
                item {
                    Column(Modifier.widthIn(max = 600.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("2. Añade los detalles", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(draft.title, { onEdit(draft.copy(title = it.take(100))) }, label = { Text("Título del producto") }, singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(draft.price, { onEdit(draft.copy(price = it.take(24))) }, label = { Text("Precio") }, prefix = { Text("$") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                        Text("Estado", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Nuevo", "Usado", "Oferta", "").forEach { status ->
                                FilterChip(draft.status == status, { onEdit(draft.copy(status = status)) }, label = { Text(status.ifBlank { "Sin estado" }) }, enabled = !state.busy)
                            }
                        }
                        OutlinedTextField(draft.brand, { onEdit(draft.copy(brand = it.take(60))) }, label = { Text("Texto de la esquina (opcional)") },
                            supportingText = { Text("Se recuerda para todas las nuevas fichas") }, singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(draft.subtitle, { onEdit(draft.copy(subtitle = it.take(100))) }, label = { Text("Subtítulo (opcional)") },
                            supportingText = { Text("Se recuerda para todas las nuevas fichas") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(draft.properties, { onEdit(draft.copy(properties = it.take(180))) }, label = { Text("Propiedades (opcional)") }, placeholder = { Text("Talla: M · Color: verde") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                        Button(onClick = onCreate, enabled = state.photo != null && draft.title.isNotBlank() && draft.price.isNotBlank() && !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shapes = ButtonDefaults.shapes()) {
                            Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Crear ficha")
                        }
                        val layout = cardLayout(draft.ratio)
                        Text("PNG de ${layout.width} × ${layout.height}. Después podrás guardarlo o compartirlo.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GalleryContent(
    cards: List<ProductCard>, selected: Set<String>, loaded: Boolean, enabled: Boolean,
    onSelect: (String) -> Unit, onGallery: (Boolean) -> Unit,
    onSave: (List<ProductCard>) -> Unit, onShare: (List<ProductCard>) -> Unit
) {
    var openedId by rememberSaveable { mutableStateOf<String?>(null) }
    val opened = cards.find { it.id == openedId }
    if (cards.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Collections, null, Modifier.size(64.dp))
            Spacer(Modifier.height(20.dp))
            Text(if (loaded) "Tu galería empieza aquí" else "Cargando fichas…", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text("Crea tu primera ficha para guardar y compartir tus productos.")
            Spacer(Modifier.height(24.dp))
            Button(onClick = { onGallery(false) }, enabled = enabled) { Text("Crear una ficha") }
        }
    } else {
        LazyVerticalGrid(columns = GridCells.Adaptive(140.dp), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("${cards.size} " + (if (cards.size == 1) "ficha" else "fichas") + " · Mantén pulsada una para seleccionar varias", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(cards, key = { it.id }, contentType = { "card" }) { card ->
                val checked = card.id in selected
                Card(shape = RoundedCornerShape(24.dp), border = if (checked) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.semantics { this.selected = checked; contentDescription = card.title }
                        .combinedClickable(enabled = enabled, onClick = { if (selected.isEmpty()) openedId = card.id else onSelect(card.id) }, onLongClick = { onSelect(card.id) })) {
                    Box {
                        AsyncImage(card.thumbnail, null, Modifier.fillMaxWidth().aspectRatio(card.aspectRatio), contentScale = ContentScale.Fit)
                        if (checked) Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                            Icon(Icons.Rounded.Check, "Seleccionada", Modifier.padding(6.dp).size(20.dp))
                        }
                    }
                    Text(card.title, Modifier.padding(12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
    if (opened != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { openedId = null }) {
            Surface(shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(opened.title, style = MaterialTheme.typography.titleLarge)
                    Box(Modifier.fillMaxWidth().weight(1f, fill = false), contentAlignment = Alignment.Center) {
                        AsyncImage(opened.file, "Ficha de ${opened.title}",
                            Modifier.aspectRatio(opened.aspectRatio, matchHeightConstraintsFirst = true), contentScale = ContentScale.Fit)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { onSave(listOf(opened)) }, enabled = enabled, modifier = Modifier.weight(1f)) { Text("Guardar") }
                        Button(onClick = { onShare(listOf(opened)) }, enabled = enabled, modifier = Modifier.weight(1f)) { Text("Compartir") }
                    }
                    Row {
                        TextButton(onClick = { onSelect(opened.id); openedId = null }, enabled = enabled) { Text("Seleccionar") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { openedId = null }) { Text("Cerrar") }
                    }
                }
            }
        }
    }
}
