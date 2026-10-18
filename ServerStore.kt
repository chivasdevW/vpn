package app.ezyvpn

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Server(val name: String, val config: String)

/** Sunucuları cihazda saklar (SharedPreferences). */
class ServerStore(context: Context) {

    private val prefs = context.getSharedPreferences("ezyvpn", Context.MODE_PRIVATE)

    fun load(): List<Server> {
        val raw = prefs.getString("servers", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Server(o.getString("name"), o.getString("config"))
        }
    }

    fun save(servers: List<Server>) {
        val arr = JSONArray()
        servers.forEach {
            arr.put(JSONObject().put("name", it.name).put("config", it.config))
        }
        prefs.edit().putString("servers", arr.toString()).apply()
    }
}
