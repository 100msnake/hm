package com.hundred_meters.hm

import android.provider.Settings.Global.getString
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class MainViewModel : ViewModel() {

    var emptyBlah : Blah = Blah(
        topic = "",
        body = "",
        randomNumberID = 0
    )

    var tempBlah : Blah = Blah(
        topic = "temp",
        body = "sort out blahs with a randomNumberID of 0, dude, FFS",
        randomNumberID = 0
    )

    var emptyBlahArray : Array<Blah> = arrayOf(emptyBlah, emptyBlah)

    private var listOfUsersTopics = mutableListOf<String>()

    // -------------------------------------------------------------------
    // data exchanged  using livedata ------------------------------------
    // -------------------------------------------------------------------
    // it doesn't feel like i did this right. i don't really understand
    // livedata or viewmodels, and the documentation is shit, or i'm stupid.

    private val _blahList = mutableStateListOf<Blah>(tempBlah)

    val blahList: List<Blah>
        get() = _blahList

    val newMessageForNotification: MutableLiveData<Blah> by lazy {
        MutableLiveData<Blah>()
    }


    // -------------------------------------------------------------------
    // data exchanged using livedata end ---------------------------------
    // -------------------------------------------------------------------

    // -------------------------------------------------------------------
    // data for UI Compose via observing ------- start  ------------------
    // -------------------------------------------------------------------
    // it doesn't feel like i did this right. i don't really understand
    // observing state to exchange data, and the documentation is shit.

    // stateBlah is the data in the ui - what the user sees in the topic and message text boxes
    var stateBlah by mutableStateOf(emptyBlah)
        private set

    fun triggerBlahComposeState(blah: Blah = emptyBlah) {
        stateBlah = blah
    }

    var stateOfTopicSwitch by mutableStateOf(false)

    // called when the switch is thrown
    fun triggerStateOfTopicSwitch(switchSetting: Boolean) {
        stateOfTopicSwitch = switchSetting
        addOrRemoveTopicHash()
    }

    // -------------------------------------------------------------------
    // data for UI Compose via observing -------end ----------------------
    // -------------------------------------------------------------------
    fun clearTextEntryBoxes(){
        stateBlah = emptyBlah
    }

    fun newBlah(
        newTopic: String = stateBlah.topic,
        newBody: String = stateBlah.body,
    ) {

        var newBodyVar = newBody

        if (newBodyVar.isEmpty()) {
            newBodyVar = "\t\uD83D\uDE08" //"*"
            addToStateBlahBody(newBodyVar)
        }
        if (newTopic.isEmpty()) {
            return
        }

        validateTopic()

        val blah = Blah(
            topic = newTopic,
            body = newBodyVar,
            randomNumberID = randomNumber()
        )

        _blahList.add(blah)
        newMessageForNotification.value = blah
        listOfUsersTopics.add(newTopic)
        clearTextEntryBoxes()
    }

    fun showNotificationArray(blahs : Array<Blah> = emptyBlahArray){
        // TODO reinstate keepBlah
        for (b in blahs){
            if (keepBlah(b.topic)) {
                newMessageForNotification.value = b
            }
        }

    }


    fun deleteBlah(IDToDelete: Int = randomNumber()) {

        if ( _blahList.isEmpty()){ return }

        for (b in _blahList) {
            if (b.randomNumberID == IDToDelete)
            {
                _blahList.remove(b)
                listOfUsersTopics.remove(b.topic)
            }
        }
    }



    fun validateTopic() {

        setTopicSwitch()
        topicSpacesToUnderScores()
    }

    private fun topicSpacesToUnderScores() {

        val t = stateBlah.topic
        val t1 = t.replace(" ", "_")
        newTopicToStateBlah(t1)
    }


    private fun addOrRemoveTopicHash() {

        topicSpacesToUnderScores()

        if (stateOfTopicSwitch) {
            if (!stateBlah.topic.startsWith("#")) {
                newTopicToStateBlah("#" + stateBlah.topic)
            }
        } else {
            val tempTopic = stateBlah.topic
            val tempTopic2: String

            if (tempTopic.startsWith("#")) {
                tempTopic2 = tempTopic.trimStart('#')
                newTopicToStateBlah(tempTopic2)
            }
        }
    }


    private fun setTopicSwitch() {

        if (stateBlah.topic.startsWith("#")) {
            triggerStateOfTopicSwitch(true)

        } else {
            triggerStateOfTopicSwitch(false)
        }
    }


    fun addToStateBlahBody(addToBody: String) {
        val blah = Blah(
            topic = stateBlah.topic,
            body = stateBlah.body + addToBody,
            randomNumberID = stateBlah.randomNumberID
        )
        triggerBlahComposeState(blah)
    }

    fun newTopicToStateBlah(newTopic: String) {
        val blah = Blah(
            topic = newTopic,
            body = stateBlah.body,
            randomNumberID = stateBlah.randomNumberID
        )
        triggerBlahComposeState(blah)
    }

    fun newBodyToStateBlah(newBody: String) {
        val blah = Blah(
            topic = stateBlah.topic,
            body = newBody,
            randomNumberID = stateBlah.randomNumberID
        )
        triggerBlahComposeState(blah)
    }

    fun keepBlah(checkThisTopic: String): Boolean {
        // reject #blahs that don't have a topic the user has already used
        if (!checkThisTopic.startsWith("#"))
        {
            return true
        }

        // only display found messages with #topic already used by user. thus, privacy for sender.
        if (listOfUsersTopics.contains(checkThisTopic))
        {
            return true
        }

        return false
    }


    fun makeBodyOK(blah: Blah): String {

        // add * to blank messages. just slightly better than a blank message.
        // makes it more obvious the user is watching a topic without contributing.

        if (blah.body.trim().isEmpty()) {
            blah.body = "\t\uD83D\uDE08"
            //(getString(R.string.error_missing_permissions))
            addToStateBlahBody(blah.body)
        }
        return blah.body
    }





}

