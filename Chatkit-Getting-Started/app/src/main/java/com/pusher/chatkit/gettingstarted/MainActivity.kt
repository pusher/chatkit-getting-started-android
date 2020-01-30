package com.pusher.chatkit.gettingstarted

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
import elements.Error
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val logTag = "MainActivity"

    private val INSTANCE_LOCATOR = "v1:us1:a373ff46-ae0f-49fa-a88b-68d1e03c53a8"
    private val TOKEN_PROVIDER_URL = "https://us1.pusherplatform.io/services/chatkit_token_provider/v1/a373ff46-ae0f-49fa-a88b-68d1e03c53a8/token"

    private val userId = "pusher-quick-start-alice"

    private lateinit var currentUser: CurrentUser
    private lateinit var currentRoom: Room

    private lateinit var adapter: MessageAdapter
    private lateinit var messagesDataModel: DataModel
    private val messagesViewModel: MessagesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // configure the recyclerview
        adapter = MessageAdapter(
            onClickListener = this::onClickMessage
        )

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
            callback = {
                runOnUiThread { onConnected(it) }
            }
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
    private fun onConnected(result: Result<CurrentUser, Error>) {
        when (result) {
            is Result.Success -> {
                // success! save the user and the room to be able to use later
                currentUser = result.value
                currentRoom = currentUser.rooms.first()

                messagesDataModel = DataModel(
                    currentUserId = currentUser.id,
                    currentUserName = currentUser.name,
                    currentUserAvatarUrl = currentUser.avatarURL
                )
                messagesDataModel.model.observe(this, Observer<DataModel.MessageModel> { newDataModel ->
                    messagesViewModel.update(newDataModel)
                })

                messagesViewModel.model.observe(this, Observer<MessagesViewModel.MessagesViewUpdate> { newViewModel ->
                    adapter.setItems(newViewModel.items)
                    when (newViewModel.change) {
                        is ChangeType.ItemUpdated -> {
                            adapter.notifyItemChanged(newViewModel.change.index)
                        }
                        is ChangeType.ItemAdded -> {
                            adapter.notifyItemInserted(newViewModel.change.index)
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
            }
            is Result.Failure -> {
                Toast.makeText(this, result.error.reason, Toast.LENGTH_SHORT).show()
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
        val item = messagesDataModel.itemAt(position)

        if (item !is DataModel.MessageItem.Local) {
            return
        }

        if (item.state == DataModel.LocalMessageState.FAILED) {
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
        sendMessage(ChatkitMessageUtil.factoryMessage(text))
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
