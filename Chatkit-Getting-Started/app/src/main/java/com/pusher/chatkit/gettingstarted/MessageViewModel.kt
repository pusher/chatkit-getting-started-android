package com.pusher.chatkit.gettingstarted

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

class MessageViewModel : ViewModel() {

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
        val rows: List<MessageRow>,
        val change: ChangeType
    )

    sealed class ChangeType {
        data class ItemAdded(val index: Int) : ChangeType()
        data class ItemUpdated(val index: Int) : ChangeType()
    }

    private val rows: MutableList<MessageRow> = mutableListOf()
    private val _model: MutableLiveData<MessageModel> = MutableLiveData()
    val model: LiveData<MessageModel> get() = _model

    fun addMessage(message: Message, internalId: String) {
        // The message could be new, or it could be a replacement for a pending or failed message
        // already in our list
        val existingRowIndex = rows.indexOfLast { row ->
            row.internalId == internalId
        }

        val newRow =
            MessageRow(message, internalId, MessageState.Confirmed)

        if (existingRowIndex == -1) {
            rows.add(newRow)
            _model.postValue(MessageModel(rows.toList(), ChangeType.ItemAdded(rows.size-1)))
        } else {
            rows[existingRowIndex] = newRow
            _model.postValue(MessageModel(rows.toList(), ChangeType.ItemUpdated(existingRowIndex)))
        }
    }

    fun addPendingMessage(message: Message, previousInternalId: String?): String {
        val internalId = previousInternalId ?: UUID.randomUUID().toString()

        val newRow =
            MessageRow(message, internalId, MessageState.Pending)

        val existingRowIndex =
            when (previousInternalId) {
                null -> -1
                else -> rows.indexOfLast { row ->
                    row.internalId == previousInternalId
                }
            }

        if (existingRowIndex > -1) {
            rows[existingRowIndex] = newRow
            _model.postValue(MessageModel(rows.toList(), ChangeType.ItemUpdated(existingRowIndex)))
        } else {
            rows.add(newRow)
            _model.postValue(MessageModel(rows.toList(), ChangeType.ItemAdded(rows.size-1)))
        }

        return internalId
    }

    fun pendingMessageConfirmed(internalId: String) {
        val existingRowIndex = rows.indexOfLast { row ->
            row.internalId == internalId
        }
        if (existingRowIndex > -1) {
            val existingRow = rows[existingRowIndex]
            rows[existingRowIndex] =
                MessageRow(
                    existingRow.message,
                    existingRow.internalId,
                    MessageState.Confirmed
                )

            _model.postValue(MessageModel(rows.toList(), ChangeType.ItemUpdated(existingRowIndex)))
        }
    }

    fun pendingMessageFailed(internalId: String) {
        val existingRowIndex = rows.indexOfLast { row ->
            row.internalId == internalId
        }
        if (existingRowIndex > -1) {
            val existingRow = rows[existingRowIndex]
            rows[existingRowIndex] =
                MessageRow(
                    existingRow.message,
                    existingRow.internalId,
                    MessageState.Failed
                )

            _model.postValue(MessageModel(rows.toList(), ChangeType.ItemUpdated(existingRowIndex)))
        }
    }
}