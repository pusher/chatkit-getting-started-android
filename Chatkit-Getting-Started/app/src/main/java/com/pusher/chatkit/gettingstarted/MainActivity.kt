package com.pusher.chatkit.gettingstarted

import android.os.Bundle
import android.util.Log
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
import com.pusher.chatkit.messages.multipart.Payload
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.util.Result
import elements.Error
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private val logTag = "MainActivity"

    private val INSTANCE_LOCATOR = "v1:us1:a373ff46-ae0f-49fa-a88b-68d1e03c53a8"
    private val TOKEN_PROVIDER_URL = "https://us1.pusherplatform.io/services/chatkit_token_provider/v1/a373ff46-ae0f-49fa-a88b-68d1e03c53a8/token"

    private val userId = "pusher-quick-start-bob"

    private lateinit var currentUser: CurrentUser
    private lateinit var currentRoom: Room

    private lateinit var adapter: MessageAdapter
    private val messagesViewModel: MessageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // configure the recyclerview
        adapter = MessageAdapter(
            currentUserId = userId,
            onClickPendingMessage = this::onClickPendingMessage,
            onClickFailedMessage = this::onClickFailedMessage
        )

        val messagesObserver = Observer<MessageViewModel.MessageModel> { newState ->
            adapter.setItems(newState.rows)
            when (newState.change) {
                is MessageViewModel.ChangeType.ItemUpdated -> {
                    adapter.notifyItemChanged(newState.change.index)
                }
                is MessageViewModel.ChangeType.ItemAdded -> {
                    adapter.notifyItemInserted(newState.change.index)
                    recyclerViewMessages.scrollToPosition(newState.change.index)
                }
            }
        }

        messagesViewModel.model.observe(this, messagesObserver)

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
                sendMessageFromTextEntry()
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
        val messageText = (message.parts[0].payload as Payload.Inline).content

        // Some messages which we sent in the distant past might not have internal IDs.
        val internalId = if (message.parts.size == 2) {
            (message.parts[1].payload as Payload.Inline).content
        } else {
            UUID.randomUUID().toString()
        }

        // add the message to our adapter
        messagesViewModel.addMessage(
            MessageViewModel.Message (
                senderId = message.sender.id,
                senderName = message.sender.name!!,
                senderAvatarUrl = message.sender.avatarURL,
                text = messageText
            ),
            internalId
        )
    }

    fun onClickSendButton(view: View) {
        sendMessageFromTextEntry()
    }

    private fun onClickPendingMessage(message: MessageViewModel.Message, internalId: String) {
        Log.d(logTag, "Pending message clicked")
    }

    private fun onClickFailedMessage(message: MessageViewModel.Message, internalId: String) {
        Log.d(logTag, "Failed message clicked")
        sendMessage(message.text, internalId)
    }

    @UiThread
    private fun sendMessageFromTextEntry() {
        val text = txtMessage.text.toString().trim()
        if (text.isEmpty()) {
            return
        }

        txtMessage.setText("")
        sendMessage(text, null)
    }

    @UiThread
    private fun sendMessage(text: String, previousInternalMessageId: String?) {
        Log.d(logTag, "Message send requested" + when (previousInternalMessageId) {
            null -> ", new message"
            else -> ", retry of internal id $previousInternalMessageId"
        })

        val internalMessageId = messagesViewModel.addPendingMessage(
            MessageViewModel.Message(
                senderId = currentUser.id,
                senderName = currentUser.name,
                senderAvatarUrl = currentUser.avatarURL,
                text = text
            ),
            previousInternalMessageId
        )

        currentUser.sendMultipartMessage(
            roomId = currentRoom.id,
            parts = listOf(
                NewPart.Inline(text, "text/plain"),
                NewPart.Inline(internalMessageId, "x-pusher/internal-id")
            ),
            callback = { result ->
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            Log.d(logTag, "Message send succeeded, internal id $internalMessageId, " +
                                    "chatkit message id ${result.value}")
                            // update the pending message row
                            messagesViewModel.pendingMessageConfirmed(internalMessageId)
                        }
                        is Result.Failure -> {
                            Log.d(logTag, "Message send failed, internal id $internalMessageId")
                            messagesViewModel.pendingMessageFailed(internalMessageId)
                            Toast.makeText(this, "Message failed to send, tap it to retry", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}
