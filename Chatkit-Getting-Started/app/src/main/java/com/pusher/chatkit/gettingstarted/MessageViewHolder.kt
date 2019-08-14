package com.pusher.chatkit.gettingstarted

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import com.pusher.chatkit.messages.multipart.Message
import com.pusher.chatkit.messages.multipart.Payload
import com.squareup.picasso.Picasso


@UiThread
internal class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(message: Message) {
        val lblSender = itemView.findViewById<TextView>(R.id.lblSender)
        val lblMessage = itemView.findViewById<TextView>(R.id.lblMessage)
        val imgAvatar = itemView.findViewById<ImageView>(R.id.imgAvatar)

        lblSender.text = message.sender.name

        val inlineMessage: Payload.Inline = message.parts[0].payload as Payload.Inline
        lblMessage.text = inlineMessage.content

        if (message.sender.avatarURL != null) {
            Picasso.get().load(message.sender.avatarURL).into(imgAvatar)
        } else {
            imgAvatar.setImageDrawable(null)
        }
    }
}
