package com.pusher.chatkit.gettingstarted

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

sealed class ChangeType {
    data class ItemAdded(val index: Int) : ChangeType()
    data class ItemUpdated(val index: Int) : ChangeType()
}

class MessagesViewModel : ViewModel() {
    enum class ViewType {
        PENDING,
        FAILED,
        FROM_ME,
        FROM_OTHER
    }

    data class MessageView(
        val senderName: String,
        val senderAvatarUrl: String?,
        val text: String,
        val viewType: ViewType
    )

    data class MessagesView(
        val items: List<MessageView>,
        val change: ChangeType
    )

    private val _model: MutableLiveData<MessagesView> = MutableLiveData()
    val model: LiveData<MessagesView> get() = _model

    fun update(dataModel: MessagesDataModel.MessagesModel) {
        val newItems = dataModel.items.map { item ->
            when (item) {
                is MessagesDataModel.MessageItem.FromServer -> {
                    val viewType =
                        if (item.message.sender.id == dataModel.currentUserId) {
                            ViewType.FROM_ME
                        } else {
                            ViewType.FROM_OTHER
                        }

                    MessageView(
                        senderName = item.message.sender.name ?: "Anonymous User",
                        senderAvatarUrl = item.message.sender.avatarURL,
                        text = MessageMapper.messageToText(item.message) ?: "",
                        viewType = viewType
                    )
                }
                is MessagesDataModel.MessageItem.Local -> {
                    val viewType =
                        when (item.state) {
                            MessagesDataModel.LocalMessageState.PENDING -> ViewType.PENDING
                            MessagesDataModel.LocalMessageState.FAILED -> ViewType.FAILED
                            MessagesDataModel.LocalMessageState.SENT -> ViewType.FROM_ME
                        }

                    MessageView(
                        senderName = dataModel.currentUserName ?: "Anonymous User",
                        senderAvatarUrl = dataModel.currentUserAvatarUrl,
                        text = MessageMapper.messageToText(item.message) ?: "",
                        viewType = viewType
                    )
                }
            }
        }

        _model.postValue(
            MessagesView(
                items = newItems,
                change = dataModel.lastChange
            )
        )
    }
}
