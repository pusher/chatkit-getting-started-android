package com.pusher.chatkit.gettingstarted

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pusher.chatkit.messages.multipart.Message
import com.pusher.chatkit.messages.multipart.NewPart
import com.pusher.chatkit.messages.multipart.Payload

sealed class ChangeType {
    data class ItemAdded(val index: Int) : ChangeType()
    data class ItemUpdated(val index: Int) : ChangeType()
}

class MessagesViewModel : ViewModel() {
    enum class MessageType {
        PENDING,
        FAILED,
        FROM_ME,
        FROM_OTHER
    }

    data class Message(
        val senderName: String,
        val senderAvatarUrl: String?,
        val text: String,
        val messageType: MessageType
    )

    data class MessagesViewUpdate(
        val items: List<Message>,
        val change: ChangeType
    )

    private val _model: MutableLiveData<MessagesViewUpdate> = MutableLiveData()

    val model get() = _model

    fun update(dataModel: DataModel.MessageModel) {
        val newMessages = dataModel.rows.map { item ->
            when (item) {
                is DataModel.MessageItem.FromServer -> {
                    val messageType =
                        if (item.message.sender.id == dataModel.currentUserId) {
                            MessageType.FROM_ME
                        } else {
                            MessageType.FROM_OTHER
                        }

                    Message(
                        senderName = item.message.sender.name ?: "Anonymous User",
                        senderAvatarUrl = item.message.sender.avatarURL,
                        text = ChatkitMessageUtil.findContentOfType("text/plain", item.message)
                            ?: "",
                        messageType = messageType
                    )
                }
                is DataModel.MessageItem.Local -> {
                    val messageType =
                        when (item.state) {
                            DataModel.LocalMessageState.PENDING -> MessageType.PENDING
                            DataModel.LocalMessageState.FAILED -> MessageType.FAILED
                            DataModel.LocalMessageState.SENT -> MessageType.FROM_ME
                        }

                    Message(
                        senderName = dataModel.currentUserName ?: "Anonymous User",
                        senderAvatarUrl = dataModel.currentUserAvatarUrl,
                        text = ChatkitMessageUtil.findContentOfType("text/plain", item.message)
                            ?: "",
                        messageType = messageType
                    )
                }
            }
        }

        _model.postValue(
            MessagesViewUpdate(
                items = newMessages,
                change = dataModel.change
            )
        )
    }
}

object ChatkitMessageUtil {
    val MIME_TYPE_INTERNAL_ID = "com-pusher-gettingstarted/internal-id"

    fun internalId(message: Message): String? =
        findContentOfType(MIME_TYPE_INTERNAL_ID, message)

    fun internalId(message: List<NewPart>): String? =
        findContentOfType(MIME_TYPE_INTERNAL_ID, message)

    fun findContentOfType(type: String, message: Message): String? {
        val part = message.parts.find {
            (it.payload as? Payload.Inline)?.type == type
        }

        return (part?.payload as? Payload.Inline)?.content
    }

    fun findContentOfType(type: String, message: List<NewPart>): String? {
        val part = message.find {
            (it as? NewPart.Inline)?.type == type
        }

        return (part as? NewPart.Inline)?.content
    }
}

class DataModel(
    private val currentUserId: String,
    private val currentUserName: String?,
    private val currentUserAvatarUrl: String?
) {
    enum class LocalMessageState {
        PENDING,
        FAILED,
        SENT
    }

    sealed class MessageItem {
        data class FromServer(
            val message: Message
        ): MessageItem()

        data class Local(
            val message: List<NewPart>,
            val state: LocalMessageState
        ): MessageItem()

        val internalId: String? get() = when (this) {
            is FromServer -> ChatkitMessageUtil.internalId(message)
            is Local -> ChatkitMessageUtil.internalId(message)
        }
    }

    data class MessageModel(
        val currentUserId: String,
        val currentUserName: String?,
        val currentUserAvatarUrl: String?,
        val rows: List<MessageItem>,
        val change: ChangeType
    )

    private val items: MutableList<MessageItem> = mutableListOf()

    private val _model: MutableLiveData<MessageModel> = MutableLiveData()

    val model: LiveData<MessageModel> get() = _model

    fun addMessageFromServer(message: Message) {
        addOrUpdateItem(MessageItem.FromServer(message))
    }

    fun addPendingMessage(message: List<NewPart>) {
        addOrUpdateItem(MessageItem.Local(message, LocalMessageState.PENDING))
    }

    fun pendingMessageSent(message: List<NewPart>) {
        addOrUpdateItem(MessageItem.Local(message, LocalMessageState.SENT))
    }

    fun pendingMessageFailed(message: List<NewPart>) {
        addOrUpdateItem(MessageItem.Local(message, LocalMessageState.FAILED))
    }

    private fun addOrUpdateItem(item: MessageItem) {
        when (item) {
            is MessageItem.FromServer -> {
                // A message from the server is canonical, and will always replace a local message
                // with the same internalId
                val index = findItemIndexByInternalId(item.internalId)

                if (index == -1) {
                    addItem(item)
                } else {
                    replaceItem(item, index)
                }
            }
            is MessageItem.Local -> {
                // We may update the state of local messages, but we should never overwrite that of
                // a FromServer message
                val index = findItemIndexByInternalId(item.internalId)

                if (index == -1) {
                    addItem(item)
                } else if (items[index] !is MessageItem.FromServer) {
                    replaceItem(item, index)
                }
            }
        }
    }

    private fun addItem(item: MessageItem) {
        items.add(item)
        _model.postValue(
            MessageModel(
                currentUserId,
                currentUserName,
                currentUserAvatarUrl,
                items,
                ChangeType.ItemAdded(items.size-1)
            )
        )
    }

    private fun replaceItem(item: MessageItem, index: Int) {
        items[index] = item
        _model.postValue(
            MessageModel(
                currentUserId,
                currentUserName,
                currentUserAvatarUrl,
                items,
                ChangeType.ItemUpdated(index)
            )
        )
    }

    private fun findItemIndexByInternalId(internalId: String?): Int =
         items.indexOfLast { item ->
             when (item) {
                 is MessageItem.FromServer -> item.internalId == internalId
                 is MessageItem.Local -> item.internalId == internalId
             }
         }
}