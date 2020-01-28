package com.pusher.chatkit.gettingstarted

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import java.util.*

@UiThread
internal class MessageAdapter(
    private val currentUserId: String,
    private val onClickPendingMessage: (Message, String) -> Unit,
    private val onClickFailedMessage: (Message, String) -> Unit
) : RecyclerView.Adapter<MessageViewHolder>(), MessageViewHolder.OnClickListener {

    internal data class Message(
        val senderId: String,
        val senderName: String?,
        val senderAvatarUrl: String?,
        val text: String
    )

    private data class MessageRow(
        val message: Message,
        val internalId: String,
        val state: MessageState
    )

    internal enum class MessageState {
        Confirmed,
        Pending,
        Failed
    }

    private val rows = mutableListOf<MessageRow>()

    override fun getItemCount() = rows.size

    val lastIndex get() = this.rows.size-1

    private enum class ViewType {
        FROM_ME,
        FROM_OTHER,
        PENDING,
        FAILED
    }

    override fun getItemViewType(position: Int): Int {
        val row = rows[position]
        return when (row.state) {
            MessageState.Pending -> ViewType.PENDING.ordinal
            MessageState.Failed -> ViewType.FAILED.ordinal
            MessageState.Confirmed ->
                if (row.message.senderId == currentUserId) {
                    ViewType.FROM_ME.ordinal
                } else {
                    ViewType.FROM_OTHER.ordinal
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout =
            when (viewType) {
                ViewType.PENDING.ordinal -> R.layout.layout_message_row_pending
                ViewType.FAILED.ordinal -> R.layout.layout_message_row_failed
                ViewType.FROM_ME.ordinal -> R.layout.layout_message_row_me
                ViewType.FROM_OTHER.ordinal -> R.layout.layout_message_row
                else -> throw Error("Unrecognised view type $viewType")
            }

        return MessageViewHolder(
                LayoutInflater.from(parent.context).inflate(layout, parent, false), this
            )
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(rows[position].message)
    }

    override fun onClick(position: Int) {
        val clickedRow = rows[position]
        when (clickedRow.state) {
            MessageState.Failed ->
                onClickFailedMessage(clickedRow.message, clickedRow.internalId)
            MessageState.Pending ->
                onClickPendingMessage(clickedRow.message, clickedRow.internalId)
            MessageState.Confirmed -> { /* Ignore */ }
        }

    }

    fun addMessage(message: Message, internalId: String) {
        // The message could be new, or it could be a replacement for a pending or failed message
        // already in our list
        val existingRowIndex = rows.indexOfLast { row ->
            row.internalId == internalId
        }

        val newRow = MessageRow(message, internalId, MessageState.Confirmed)

        if (existingRowIndex == -1) {
            rows.add(newRow)
            notifyItemInserted(rows.size-1)
        } else {
            rows[existingRowIndex] = newRow
            notifyItemChanged(existingRowIndex)
        }
    }

    fun addPendingMessage(message: Message, previousInternalId: String?): String {
        val internalId = previousInternalId ?: UUID.randomUUID().toString()

        val newRow = MessageRow(message, internalId, MessageState.Pending)

        val existingRowIndex =
            when (previousInternalId) {
                null -> -1
                else -> rows.indexOfLast { row ->
                    row.internalId == previousInternalId
                }
            }

        if (existingRowIndex > -1) {
            rows[existingRowIndex] = newRow
            notifyItemChanged(existingRowIndex)
        } else {
            rows.add(newRow)
            notifyItemInserted(rows.size - 1)
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
                MessageRow(existingRow.message, existingRow.internalId, MessageState.Confirmed)

            notifyItemChanged(existingRowIndex)
        }
    }

    fun pendingMessageFailed(internalId: String) {
        val existingRowIndex = rows.indexOfLast { row ->
            row.internalId == internalId
        }
        if (existingRowIndex > -1) {
            val existingRow = rows[existingRowIndex]
            rows[existingRowIndex] =
                MessageRow(existingRow.message, existingRow.internalId, MessageState.Failed)

            notifyItemChanged(existingRowIndex)
        }
    }
}
