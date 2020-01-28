package com.pusher.chatkit.gettingstarted

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

@UiThread
internal class MessageViewHolder(
    itemView: View,
    private val clickListener: OnClickListener
) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

    init {
        itemView.setOnClickListener(this)
    }

    fun bind(message: MessageViewModel.Message) {
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

    override fun onClick(v: View?) {
        clickListener.onClick(this.adapterPosition)
    }

    internal interface OnClickListener {
        fun onClick(position: Int)
    }
}
