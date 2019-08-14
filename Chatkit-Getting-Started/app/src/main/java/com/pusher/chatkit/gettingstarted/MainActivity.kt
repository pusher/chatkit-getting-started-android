package com.pusher.chatkit.gettingstarted

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.pusher.chatkit.*
import com.pusher.chatkit.messages.multipart.Message
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.util.Result
import elements.Error
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val INSTANCE_LOCATOR = "v1:us1:07864abd-2a7e-40c4-a0cb-6b9223b85aec"
    private val TOKEN_PROVIDER_URL = "https://us1.pusherplatform.io/services/chatkit_token_provider/v1/07864abd-2a7e-40c4-a0cb-6b9223b85aec/token"

    private val userId = "pusher-quick-start-alice"

    private lateinit var currentUser: CurrentUser
    private lateinit var currentRoom: Room

    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // configure the recyclerview
        adapter = MessageAdapter(userId)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerViewMessages.layoutManager = layoutManager
        recyclerViewMessages.adapter = adapter

        // create the chat manager
        val chatManager = ChatManager(
            instanceLocator = INSTANCE_LOCATOR,
            userId = userId,
            dependencies = AndroidChatkitDependencies(
                tokenProvider = ChatkitTokenProvider(
                    endpoint = TOKEN_PROVIDER_URL,
                    userId = userId
                ),
                context = this
            )
        )

        // attempt to connect
        chatManager.connect(
            listeners = ChatListeners(),
            callback = this::onConnected
        )

        txtMessage.setOnEditorActionListener { _, actionId, event ->
            // Return pressed on external / simulator keyboard
            val returnPressed = event != null &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    event.keyCode == KeyEvent.KEYCODE_ENTER
            // Send pressed on soft keyboard
            val sendPressed = actionId == EditorInfo.IME_ACTION_SEND

            if (sendPressed || returnPressed) {
                sendMessage()
                true
            } else {
                // We must declare we "handled" all events related to the enter key, otherwise
                // further events (like key-up) will move the cursor focus elsewhere.
                event?.keyCode == KeyEvent.KEYCODE_ENTER
            }
        }
    }

    private fun onConnected(result: Result<CurrentUser, Error>) {
        when (result) {
            is Result.Success -> {
                // success! save the user and the room to be able to use later
                currentUser = result.value
                currentRoom = currentUser.rooms.first()

                // subscribe to room
                currentUser.subscribeToRoomMultipart(
                    currentRoom.id,
                    consumer = { roomEvent ->
                        when (roomEvent) {
                            is RoomEvent.MultipartMessage -> runOnUiThread {
                                this.onNewMessage(roomEvent.message)
                            }
                        }
                    },
                    messageLimit = 50,
                    callback = {}
                )
            }
            is Result.Failure -> {
                runOnUiThread {
                    Toast.makeText(this, result.error.reason, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @UiThread
    private fun onNewMessage(message: Message) {
        // add the message to our adapter
        adapter.addMessage(message)
        // scroll to view the new message
        recyclerViewMessages.scrollToPosition(adapter.lastIndex)
    }

    fun onClickSend(view: View) {
        sendMessage()
    }

    @UiThread
    private fun sendMessage() {
        val text = txtMessage.text.toString().trim()
        if (text.isEmpty()) {
            return
        }

        //        txtMessage.isEnabled = false
        currentUser.sendSimpleMessage(
            roomId = currentRoom.id,
            messageText = txtMessage.text.toString(),
            callback = { result ->
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            // clear the edit text
                            txtMessage.setText("")
                        }
                        is Result.Failure -> {
                            Toast.makeText(
                                this,
                                "Could not send message",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

//                    txtMessage.isEnabled = true
                }
            }
        )
    }
}
