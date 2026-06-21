package com.example.workman.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes network connectivity changes and exposes them as a Flow.
 * Usage: collect `observeConnectivity(context)` in your composable/viewmodel.
 */
object ConnectivityObserver {

    enum class Status {
        Available, Unavailable, Losing, Lost
    }

    fun observe(context: Context): Flow<Status> = callbackFlow {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(Status.Available)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                trySend(Status.Losing)
            }

            override fun onLost(network: Network) {
                trySend(Status.Lost)
            }

            override fun onUnavailable() {
                trySend(Status.Unavailable)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emit initial state
        val currentStatus = getCurrentStatus(connectivityManager)
        trySend(currentStatus)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * Check current connectivity status synchronously.
     */
    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return getCurrentStatus(cm) == Status.Available
    }

    private fun getCurrentStatus(cm: ConnectivityManager): Status {
        val network = cm.activeNetwork ?: return Status.Unavailable
        val capabilities = cm.getNetworkCapabilities(network) ?: return Status.Unavailable
        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            Status.Available
        } else {
            Status.Unavailable
        }
    }
}

