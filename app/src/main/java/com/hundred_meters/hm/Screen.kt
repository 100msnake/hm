package com.hundred_meters.hm

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun Screen(
    modifier: Modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    mainViewModel: MainViewModel = viewModel(),
) {

    val screenContext = LocalContext.current
    val mainActivity = MainActivity()
    val sky_blue = Color(0xff00B5E2)



    Scaffold(

        //modifier.padding(horizontal = 36.dp, vertical = 36.dp),


        // -------------------------------------------------------------
        // ---------------- topBar -------------------------------------
        // -------------------------------------------------------------


        topBar = {
            TopAppBar(

                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        //horizontalArrangement = Arrangement.Center

                    )
                    {

                        Text(
                            text = stringResource(id = R.string.app_name),
                            modifier = modifier.weight(1f),
                        )

                        IconButton(
                            onClick = { mainActivity.openAppNotificationSettings(screenContext) },
                            modifier = modifier,
                        )
                        {
                            Icon(
                                Icons.Filled.Notifications,
                                modifier = modifier,
                                contentDescription = stringResource(id = R.string.notification_settings)
                            )
                        }

                    } // Row
                }, // title

                // backgroundColor = sky_blue,
                backgroundColor = Color.Black,
                contentColor = Color.White,

                ) // topBar
        },

        // -------------------------------------------------------------
        // ---------------- floating action bar ------------------------
        // -------------------------------------------------------------

        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {

            AddFAB(
                mainViewModel,
                modifier = modifier,
            )
        }

    ) { PaddingValues  ->

        // I have zero idea how padding works with this mandatory PaddingValues shit.

        // -------------------------------------------------------------
        // ---------------- column -------------------------------------
        // -------------------------------------------------------------

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues),

            ) {

            // -------------------------------------------------------------
            // ---------------- circle --------------------------------------
            // -------------------------------------------------------------


            Row(
                modifier = modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_circle_vector),
                    modifier = modifier
                        .size(64.dp),
                    contentDescription = stringResource(id = R.string.app_icon)
                )
            }

            // -------------------------------------------------------------
            // ---------------- intro text ---------------------------------
            // -------------------------------------------------------------


            Text(
                modifier = modifier,
                text = stringResource(id = R.string.intro),
            )

            // -------------------------------------------------------------
            // ---------------- divider ------------------------------------
            // -------------------------------------------------------------

            Divider(
                modifier = modifier,
            )

            // -------------------------------------------------------------
            // ---------------- divider ------------------------------------
            // -------------------------------------------------------------

            Text(
                modifier = modifier,
                text = stringResource(id = R.string.send_a_new_message)
            )

            // -------------------------------------------------------------
            // ---------------- topic text field ---------------------------
            // -------------------------------------------------------------

            //var topicText by rememberSavable() { mutableStateOf("") }
            TopicTextField(
                modifier = modifier
                    .fillMaxWidth()
                    .onFocusChanged { mainViewModel.validateTopic() },
                topicText = mainViewModel.stateBlah.topic,
                onTopicChange = {
                    mainViewModel.newTopicToStateBlah(it)
                })

            // TODO validate topic

            // -------------------------------------------------------------
            // ---------------- topic switch -------------------------------
            // -------------------------------------------------------------

            TopicSwitch(
                modifier = modifier,
                mainViewModel.stateOfTopicSwitch,
                onTopicCheckedChange = { mainViewModel.triggerStateOfTopicSwitch(it) })


            // -------------------------------------------------------------
            // ---------------- body text field  ---------------------------
            // -------------------------------------------------------------

            BodyTextField(
                modifier = modifier
                    .fillMaxWidth(),
                //bodyText = bodyText,
                bodyText = mainViewModel.stateBlah.body,
                //onBodyChange = { bodyText = it })
                onBodyChange = {
                    mainViewModel.newBodyToStateBlah(it)
                }
            )

            // -------------------------------------------------------------
            // ---------------- send button --------------------------------
            // -------------------------------------------------------------

            Row(
                modifier = modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                SendButton(
                    modifier,
                    onDoSomething = {
                        if ( mainViewModel.stateBlah.topic.isNotEmpty() ) {
                            mainViewModel.newBlah()
                        } else {
                            val text = R.string.topic_needed
                            val duration = Toast.LENGTH_LONG
                            val toast = Toast.makeText(screenContext, text, duration)
                            toast.show()
                        }
                    },
                )
            }
            // TODO center in layout
            // TODO trigger newBlah
            // TODO colour of send button
            // TODO events triggered by clicks

            // -------------------------------------------------------------
            // ---------------- divider ------------------------------------
            // -------------------------------------------------------------

            Divider(
                modifier = modifier
                    .fillMaxWidth(),
            )

            // -------------------------------------------------------------
            // ---------------- persist string -----------------------------
            // -------------------------------------------------------------

            //Text(
            //    modifier = modifier,
            //    text = stringResource(id = R.string.persist)
            //)

            // -------------------------------------------------------------
            // ---------------- divider ------------------------------------
            // -------------------------------------------------------------


            Divider(
                modifier = modifier
                    .fillMaxWidth(),
            )

            // -------------------------------------------------------------
            // ---------------- messages being sent text -------------------
            // -------------------------------------------------------------

            Text(
                modifier = modifier,
                text = stringResource(id = R.string.messages)
            )

            // -------------------------------------------------------------
            // ---------------- cards --------------------------------------
            // -------------------------------------------------------------

            for (b in mainViewModel.blahList) {

                BlahCard(
                    modifier = modifier,
                    blah = b,
                    onClose = { if ( b.randomNumberID > 0 ) { mainViewModel.deleteBlah(b.randomNumberID) } }
                )
            }
        } // column
    } // scaffold


} // fun Screen


// -------------------------------------------------------------
// -------------------------------------------------------------
// -------------------------------------------------------------
// -------------------------------------------------------------
// -------------------------------------------------------------
// -------------------------------------------------------------

@Composable
fun SendButton(modifier: Modifier, onDoSomething: () -> Unit) {


    Button(
        modifier = modifier,
        onClick = onDoSomething,
        shape = MaterialTheme.shapes.medium,

    ) {
        Row {
            Text(
                modifier = modifier,
                text = stringResource(id = R.string.send)
            )
            Spacer(modifier = modifier)
            Icon(
                Icons.Filled.Send,
                modifier = modifier,
                contentDescription = "Send"
            )
        }
    }
}


@Composable
fun BlahCard(
    modifier: Modifier,
    blah: Blah,
    onClose: () -> Unit
) {
    Card(
        modifier = modifier,
        //backgroundColor = MaterialTheme.colors.surface
    ) {

        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = blah.topic,
                    modifier = modifier.weight(1f),
                    style = MaterialTheme.typography.body2,

                )
                IconButton(
                    onClick = onClose,
                    modifier = modifier
                ) {
                    Icon(
                        Icons.Filled.Close,
                        modifier = modifier,
                        contentDescription = "Close"
                    )
                }
            }
            Text(
                modifier = modifier,
                text = blah.body
            )
        } // Column
    } // Card
}


@Composable
fun TopicTextField(modifier: Modifier, topicText: String, onTopicChange: (String) -> Unit) {

    OutlinedTextField(
        modifier = modifier,
        value = topicText,
        onValueChange = onTopicChange,
        label = { Text(stringResource(id = R.string.topic)) },
        textStyle = MaterialTheme.typography.body2

    )
}

@Composable
fun BodyTextField(modifier: Modifier, bodyText: String, onBodyChange: (String) -> Unit) {

    OutlinedTextField(
        modifier = modifier,
        value = bodyText,
        onValueChange = onBodyChange,
        label = { Text(stringResource(id = R.string.message)) },
        textStyle = MaterialTheme.typography.body1
    )
}

// TODO typing in topicText empties bodyText, and vide versa

@Composable
fun TopicSwitch(
    modifier: Modifier,
    topicCheckedState: Boolean,
    onTopicCheckedChange: (Boolean) -> Unit
) {

    Row(

    )
    {

        Switch(
            modifier = modifier,
            checked = topicCheckedState,
            onCheckedChange = onTopicCheckedChange,
        )

        Text(
            modifier = modifier,
            text = stringResource(id = R.string.private_switch),
        )
    }

}

@Composable
fun AddFAB(mainViewModel: MainViewModel, modifier: Modifier) {
    FloatingActionButton(
        modifier = modifier,
        onClick = {
            mainViewModel.triggerBlahComposeState(mainViewModel.emptyBlah)
        }) {
        Icon(
            modifier = modifier,
            imageVector = Icons.Default.Add,
            contentDescription = "fab icon"
        )
    }
}


// -------------------------------------------------------------
// ----------------- previews ----------------------------------
// -------------------------------------------------------------

@Preview
@Composable
fun Shit3() {
    MFTheme {
        Surface()
        {
            Screen()
        }
    }
}

@Preview
@Composable
fun Shit4() {
    MFTheme(darkTheme = true) {
        Surface()
        {
            Screen()
        }
    }
}


// TODO all text coming from string resource
// TODO content descriptions for images and icons
// TODO accessibility check
// TODO prettier



