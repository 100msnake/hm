package com.hundred_meters.hm

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class MainViewModel : ViewModel() {


    var emptyBlah : Blah = Blah(
        topic = "",
        body = "",
        randomNumberID = 0
    )

    private var listOfUsersTopics = mutableListOf<String>()


    // -------------------------------------------------------------------
    // data for Main Activity via livedata -------------------------------
    // -------------------------------------------------------------------

    private var singleSourceOfUserBlahs: MutableSet<Blah> = mutableSetOf()

    private val _blahList = singleSourceOfUserBlahs.toMutableStateList()
    val blahList: List<Blah>
        get() = _blahList

    val newMessageForNotification: MutableLiveData<Blah> by lazy {
        MutableLiveData<Blah>()
    }

    val singleSourceOfTruth: MutableLiveData<MutableSet<Blah>> by lazy {
        MutableLiveData<MutableSet<Blah>>()
    }

    // -------------------------------------------------------------------
    // data for Main Activity via livedata -------- end ------------------
    // -------------------------------------------------------------------

    // -------------------------------------------------------------------
    // data for UI Compose via observing start  --------------------------
    // -------------------------------------------------------------------

    // stateBlah is the data in the ui - what the user sees it the topic and message text boxes
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
    // data for UI Compose via observing end --------------------------
    // -------------------------------------------------------------------


    fun newBlah(
        newTopic: String = stateBlah.topic,
        newBody: String = stateBlah.body,
    ) {

        var newBodyVar = newBody

        if (newBodyVar.isEmpty()) {
            newBodyVar = "*"
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

        singleSourceOfUserBlahs.add(blah)
        _blahList.add(blah)
        newMessageForNotification.value = blah
        singleSourceOfTruth.value?.add(blah)
        listOfUsersTopics.add(newTopic)

    }


    fun deleteBlah(IDToDelete: Int) {

        if ( singleSourceOfUserBlahs.isEmpty()){ return }

        for (b in singleSourceOfUserBlahs) {
            if (b.randomNumberID == IDToDelete)
            {
                if (singleSourceOfUserBlahs.contains(b))
                {
                    singleSourceOfUserBlahs.remove(b)
                }
                if (_blahList.contains(b))
                {
                    _blahList.remove(b)
                }
                if (singleSourceOfTruth.value?.contains(b) == true)
                {
                    singleSourceOfTruth.value?.remove(b)
                }

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

        if (!checkThisTopic.startsWith("#")) {
            return true
        }

        // only display found messages with #topic already used by user. thus, privacy for sender.
        if (listOfUsersTopics.contains(checkThisTopic)) {
            return true
        }

        Log.d(TAG, "topic rejected: $checkThisTopic")
        return false
    }


    fun makeBodyOK(blah: Blah): String {

        // add * to blank messages. just slightly better than a blank message.
        // makes it more obvious the user is watching a topic without contributing.

        if (blah.body.trim().isEmpty()) {
            blah.body = "*"
            addToStateBlahBody(blah.body)
        }
        return blah.body
    }




}


// TODO i tried to have a single source of truth for user Blahs, but different types were needed
//  for compose state and livedata and they didn't convert.
//  still, there be a proper single source of truth


// TODO currently the notification channel is not deleted when the last blah using that topic is deleted.
//  Maybe not neccessary, but i thought i would mention it. MF


