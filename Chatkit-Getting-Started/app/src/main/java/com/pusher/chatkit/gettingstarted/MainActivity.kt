package com.pusher.chatkit.gettingstarted

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.pusher.chatkit.*
import com.pusher.chatkit.messages.multipart.Message
import com.pusher.chatkit.messages.multipart.NewPart
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.util.Result
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var currentUser: CurrentUser
    private lateinit var currentRoom: Room

    private lateinit var adapter: MessageAdapter
    private lateinit var messagesDataModel: MessagesDataModel
    private val messagesViewModel: MessagesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Activity has been invoked without preceding login
        if (Dependencies.currentUser == null) {
            startActivity(Intent(applicationContext, GreeterActivity::class.java))
            return
        }

        // configure the recyclerview
        adapter = MessageAdapter(
            onClickListener = this::onClickMessage
        )

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true

        recyclerViewMessages.layoutManager = layoutManager
        recyclerViewMessages.adapter = adapter

        currentUser = Dependencies.currentUser!!
        currentRoom = currentUser.rooms.first()

        messagesDataModel = MessagesDataModel(
            currentUserId = currentUser.id,
            currentUserName = currentUser.name,
            currentUserAvatarUrl = currentUser.avatarURL
        )
        messagesDataModel.model.observe(this, Observer<MessagesDataModel.MessagesModel> { newDataModel ->
            messagesViewModel.update(newDataModel)
        })

        messagesViewModel.model.observe(this, Observer<MessagesViewModel.MessagesView> { newViewModel ->
            adapter.update(newViewModel)
            when (newViewModel.change) {
                is ChangeType.ItemAdded -> {
                    recyclerViewMessages.scrollToPosition(newViewModel.change.index)
                }
            }
        })

        // subscribe to room
        currentUser.subscribeToRoomMultipart(
            currentRoom.id,
            consumer = { roomEvent ->
                runOnUiThread {
                    when (roomEvent) {
                        is RoomEvent.MultipartMessage -> this.onNewMessage(roomEvent.message)
                    }
                }
            },
            messageLimit = 50,
            callback = {}
        )

        txtMessage.setOnEditorActionListener { _, actionId, event ->
            // Return pressed on external / simulator keyboard
            val returnPressed = event != null &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    event.keyCode == KeyEvent.KEYCODE_ENTER
            // Send pressed on soft keyboard
            val sendPressed = actionId == EditorInfo.IME_ACTION_SEND

            if (sendPressed || returnPressed) {
                sendMessageFromTextEntry()
                true
            } else {
                // We must declare we "handled" all events related to the enter key, otherwise
                // further events (like key-up) will move the cursor focus elsewhere.
                event?.keyCode == KeyEvent.KEYCODE_ENTER
            }
        }
    }

    @UiThread
    private fun onNewMessage(message: Message) {
        messagesDataModel.addMessageFromServer(message)
    }

    fun onClickSendButton(view: View) {
        sendMessageFromTextEntry()
    }

    private fun onClickMessage(position: Int) {
        val item = messagesDataModel.model.value?.items?.get(position)

        if (item !is MessagesDataModel.MessageItem.Local) {
            return
        }

        if (item.state == MessagesDataModel.LocalMessageState.FAILED) {
            sendMessage(item.message)
        }
    }

    @UiThread
    private fun sendMessageFromTextEntry() {
        val text = txtMessage.text.toString().trim()
        if (text.isEmpty()) {
            return
        }

        txtMessage.setText("")
        sendMessage(MessageMapper.textToNewMessage(text))
    }

    @UiThread
    private fun sendMessage(message: List<NewPart>) {
        messagesDataModel.addPendingMessage(message)

        currentUser.sendMultipartMessage(
            roomId = currentRoom.id,
            parts = message,
            callback = { result ->
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            messagesDataModel.pendingMessageSent(message)
                        }
                        is Result.Failure -> {
                            messagesDataModel.pendingMessageFailed(message)
                            Toast.makeText(this, "Message failed to send, tap it to retry", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}
