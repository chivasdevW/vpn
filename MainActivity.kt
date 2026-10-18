package app.ezyvpn

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wireguard.android.backend.GoBackend

private val Ink = Color(0xFF0F2A43)
private val Deep = Color(0xFF173F63)
private val Sun = Color(0xFFFFC857)
private val Foam = Color(0xFFEAF2F8)
private val Coral = Color(0xFFFF7A6B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Deep, primary = Sun, onPrimary = Ink,
                onBackground = Foam, onSurface = Foam
            )) {
                Surface(color = Ink, modifier = Modifier.fillMaxSize()) { EzyApp() }
            }
        }
    }
}

@Composable
fun EzyApp(vm: VpnViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val status by vm.status.collectAsStateWithLifecycle()
    val servers by vm.servers.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    // Android'in VPN izin penceresi
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { if (it.resultCode == Activity.RESULT_OK) vm.connect() }

    fun toggle() {
        when (status) {
            VpnStatus.Connected -> vm.disconnect()
            VpnStatus.Connecting -> Unit
            else -> {
                val intent = GoBackend.VpnService.prepare(context)
                if (intent != null) permission.launch(intent) else vm.connect()
            }
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ezyvpn", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))

        Spacer(Modifier.weight(1f))
        PowerButton(status, enabled = selected != null, onClick = ::toggle)
        Spacer(Modifier.height(24.dp))

        Text(
            when (status) {
                VpnStatus.Connected -> "Bağlandın"
                VpnStatus.Connecting -> "Bağlanıyor…"
                is VpnStatus.Failed -> "Bağlanamadı"
                VpnStatus.Disconnected -> if (selected == null) "Önce bir sunucu ekle" else "Korunmuyorsun"
            },
            fontSize = 28.sp, fontWeight = FontWeight.Bold
        )
        (status as? VpnStatus.Failed)?.let {
            Spacer(Modifier.height(8.dp))
            Text(it.message, color = Coral, fontSize = 14.sp)
        }
        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Sunucular", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            TextButton(onClick = { showAdd = true }) { Text("Sunucu ekle") }
        }
        LazyColumn(Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(servers) { s ->
                val active = s == selected
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Deep,
                    border = if (active) BorderStroke(2.dp, Sun) else null,
                    modifier = Modifier.fillMaxWidth().clickable(enabled = status != VpnStatus.Connected) { vm.select(s) }
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.name, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.removeServer(s) }, enabled = status != VpnStatus.Connected) { Text("Sil") }
                    }
                }
            }
        }
    }

    if (showAdd) AddServerDialog(
        onDismiss = { showAdd = false },
        onSave = { name, cfg -> vm.addServer(name, cfg); showAdd = false }
    )
}

@Composable
private fun PowerButton(status: VpnStatus, enabled: Boolean, onClick: () -> Unit) {
    val ring by animateColorAsState(
        when (status) {
            VpnStatus.Connected -> Sun
            VpnStatus.Connecting -> Foam
            is VpnStatus.Failed -> Coral
            VpnStatus.Disconnected -> Deep
        }, label = "ring"
    )
    val on = status == VpnStatus.Connected
    Box(
        Modifier.size(200.dp).clip(CircleShape).background(ring.copy(alpha = 0.25f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(150.dp).clip(CircleShape).background(if (on) Sun else Deep),
            contentAlignment = Alignment.Center
        ) {
            Text(if (on) "Kapat" else "Bağlan", color = if (on) Ink else Foam, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddServerDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var config by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sunucu ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Sunucu adı") }, singleLine = true)
                OutlinedTextField(
                    config, { config = it }, label = { Text("WireGuard yapılandırması") },
                    placeholder = { Text("[Interface] … [Peer] …") }, minLines = 6, maxLines = 10
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, config) }, enabled = name.isNotBlank() && config.contains("[Peer]")) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}
