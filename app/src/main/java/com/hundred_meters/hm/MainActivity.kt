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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


const val TAG = "mfmf"


class MainActivity : ComponentActivity() {

    val context: Context = this

    // name the encryption you use in case encryption changes
    private val encryptionName: String = "100m00"

    private var broadcastBlahs: Array<Blah> = arrayOf()

    private var debugging = true


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


        val mainViewModel: MainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val newMessageObserver = Observer<Blah> { newMessageByUser ->
            showNotification(message = newMessageByUser)
        }

        val singleSourceOfTruthObserver = Observer<MutableSet<Blah>> { singleSourceOfTruth ->
            broadcastBlahs = singleSourceOfTruth.toTypedArray()
        }

        mainViewModel.newMessageForNotification.observe(this, newMessageObserver)
        mainViewModel.singleSourceOfTruth.observe(this, singleSourceOfTruthObserver)


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

        // start nearby connect
        startAdvertising()
        startDiscovery()

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
        val mainViewModel: MainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        for (item in blahArray) {
            if (mainViewModel.keepBlah(item.topic)) {
                //showNotification(message = item)
                mainViewModel.newMessageForNotification.value = item
                // TODO should all  notifications go through mainViewModel? NOT straight to showNotification?
                // TODO send to mainViewModel as an array, or individual blahs?
            }
        }
    }

    // TODO check over notification builder details. There must be cool things you missed
    // https://developer.android.com/reference/android/app/Notification.Builder


    private fun showNotification(
        message: Blah = safetyBlah,
        priority: Int = NotificationCompat.PRIORITY_LOW,
    ) {

        // TODO showNotification() looks really confused. UPDATE this bro. WTF.

        if (blahOK(message) && priorityOK(priority)) {

            val mainViewModel: MainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

            val CHANNEL_ID = mainViewModel.stateBlah.topic

            val importance = NotificationManager.IMPORTANCE_LOW

            val group = mainViewModel.stateBlah.topic

            createNotificationChannel(
                blah = message, //mainViewModel.stateBlah,
                importance1to5 = importance
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
        } //
        // TODO notification_id is the numberID of the Blah, so we can identify the Blah for pending intent?
    }


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

    private fun debugBlah(aString : String){

        //val bugBlah = Blah( topic = "DEBUG MESSAGE", body = aString, randomNumberID = 123)
        //showNotification(bugBlah)

        val mainViewModel: MainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        mainViewModel.newBlah(newTopic = "DEBUGGING MESSAGE", newBody = aString )
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


    fun importanceOK(importance: Int): Boolean {

        if (importance == 1) { // MIN
            return true
        }
        if (importance == 2) { // LOW
            return true
        }
        if (importance == 3) {
            return true
        }
        if (importance == 4) {
            return true
        }
        if (importance == 5) { // MAX
            return true
        }

        return false
    }


    private fun priorityOK(priority: Int): Boolean {

        if (priority == -2) { // MIN
            return true
        }
        if (priority == -1) { // LOW
            return true
        }
        if (priority == 0) {
            return true
        }
        if (priority == 1) {
            return true
        }
        if (priority == 2) { // MAX
            return true
        }

        return false
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


// =======================================================================

// ------------------------------------------------------------------------
// NOTIFICATION ends
// ------------------------------------------------------------------------

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

    }


    private fun startDiscovery() {

        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        Nearby.getConnectionsClient(context)
            .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener { unused: Void? -> }
            .addOnFailureListener { e: java.lang.Exception? -> }

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
                if (debugging){debugBlah("found... $endpointId")}

            }

            override fun onEndpointLost(endpointId: String) {
                // A previously discovered endpoint has gone away.
                Log.d(TAG, "\nlost... $endpointId")
                if (debugging){debugBlah("lost... $endpointId")}

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

                        Log.d(TAG, "\nsending data to...$toEndPoint")
                        if (debugging){debugBlah("sending data to...$toEndPoint")}


                        sendIt(toEndPoint)
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {}
                    ConnectionsStatusCodes.STATUS_ERROR -> {}
                    else -> {}
                }
            }

            override fun onDisconnected(endpointId: String) {
                // We've been disconnected from this endpoint. No more data can be
                // sent or received.
                Log.d(TAG, "\nsuddenly disconnected from... $endpointId")
                if (debugging){debugBlah("suddenly disconnected from... $endpointId")}
            }
        }


// ---------------------------------------------------------------------------------
// Nearby Connections - Exchanging data
// ---------------------------------------------------------------------------------


    fun sendIt(toEndpointId: String) {
        //val bytesPayload = Payload.fromBytes(byteArrayOf(0xa, 0xb, 0xc, 0xd))
        // mf
        //val crypt = Crypt()

        // TODO what is the payload. should be self from blahs
        //val bytesPayload = Payload.fromBytes(serialiseToSend(selfBlahs))

        //val bytesPayload = Payload.fromBytes(serializeToSend(mutableListOf(mm, mm2)))

        // mf just for testing


        val bytesPayLoad = Payload.fromBytes(serialise(broadcastBlahs))
        Nearby.getConnectionsClient(context).sendPayload(toEndpointId, bytesPayLoad)

            if (debugging){debugBlah("sending to: $toEndpointId \n\n$bytesPayLoad")}

        // mf delete when complete? probably
        toastLong(getString(R.string.sending))


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

                    var turnedToText = ""

                    for (b in messageReceived){
                        turnedToText += b.topic + "\n" + b.body + "\n" + b.randomNumberID.toString() + "\n\n"
                    }

                    if (debugging){debugBlah("received from : $endpointId \n\n$turnedToText")}


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
            if (foundMessages.isNotEmpty())
            {
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



//------------------------------------

    fun newLabel(): String {
        val numberName = randomNumber().toString()
        return "100m$encryptionName$numberName"
    }
// TODO sent package should have a name from newLabel() to include encryption type, surely

//--------------------------------------


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

        for (thisPermission in getListOfPermissions()) {

            if (checkSelfPermission(thisPermission)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "\nok: $thisPermission")
            } else {
                Log.d(TAG, "\nrequested permission for $thisPermission")
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
                Log.d(TAG, "requestPermissionLauncher")
            } else {

                toastLong(getString(R.string.error_missing_permissions))

                // TODO: 'needs permission' notice given when request ignored by activity
                // should be given when user has denied permission
            }
        }
// --------------------------------------------------------------
// permissions end
//---------------------------------------------------------------

} // end


//---------------------------------

// TODO link screen to messages. get it actually working


// TODO when you press on a topic, the UI edits that topic
// TODO check cut copy paste in textEdits
// TODO keep list of topics used and delete found messages not matching

