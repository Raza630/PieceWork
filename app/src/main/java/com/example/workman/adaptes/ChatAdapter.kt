package com.example.workman.adaptes

import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.workman.R
import com.example.workman.dataClass.ChatMessage
import com.example.workman.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ChatAdapter(
    private val currentUserId: String // Only keep this one parameter
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()
    private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            with(binding) {
                messageText.text = message.messageText
                // Debug logging to verify IDs
                Log.d("ChatAdapter", "Current: $currentUserId, Sender: ${message.senderId}")

                senderName.text = if (message.senderId == currentUserId) "You" else "Other User"
                messageTime.text = dateFormat.format(Date(message.timestamp))

                // Align messages based on sender
                val params = messageLayout.layoutParams as LinearLayout.LayoutParams
                if (message.senderId == currentUserId) {
                    params.gravity = Gravity.END
                    messageLayout.setBackgroundResource(R.drawable.bg_sent_message)
                } else {
                    params.gravity = Gravity.START
                    messageLayout.setBackgroundResource(R.drawable.bg_received_message)
                }
                messageLayout.layoutParams = params
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    fun submitList(newMessages: List<ChatMessage>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = messages.size
            override fun getNewListSize() = newMessages.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                messages[oldPos].messageId == newMessages[newPos].messageId
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                messages[oldPos] == newMessages[newPos]
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages.clear()
        messages.addAll(newMessages)
        diffResult.dispatchUpdatesTo(this)
    }

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }
}




//class ChatAdapter(
//    private val currentUserId: String
//) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
//
//    private val messages = mutableListOf<ChatMessage>()
//
//    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val messageText: TextView = itemView.findViewById(R.id.messageText)
//        private val senderName: TextView = itemView.findViewById(R.id.senderName)
//        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
//        private val messageLayout: LinearLayout = itemView.findViewById(R.id.messageLayout)
//
//        fun bind(message: ChatMessage) {
//            messageText.text = message.messageText
//            senderName.text = if (message.senderId == currentUserId) "You" else "Other User"
//
//            // Format timestamp
//            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
//            messageTime.text = sdf.format(Date(message.timestamp))
//
//            // Align messages based on sender
//            val params = messageLayout.layoutParams as LinearLayout.LayoutParams
//            if (message.senderId == currentUserId) {
//                params.gravity = Gravity.END
//                messageLayout.setBackgroundResource(R.drawable.bg_sent_message)
//            } else {
//                params.gravity = Gravity.START
//                messageLayout.setBackgroundResource(R.drawable.bg_received_message)
//            }
//            messageLayout.layoutParams = params
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_message, parent, false)
//        return MessageViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
//        holder.bind(messages[position])
//    }
//
//    override fun getItemCount() = messages.size
//
//    fun submitList(newMessages: List<ChatMessage>) {
//        messages.clear()
//        messages.addAll(newMessages)
//        notifyDataSetChanged()
//    }
//}








//class ChatAdapter(private val messages: List<ChatMessage>, private val currentUserId: String) :
//    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
//
//    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val textMessage: TextView = itemView.findViewById(R.id.textMessage)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(
//            R.layout.item_message, parent, false
//        )
//        return MessageViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
//        val message = messages[position]
//        holder.textMessage.text = message.messageText
//
//        // Set up different layouts for messages based on the sender
//        if (message.senderId == currentUserId) {
//            holder.textMessage.gravity = Gravity.END
//        } else {
//            holder.textMessage.gravity = Gravity.START
//        }
//    }
//
//    override fun getItemCount(): Int = messages.size
//}
