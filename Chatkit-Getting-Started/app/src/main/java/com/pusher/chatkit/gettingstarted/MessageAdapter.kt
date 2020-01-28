package com.pusher.chatkit.gettingstarted

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView

@UiThread
internal class MessageAdapter(
    private val currentUserId: String,
    private val onClickPendingMessage: (MessageViewModel.Message, String) -> Unit,
    private val onClickFailedMessage: (MessageViewModel.Message, String) -> Unit
) : RecyclerView.Adapter<MessageViewHolder>(), MessageViewHolder.OnClickListener {

    private var items: List<MessageViewModel.MessageRow> = listOf()

    fun setItems(items: List<MessageViewModel.MessageRow>) {
        this.items = items
    }

    override fun getItemCount() = items.size

    private enum class ViewType {
        FROM_ME,
        FROM_OTHER,
        PENDING,
        FAILED
    }

    override fun getItemViewType(position: Int): Int {
        val row = items[position]
        return when (row.state) {
            MessageViewModel.MessageState.Pending -> ViewType.PENDING.ordinal
            MessageViewModel.MessageState.Failed -> ViewType.FAILED.ordinal
            MessageViewModel.MessageState.Confirmed ->
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
        holder.bind(items[position].message)
    }

    override fun onClick(position: Int) {
        val clickedRow = items[position]
        when (clickedRow.state) {
            MessageViewModel.MessageState.Failed ->
                onClickFailedMessage(clickedRow.message, clickedRow.internalId)
            MessageViewModel.MessageState.Pending ->
                onClickPendingMessage(clickedRow.message, clickedRow.internalId)
            MessageViewModel.MessageState.Confirmed -> { /* Ignore */ }
        }

    }
}
