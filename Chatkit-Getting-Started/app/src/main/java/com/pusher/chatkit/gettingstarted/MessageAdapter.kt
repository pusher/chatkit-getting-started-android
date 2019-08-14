package com.pusher.chatkit.gettingstarted

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import com.pusher.chatkit.messages.multipart.Message

internal enum class ViewType {
    FROM_ME,
    FROM_OTHER
}

@UiThread
internal class MessageAdapter(
    private val currentUserId: String
) : RecyclerView.Adapter<MessageViewHolder>() {

    private val messages = mutableListOf<Message>()

    override fun getItemCount() = messages.size

    val lastIndex get() = this.messages.size-1

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender.id == currentUserId) {
            ViewType.FROM_ME.ordinal
        } else {
            ViewType.FROM_OTHER.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout =
            if (viewType == ViewType.FROM_ME.ordinal) {
                R.layout.layout_message_row_me
            } else {
                R.layout.layout_message_row
            }

        return MessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(layout, parent, false)
            )
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    fun addMessage(message: Message) {
        messages.add(message)

        notifyItemInserted(lastIndex)
    }
}
