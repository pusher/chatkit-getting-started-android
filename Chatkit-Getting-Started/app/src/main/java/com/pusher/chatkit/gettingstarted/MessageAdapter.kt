package com.pusher.chatkit.gettingstarted

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.pusher.chatkit.messages.multipart.Message

class MessageAdapter(private val context: Context,
                     private val currentUserId: String)
    : androidx.recyclerview.widget.RecyclerView.Adapter<MessageViewHolder>() {

    var messages = mutableListOf<Message>()


    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): MessageViewHolder {
        return MessageViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.row_message, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(context, messages[position], currentUserId)
    }

    fun addMessage(message: Message) {
        this.messages.add(message)
        notifyItemInserted(this.messages.size)
    }


}