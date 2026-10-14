package com.mobiledumper7.gui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Public repo — opened when the GitHub mark in the top bar is tapped. */
private const val REPO_URL = "https://github.com/alenhimself/UEDumper-Android"

private val DumperDarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF4DD0E1),
    secondary = androidx.compose.ui.graphics.Color(0xFF80CBC4),
    tertiary = androidx.compose.ui.graphics.Color(0xFFB2FF59),
    surface = androidx.compose.ui.graphics.Color(0xFF1A1D21),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF24292F),
    background = androidx.compose.ui.graphics.Color(0xFF121417),
)

data class InstalledApp(val label: String, val packageName: String)

class DumperViewModel(app: Application) : AndroidViewModel(app) {
    var rootOk by mutableStateOf<Boolean?>(null)
    var binaryOk by mutableStateOf(false)
    var running by mutableStateOf(false)
    var resultZip by mutableStateOf<String?>(null)
    var resultOk by mutableStateOf<Boolean?>(null)
    val logLines = mutableStateListOf<String>()
    var installedApps by mutableStateOf<List<InstalledApp>>(emptyList())
    var appsLoading by mutableStateOf(false)

    fun appendLog(line: String) {
        logLines.add(line)
        if (logLines.size > 2000) logLines.removeRange(0, logLines.size - 2000)
    }

    fun checkRoot() {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = DumperRunner.isRootAvailable()
            withContext(Dispatchers.Main) { rootOk = ok }
            if (ok) {
                val bin = DumperRunner.ensureBinary(getApplication()) { appendLog(it) }
                withContext(Dispatchers.Main) { binaryOk = bin != null }
            }
        }
    }

    fun loadInstalledApps() {
        if (appsLoading) return
        appsLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val apps = try {
                val infos: List<ApplicationInfo> =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getInstalledApplications(
                            PackageManager.ApplicationInfoFlags.of(0),
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getInstalledApplications(0)
                    }
                infos.mapNotNull { info ->
                    val pkg = info.packageName ?: return@mapNotNull null
                    // Skip obvious system packages, keep everything launchable-looking.
                    if ((info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                        pm.getLaunchIntentForPackage(pkg) == null
                    ) {
                        return@mapNotNull null
                    }
                    InstalledApp(
                        label = pm.getApplicationLabel(info)?.toString() ?: pkg,
                        packageName = pkg,
                    )
                }.sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))
            } catch (_: Exception) {
                emptyList()
            }
            withContext(Dispatchers.Main) {
                installedApps = apps
                appsLoading = false
            }
        }
    }

    fun runDump(request: DumpRequest) {
        if (running) return
        running = true
        resultZip = null
        resultOk = null
        viewModelScope.launch(Dispatchers.IO) {
            val res = DumperRunner.runDump(getApplication(), request) { appendLog(it) }
            withContext(Dispatchers.Main) {
                running = false
                resultZip = res.zipPath
                resultOk = res.success
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = DumperDarkColors) {
                DumperScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DumperScreen(vm: DumperViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedProfile by remember { mutableStateOf(GameProfiles.Auto) }
    var packageName by remember { mutableStateOf("") }
    var memType by remember { mutableStateOf(1) }
    var suspendGame by remember { mutableStateOf(true) }
    var dumpSo by remember { mutableStateOf(false) }
    var appMenuExpanded by remember { mutableStateOf(false) }

    val guessed = GameProfiles.guessProfile(packageName)

    LaunchedEffect(Unit) {
        vm.checkRoot()
        vm.loadInstalledApps()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("UEDumper-Android", fontWeight = FontWeight.Bold)
                        Text(
                            "UE dumper for Android  •  v1.2.1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { openUrl(context, REPO_URL) }) {
                        Icon(
                            painterResource(R.drawable.ic_github),
                            contentDescription = "GitHub repo",
                            tint = androidx.compose.ui.graphics.Color.White,
                        )
                    }
                    RootBadge(rootOk = vm.rootOk)
                    IconButton(onClick = {
                        vm.logLines.clear()
                        vm.checkRoot()
                        vm.loadInstalledApps()
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Target game ────────────────────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("1  •  Target game")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameProfiles.all.forEach { profile ->
                            FilterChip(
                                selected = selectedProfile.id == profile.id,
                                onClick = {
                                    selectedProfile = profile
                                    if (profile.packages.isNotEmpty()) {
                                        packageName = profile.packages.first()
                                    }
                                },
                                label = { Text(profile.label) },
                            )
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = appMenuExpanded,
                        onExpandedChange = { appMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = packageName,
                            onValueChange = { packageName = it },
                            label = { Text("Game package (e.g. com.tencent.ig)") },
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = appMenuExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = appMenuExpanded,
                            onDismissRequest = { appMenuExpanded = false },
                        ) {
                            val query = packageName.trim().lowercase()
                            val candidates = vm.installedApps
                                .filter {
                                    query.isEmpty() ||
                                        it.packageName.lowercase().contains(query) ||
                                        it.label.lowercase().contains(query)
                                }
                                .take(50)
                            if (candidates.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (vm.appsLoading) "Loading apps…"
                                            else "No matches — type package manually",
                                        )
                                    },
                                    onClick = { appMenuExpanded = false },
                                )
                            }
                            candidates.forEach { app ->
                                DropdownMenuItem(
                                    text = { Text("${app.label}\n${app.packageName}") },
                                    onClick = {
                                        packageName = app.packageName
                                        selectedProfile = GameProfiles.guessProfile(app.packageName)
                                        appMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    val activeProfile =
                        if (selectedProfile.id == "auto") guessed else selectedProfile
                    Text(
                        "Native profile: ${activeProfile.label} — ${activeProfile.hint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Options ────────────────────────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("2  •  Options")
                    Text("Memory access", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = memType == 1, onClick = { memType = 1 })
                        Text("process_vm_readv", Modifier.padding(end = 16.dp))
                        RadioButton(selected = memType == 2, onClick = { memType = 2 })
                        Text("pread")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Suspend game while dumping")
                            Text(
                                "Recommended — freezes the game with SIGSTOP.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = suspendGame, onCheckedChange = { suspendGame = it })
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Also dump UE .so module")
                            Text(
                                "Saves libUE4/libUnreal from memory (-d).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = dumpSo, onCheckedChange = { dumpSo = it })
                    }
                    Text(
                        "Outputs: C++ SDK + .usmap + .idmap + .json + GObjects (always on). " +
                            "Result is a .zip next to the SDK folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Run ────────────────────────────────────────────────────
            Button(
                onClick = {
                    vm.runDump(
                        DumpRequest(
                            packageName = packageName,
                            memType = memType,
                            suspendGame = suspendGame,
                            dumpSo = dumpSo,
                        ),
                    )
                },
                enabled = !vm.running && packageName.isNotBlank() && vm.rootOk == true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (vm.running) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(18.dp)
                            .height(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Dumping… (do not close the game)")
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Dump selected game")
                }
            }
            if (vm.rootOk == false) {
                Text(
                    "Root not detected. This app needs KernelSU / Magisk / APatch " +
                        "root to read another app's memory.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // ── Result ─────────────────────────────────────────────────
            vm.resultOk?.let { ok ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (ok) {
                            androidx.compose.ui.graphics.Color(0xFF1E3A24)
                        } else {
                            androidx.compose.ui.graphics.Color(0xFF3A1E1E)
                        },
                    ),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (ok) Icons.Filled.CheckCircle else Icons.Filled.Error,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (ok) "Dump finished" else "Dump failed",
                                fontWeight = FontWeight.Bold,
                            )
                            vm.resultZip?.let {
                                SelectionContainer { Text(it, style = MaterialTheme.typography.bodySmall) }
                            } ?: Text(
                                "See the console below for the reason.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        vm.resultZip?.let { zip ->
                            IconButton(onClick = { copyText(context, zip) }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy path")
                            }
                            IconButton(onClick = { shareZip(context, File(zip)) }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share zip")
                            }
                        }
                    }
                }
            }

            // ── Console ────────────────────────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0B0E11))) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Terminal, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        SectionTitle("Console")
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(onClick = { vm.logLines.clear() }) { Text("Clear") }
                    }
                    Spacer(Modifier.height(8.dp))
                    val listState = rememberLazyListState()
                    LaunchedEffect(vm.logLines.size) {
                        if (vm.logLines.isNotEmpty()) listState.animateScrollToItem(vm.logLines.size - 1)
                    }
                    SelectionContainer {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (vm.logLines.isEmpty()) {
                                item {
                                    Text(
                                        "Logs will appear here.\nBinary: ${DumperRunner.abiDir()} • " +
                                            if (vm.binaryOk) "ready" else "not installed yet",
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = androidx.compose.ui.graphics.Color(0xFF8A949E),
                                    )
                                }
                            }
                            items(vm.logLines) { line ->
                                Text(
                                    line,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color(0xFFC9D4DC),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
    scope.launch { } // keep scope referenced for future actions
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun RootBadge(rootOk: Boolean?) {
    val (label, color) = when (rootOk) {
        true -> "root" to androidx.compose.ui.graphics.Color(0xFF4CAF50)
        false -> "no root" to androidx.compose.ui.graphics.Color(0xFFF44336)
        null -> "…" to androidx.compose.ui.graphics.Color(0xFF9E9E9E)
    }
    Text(
        "● $label",
        color = color,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(end = 4.dp),
    )
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    } catch (_: Exception) {
        copyText(context, url)
    }
}

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("dump-path", text))
}

private fun shareZip(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share dump zip"))
    } catch (_: Exception) {
        copyText(context, file.absolutePath)
    }
}
