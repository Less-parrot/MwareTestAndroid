package net.christianbeier.droidvnc_ng.sockets

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import net.christianbeier.droidvnc_ng.sockets.configSockets.CommandsRemoteService
import net.christianbeier.droidvnc_ng.sockets.configSockets.GetIpDevice


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, CommandsRemoteService::class.java)
        ContextCompat.startForegroundService(this, intent)

        setContent {
            //NavHostController()
            val getIpDevice = GetIpDevice()

            Box(modifier = Modifier.fillMaxSize(), Alignment.Center){
                Column {
                    Text(text = getIpDevice.toString())
                    //Text(text = "$marcaDispositivo \n $idDispositivo")
                }
            }
        }
    }
}