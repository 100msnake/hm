package com.hundred_meters.hm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.Surface
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.google.android.gms.nearby.connection.DiscoveryOptions
import java.lang.Exception
import kotlin.text.Charsets.UTF_8


const val TAG = "mfmf"


class MainActivity : ComponentActivity() {

    val context: Context = this
    private var debugging = true

// =================================================================================
// NEARBY CONNECTIONS 2 start
// =================================================================================

    var messageReceived : String = ""
    /** callback for receiving payloads */
    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let {
                messageReceived = String(it, UTF_8)
                //Log.d(TAG, "RECEIVED: " + String(it, UTF_8))
                if (debugging){
                    var blah = Blah(topic = "test", body = messageReceived, randomNumberID = 0)
                    showNotification(blah,priority = NotificationCompat.PRIORITY_MAX )
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {

            if (update.status == PayloadTransferUpdate.Status.SUCCESS
                && messageReceived != "") {
                val oc = messageReceived!!
                Log.d(TAG, "oc: " + oc)
                messageReceived = ""
            }
        }
    }

    // Callbacks for connections to other devices
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Accepting a connection means you want to receive messages. Hence, the API expects
            // that you attach a PayloadCall to the acceptance
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            Log.d(TAG,"endpointName\n(${info.endpointName})")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                //connectionsClient.stopAdvertising()
                //connectionsClient.stopDiscovery()
                opponentEndpointId = endpointId
                //binding.opponentName.text = opponentName
                Log.d(TAG,"Connected")
                //setGameControllerEnabled(true) // we can start playing
                sendGameChoice("from another machine")
            }
        }

        override fun onDisconnected(endpointId: String) {
            //resetGame()
            Log.d(TAG, "disconnected")
        }
    }

    // Callbacks for finding other devices
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            connectionsClient.requestConnection("myCodeName", endpointId, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
        }
    }



// =================================================================================
// NEARBY CONNECTIONS 2 end
// =================================================================================


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MFTheme() {
                //MFTheme( darkTheme = true ) {
                Surface()
                {
                    Screen()
                }
            }
        }

        // --------------------------------------------------------------------------------
        // ------- observe data in viewModel ----------------------------------------------
        // --------------------------------------------------------------------------------
        // note: to show notifications you need to send the blah to MainViewModel first.
        // it should be done that way, i think, to allow for lifecycle changes.

        val mainViewModel: MainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val newMessageObserver = Observer<Blah> { newMessageByUser ->
            showNotification(message = newMessageByUser)
        }

        mainViewModel.newMessageForNotification.observe(this, newMessageObserver)

        // --------------------------------------------------------------------------------
        // ------- observe data in viewModel ------- end ----------------------------------
        // --------------------------------------------------------------------------------

        // string sent by intent from other apps to 100m. eg: the url of a facebook page sent via share
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                        mainViewModel.addToStateBlahBody("\n$it\n")
                    }
                }
            }
        }


        clearNotificationChannels()

        // ask user for location permission on launch
        checkForPermission()

        // ------------------------------------------------
        // nearby connections 2
        connectionsClient = Nearby.getConnectionsClient(this)

        // ------------------------------------------------

        // nearby connections 1 and 2--------------------------------------
        // start nearby connect
        startAdvertising()
        startDiscovery()
        // nearby connections 1 and 2--------------------------------------

        debug()
    }

    // ------------------------------------------------------------------------
    // debug
    // ------------------------------------------------------------------------

    fun debug() {
        if (debugging) {
            Log.d(TAG, "debugging on: private var debugging = true\n")
        }

    }
    // ------------------------------------------------------------------------
    // NOTIFICATION
    // ------------------------------------------------------------------------


    private fun clearNotificationChannels() {

        // deleting these channels would be better done when the user closes the program
        // but there doesn't seem to ba an appropriate lifecycle event.
        // so, a record remains in the notification channels of what topics the user used
        // until the next start. not sure if that matters or not.

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelList: List<NotificationChannel> = notificationManager.notificationChannels
        for (channel in channelList) {
            destroyNotificationChannel(channel.id)
        }

    }


    // just a convenience Blah.
    private val safetyBlah: Blah = Blah(
        topic = "100m",
        body = "100m",
        randomNumberID = 0
    )


    fun showFoundNotifications(blahArray: Array<Blah>) {
        if (debugging) {
            for (m in blahArray) {
                toastLong("notification: \n" + m.toString())
            }
        }
        val mainViewModel: MainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        mainViewModel.showNotificationArray(blahArray)
    }


    private fun showNotification(
        message: Blah = safetyBlah,
        priority: Int = NotificationCompat.PRIORITY_LOW,
    ) {

        // showNotification() should be called via mainViewModel for some lifecycle awareness (i assume)
        // mainViewModel changes the value of newMessageForNotification which is an observed variable

        if (blahOK(message)) {

            // is stateBlah correct here? should it be message
            //val CHANNEL_ID = mainViewModel.stateBlah.topic
            val CHANNEL_ID = message.topic


            // is stateBlah correct here? should it be message
            //val group = mainViewModel.stateBlah.topic
            // maybe "100m"
            val group = message.topic

            // low importance for defult notification channel.
            // descretion seems the right choice. user can upgrade channel if they want
            createNotificationChannel(
                blah = message, //mainViewModel.stateBlah,
                importance1to5 = NotificationManager.IMPORTANCE_LOW
            )

            // content of notification
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_circle_vector)
                .setContentTitle(message.topic)
                .setContentText(message.body)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message.body)
                        .setBigContentTitle(message.topic)
                )
                .setPriority(priority)
                .setGroup(group)
            //.setAutoCancel(true)
            //.setContentIntent(pendingIntent)
            //.addAction(R.drawable.ic_snake_right, getString(R.string.start), startPendingIntent)
            //.addAction(R.drawable.ic_snake_right, getString(R.string.copy), editPendingIntent)


            // show the notification
            with(NotificationManagerCompat.from(this)) {
                // notificationId is a unique int for each notification that you must define
                notify(message.randomNumberID, builder.build())
            }
        }
    }


    private fun blahOK(blah: Blah): Boolean {
        val mainViewModel: MainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        if (blah.body.isBlank()) {
            mainViewModel.makeBodyOK(blah)
        }
        if (blah.topic.isEmpty()) {
            toastLong(getString(R.string.topic_needed))
            return false
        }
        return true
    }


    private fun createNotificationChannel(
        blah: Blah = safetyBlah,
        importance1to5: Int = NotificationManager.IMPORTANCE_DEFAULT
    ) {

        val id: String = blah.topic

        val name: CharSequence = blah.topic

        val channel = NotificationChannel(id, name, importance1to5)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    private fun destroyNotificationChannel(id: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.deleteNotificationChannel(id)

    }


    fun openAppNotificationSettings(context: Context) {
        val intent = Intent().apply {

            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            // TODO you can improve this by adding the specific channel, but i kept making mistakes and gave up
            //  info https://developer.android.com/training/notify-user/channels
        }
        context.startActivity(intent)
    }

//----------------------------------------------------------
// toast
//----------------------------------------------------------

    private fun toastShort(words: String) {
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(context, words, duration)
        toast.show()
    }

    private fun toastLong(words: String) {
        val duration = Toast.LENGTH_LONG
        val toast = Toast.makeText(context, words, duration)
        toast.show()
    }

//----------------------------------------------------------
// toast end
//----------------------------------------------------------

// =======================================================================

// ------------------------------------------------------------------------
// NOTIFICATION ends
// ------------------------------------------------------------------------

// =================================================================================
// NEARBY CONNECTIONS 2 start
// =================================================================================
    private val STRATEGY = Strategy.P2P_CLUSTER

    private lateinit var connectionsClient: ConnectionsClient

    private var opponentEndpointId: String? = null

    //private lateinit var binding: ActivityMainBinding

    private fun sendGameChoice(messageToSend: String = "test mf") {
        connectionsClient.sendPayload(
            opponentEndpointId!!,
            Payload.fromBytes(messageToSend.toByteArray(UTF_8))
        )
    }


    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startAdvertising(
            "myCodeName", // myCodeName
            packageName,
            connectionLifecycleCallback,
            options
        )
        //if (debugging){toastShort("advertising")}
    }



// =================================================================================
// NEARBY CONNECTIONS 2 end
// =================================================================================
/*
// =================================================================================
// NEARBY CONNECTIONS
// =================================================================================

// https://developers.google.com/nearby/connections/android/discover-devices
// https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#public-abstract-taskvoid-startadvertising
// this is mostly code copied from the tutorial. i only wrote a few lines in
// sendIt() and ReceiveBytesPayloadListener(). MF.
// ---------------------------------------------------------------------------------
// Nearby Connections - Advertising and discovery
// ---------------------------------------------------------------------------------


    //-----------------------------------------------------------------------------------

    private val SERVICE_ID: String = "100m"
    private val STRATEGY = Strategy.P2P_CLUSTER

    private fun startAdvertising() {

        val advertisingOptions: AdvertisingOptions =
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        Nearby.getConnectionsClient(context)
            .startAdvertising(
                labelToAdvertise, SERVICE_ID, connectionLifecycleCallback, advertisingOptions
            )
            .addOnSuccessListener { unused: Void? -> }
            .addOnFailureListener { e: Exception? -> }
        if (debugging) {
            toastLong("advertising")
        }

    }

// --------------------------------------------------------------------------------

    private fun startDiscovery() {

        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        Nearby.getConnectionsClient(context)
            .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener { unused: Void? -> }
            .addOnFailureListener { e: java.lang.Exception? -> }
        if (debugging) {
            toastShort("discovering")
        }

    }


// ---------------------------------------------------------------------------------
// Nearby Connections - Establishing connections
// ---------------------------------------------------------------------------------


    private val endpointDiscoveryCallback: EndpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {

            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {

                // An endpoint was found. We request a connection to it.
                Nearby.getConnectionsClient(context)
                    .requestConnection(labelToAdvertise, endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener { unused: Void? -> }
                    .addOnFailureListener { e: java.lang.Exception? -> Log.d(TAG, "error: $e") }
                if (debugging) {
                    toastLong("found... $endpointId")
                }

            }

            override fun onEndpointLost(endpointId: String) {
                // A previously discovered endpoint has gone away.
                if (debugging) {
                    toastLong("\nlost... $endpointId")
                }
            }
        }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                // Automatically accept the connection on both sides.
                Nearby.getConnectionsClient(context)
                    .acceptConnection(endpointId, ReceiveBytesPayloadListener)

            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {

                        val toEndPoint: String = endpointId

                        if (debugging) {
                            toastLong("sending data to...$toEndPoint")
                        }

                        sendIt(toEndPoint)
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {}
                    ConnectionsStatusCodes.STATUS_ERROR -> {}
                    else -> {}
                }
            }

            override fun onDisconnected(endpointId: String) {
                if (debugging) {
                    toastLong("suddenly disconnected from... $endpointId")
                }
            }
        }


// ---------------------------------------------------------------------------------
// Nearby Connections - Exchanging data
// ---------------------------------------------------------------------------------


    fun sendIt(toEndpointId: String) {


        val mainViewModel: MainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val payloadContents = mainViewModel.blahList.toTypedArray()
        val payloadSerialised = serialise(payloadContents)
        val bytesPayLoad = Payload.fromBytes(payloadSerialised)
        Nearby.getConnectionsClient(context).sendPayload(toEndpointId, bytesPayLoad)

        // debatable to include "sending" or not
        toastLong(getString(R.string.sending))

        if (debugging) {
            val unserialised = unSerialise(bytesPayLoad.asBytes())
            for (m in unserialised) {
                toastLong("sent and userialised: \n" + m.toString())
            }
        }

    }


    private val ReceiveBytesPayloadListener: PayloadCallback =
        object : PayloadCallback() {
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                // This always gets the full data of the payload. Is null if it's not a BYTES payload.
                if (payload.type == Payload.Type.BYTES) {
                    // val receivedBytes = payload.asBytes()

                    //mf
                    val messageReceived = unSerialise(payload.asBytes())
                    showFoundNotifications(messageReceived)

                    if (debugging) {
                        for (m in messageReceived) {
                            toastLong("received: \n" + m.toString())
                        }
                    }

                }
            }


            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate,
            ) {
                // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
                // after the call to onPayloadReceived().
            }
        }


// ---------------------------------------------------------------------------------
// Nearby Connections - Disconnecting
// ---------------------------------------------------------------------------------
// Finally, disconnectFromEndpoint() disconnects from a particular remote endpoint,
// and stopAllEndpoints() disconnects from all connected endpoints. Remote endpoints
// are notified of disconnection via the ConnectionLifecycleCallback.onDisconnected().

//------------------------------------------------------------------------------
// Senders and Receivers both can expect the PayloadCallback.onPayloadTransferUpdate() callback to be invoked to update them about the progress of outgoing and incoming Payloads, respectively.
//
// The connections established are full-duplex, which means that Advertisers and Discoverers can simultaneously send and receive Payloads.
// ---------------------------------------------------------------------------------
// Disconnecting
// ---------------------------------------------------------------------------------
// Finally, disconnectFromEndpoint() disconnects from a particular remote endpoint, and stopAllEndpoints() disconnects from all connected endpoints. Remote endpoints are notified of disconnection via the ConnectionLifecycleCallback.onDisconnected().
*/

// ----------------------------------------------------------------------------------
// serialize and encrypt
// ----------------------------------------------------------------------------------

    // these messages exchanged between users should be encrypted.
    // android best practices and good manners.
    // unfortunately, all the encryption / decryption code i found and tried caused fatal errors.
    // this needs to be done by a real programmer (not me). MF

    private fun serialise(messages: Array<Blah>): ByteArray {
        val step1 = Json.encodeToString(messages)
        val step2 = step1.toByteArray(Charsets.UTF_8)
        return step2
    }


    fun unSerialise(foundMessages: ByteArray?): Array<Blah> {
        if (foundMessages != null) {
            if (foundMessages.isNotEmpty()) {
                val step3 = foundMessages.toString(Charsets.UTF_8)
                val step4 = Json.decodeFromString<Array<Blah>>(step3)
                return step4
            }
        }
        return arrayOf(safetyBlah)
    }


// --------------------------------------------------------------------------------
// SERIALIZE and encrypt end
// --------------------------------------------------------------------------------


// --------------------------------------------------------------
// permissions
//---------------------------------------------------------------


    private fun getListOfPermissions(): List<String> {

        // see https://developers.google.com/nearby/connections/strategies

        var askForThesePermissions: List<String> = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )


        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.R
        ) {
            askForThesePermissions = listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION

            )
        }



        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
            askForThesePermissions = listOf(

                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,

                )
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askForThesePermissions = listOf(

                Manifest.permission_group.NEARBY_DEVICES,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.POST_NOTIFICATIONS

            )
        }


        return askForThesePermissions

    }


    private fun checkForPermission() {

        if (debugging) {
            Log.d(TAG, "android version: " + Build.VERSION.SDK_INT.toString())
        }

        for (thisPermission in getListOfPermissions()) {

            if (checkSelfPermission(thisPermission)
                == PackageManager.PERMISSION_GRANTED
            ) {
                if (debugging) {
                    Log.d(TAG, "\nok: $thisPermission")
                }
            } else {
                if (debugging) {
                    Log.d(TAG, "\nrequested permission for $thisPermission")
                }
                requestPermissionLauncher.launch(thisPermission)

            }
        }
    }


    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        )
        { isGranted: Boolean ->
            if (isGranted) {
                if (debugging) {
                    Log.d(TAG, "requestPermissionLauncher")
                }
            } else {

                toastLong(getString(R.string.error_missing_permissions))

                // TODO: 'needs permission' notice given when request ignored by activity
                // should be given when user has denied permission
            }
        }
// --------------------------------------------------------------
// permissions end
//---------------------------------------------------------------



    private fun startDiscovery(){
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(packageName,endpointDiscoveryCallback,options)
        //if(debugging){toastLong("discovering")}
    }

} // end


//---------------------------------




