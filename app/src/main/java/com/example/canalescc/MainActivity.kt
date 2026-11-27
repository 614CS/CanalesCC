package com.example.canalescc

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { App() }
            }
        }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE) }
    val cache = remember { File(ctx.filesDir, "playlist.m3u") }

    var url by remember { mutableStateOf(prefs.getString("url", "") ?: "") }
    var channels by remember { mutableStateOf(listOf<Channel>()) }
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf<Channel?>(null) }
    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }

    fun load(text: String) {
        channels = M3uParser.parse(text)
        status = "${channels.size} canales"
    }

    LaunchedEffect(Unit) {
        if (cache.exists()) load(cache.readText())
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val t = ctx.contentResolver.openInputStream(uri)!!.bufferedReader().readText()
                    cache.writeText(t)
                    t
                }
                load(text)
            } catch (e: Exception) {
                status = "Error: ${e.message}"
            }
        }
    }

    playing?.let { ch ->
        PlayerScreen(ch) { playing = null }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = url, onValueChange = { url = it },
            label = { Text("URL de lista M3U") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    status = "Descargando…"
                    try {
                        val text = withContext(Dispatchers.IO) {
                            val conn = URL(url.trim()).openConnection().apply {
                                connectTimeout = 15000; readTimeout = 30000
                            }
                            val t = conn.getInputStream().bufferedReader().readText()
                            cache.writeText(t)
                            t
                        }
                        prefs.edit().putString("url", url.trim()).apply()
                        load(text)
                    } catch (e: Exception) {
                        status = "Error: ${e.message}"
                    }
                }
            }) { Text("Cargar URL") }
            Button(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Abrir archivo") }
            Text(status, Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newName, onValueChange = { newName = it },
                label = { Text("Nombre") }, singleLine = true, modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = newUrl, onValueChange = { newUrl = it },
                label = { Text("URL del canal") }, singleLine = true, modifier = Modifier.weight(2f)
            )
            Button(
                onClick = {
                    if (newUrl.isNotBlank()) {
                        val n = newName.ifBlank { newUrl }
                        scope.launch {
                            val text = withContext(Dispatchers.IO) {
                                if (!cache.exists()) cache.writeText("#EXTM3U\n")
                                cache.appendText("\n#EXTINF:-1,$n\n${newUrl.trim()}\n")
                                cache.readText()
                            }
                            load(text)
                            newName = ""; newUrl = ""
                        }
                    }
                },
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
            ) { Text("Añadir") }
        }

        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Buscar canal") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        val shown = remember(channels, query) {
            if (query.isBlank()) channels
            else channels.filter {
                it.name.contains(query, true) || it.group.contains(query, true)
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(shown) { ch ->
                Column(
                    Modifier.fillMaxWidth()
                        .clickable { playing = ch }
                        .padding(vertical = 10.dp, horizontal = 8.dp)
                ) {
                    Text(ch.name, style = MaterialTheme.typography.titleMedium)
                    if (ch.group.isNotEmpty()) {
                        Text(ch.group, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
