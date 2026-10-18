package app.ezyvpn

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** WireGuard tünelini açıp kapatan ince katman. */
class TunnelManager(context: Context) {

    private val backend = GoBackend(context.applicationContext)

    private val tunnel = object : Tunnel {
        override fun getName() = "ezyvpn"
        override fun onStateChange(newState: Tunnel.State) {}
    }

    suspend fun connect(configText: String) = withContext(Dispatchers.IO) {
        val config = Config.parse(configText.byteInputStream().bufferedReader())
        backend.setState(tunnel, Tunnel.State.UP, config)
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        backend.setState(tunnel, Tunnel.State.DOWN, null)
    }
}
