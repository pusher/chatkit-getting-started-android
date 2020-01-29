package com.pusher.chatkit.gettingstarted

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

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

    data class Model(
        val items: List<Message>,
        val change: ChangeType
    )

    private val _model: MutableLiveData<Model> = MutableLiveData()

    val model get() = _model

    fun update(dataModel: DataModel.MessageModel) {
        _model.postValue(
            Model(
                items = dataModel.rows.map { item ->
                    val senderName = item.message.senderName ?: "Anonymous User"

                    val messageType = when (item.state) {
                        DataModel.MessageState.Pending -> MessageType.PENDING
                        DataModel.MessageState.Failed -> MessageType.FAILED
                        DataModel.MessageState.Confirmed -> {
                            if (item.message.senderId == dataModel.currentUserId) {
                                MessageType.FROM_ME
                            } else {
                                MessageType.FROM_OTHER
                            }
                        }
                    }

                    Message(senderName, item.message.senderAvatarUrl, item.message.text, messageType)
                },
                change = dataModel.change
            )
        )
    }
}

class DataModel(
    private val currentUserId: String
) {

    data class Message(
        val senderId: String,
        val senderName: String?,
        val senderAvatarUrl: String?,
        val text: String
    )

    data class MessageRow(
        val message: Message,
        val internalId: String,
        val state: MessageState
    )

    enum class MessageState {
        Confirmed,
        Pending,
        Failed
    }

    data class MessageModel(
        val currentUserId: String,
        val rows: List<MessageRow>,
        val change: ChangeType
    )

    private val rows: MutableList<MessageRow> = mutableListOf()

    private val _model: MutableLiveData<MessageModel> = MutableLiveData()

    val model: LiveData<MessageModel> get() = _model


    fun addConfirmedMessage(message: Message, internalId: String) {
        val newRow =
            MessageRow(message, internalId, MessageState.Confirmed)

        addOrUpdateRow(internalId, newRow)
    }

    fun addPendingMessage(message: Message, previousInternalId: String?): String {
        val internalId = previousInternalId ?: UUID.randomUUID().toString()

        val newRow =
            MessageRow(message, internalId, MessageState.Pending)

        addOrUpdateRow(internalId, newRow)

        return internalId
    }

    fun pendingMessageConfirmed(internalId: String) {
        updateRowState(internalId, MessageState.Confirmed)
    }

    fun pendingMessageFailed(internalId: String) {
        updateRowState(internalId, MessageState.Failed)
    }


    private fun indexOfRow(internalId: String): Int =
        rows.indexOfLast { row ->
            row.internalId == internalId
        }

    private fun addRow(newRow: MessageRow) {
        rows.add(newRow)
        _model.postValue(
            MessageModel(
                currentUserId,
                rows.toList(),
                ChangeType.ItemAdded(rows.size - 1)
            )
        )
    }

    private fun updateRow(position: Int, newRow: MessageRow) {
        rows[position] = newRow
        _model.postValue(
            MessageModel(
                currentUserId,
                rows.toList(),
                ChangeType.ItemUpdated(position)
            )
        )
    }

    private fun updateRowState(internalId: String, newState: MessageState) {
        val existingRowIndex = indexOfRow(internalId)

        if (existingRowIndex != -1) {
            val existingRow = rows[existingRowIndex]
            val newRow =
                MessageRow(
                    existingRow.message,
                    existingRow.internalId,
                    newState
                )

            updateRow(existingRowIndex, newRow)
        }
    }

    private fun addOrUpdateRow(internalId: String, newRow: MessageRow) {
        val existingRowIndex = indexOfRow(internalId)

        if (existingRowIndex == -1) {
            addRow(newRow)
        } else {
            updateRow(existingRowIndex, newRow)
        }
    }
}