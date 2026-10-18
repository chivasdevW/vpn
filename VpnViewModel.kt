package app.ezyvpn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface VpnStatus {
    data object Disconnected : VpnStatus
    data object Connecting : VpnStatus
    data object Connected : VpnStatus
    data class Failed(val message: String) : VpnStatus
}

class VpnViewModel(app: Application) : AndroidViewModel(app) {

    private val tunnel = TunnelManager(app)
    private val store = ServerStore(app)

    private val _status = MutableStateFlow<VpnStatus>(VpnStatus.Disconnected)
    val status: StateFlow<VpnStatus> = _status.asStateFlow()

    private val _servers = MutableStateFlow(store.load())
    val servers: StateFlow<List<Server>> = _servers.asStateFlow()

    private val _selected = MutableStateFlow(_servers.value.firstOrNull())
    val selected: StateFlow<Server?> = _selected.asStateFlow()

    fun select(server: Server) { _selected.value = server }

    fun addServer(name: String, config: String) {
        val server = Server(name.trim(), config.trim())
        _servers.value = _servers.value + server
        store.save(_servers.value)
        if (_selected.value == null) _selected.value = server
    }

    fun removeServer(server: Server) {
        _servers.value = _servers.value - server
        store.save(_servers.value)
        if (_selected.value == server) _selected.value = _servers.value.firstOrNull()
    }

    fun connect() {
        val server = _selected.value ?: return
        _status.value = VpnStatus.Connecting
        viewModelScope.launch {
            _status.value = try {
                tunnel.connect(server.config)
                VpnStatus.Connected
            } catch (e: Exception) {
                VpnStatus.Failed(e.message ?: "Bağlantı kurulamadı")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            runCatching { tunnel.disconnect() }
            _status.value = VpnStatus.Disconnected
        }
    }
}
