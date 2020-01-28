package com.pusher.chatkit.gettingstarted

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso


@UiThread
internal class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(message: MessageAdapter.Message) {
        val lblSender = itemView.findViewById<TextView>(R.id.lblSender)
        val lblMessage = itemView.findViewById<TextView>(R.id.lblMessage)
        val imgAvatar = itemView.findViewById<ImageView>(R.id.imgAvatar)

        lblSender.text = message.senderName
        lblMessage.text = message.text

        if (message.senderAvatarUrl != null) {
            Picasso.get().load(message.senderAvatarUrl).into(imgAvatar)
        } else {
            imgAvatar.setImageDrawable(null)
        }
    }
}
