package priv.kit.ui.adb

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import priv.kit.ui.state.PrivilegeUiViewModelStore

@Suppress("DEPRECATION")
internal fun Context.isPrivilegeUiWifiConnected(): Boolean =
    runCatching {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
            ?: return@runCatching false
        connectivityManager.allNetworks.any { network ->
            connectivityManager.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }.getOrDefault(false)

internal fun PrivilegeUiViewModelStore.isWifiConnected(): Boolean =
    requireContext().isPrivilegeUiWifiConnected()
