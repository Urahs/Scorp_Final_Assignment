package com.example.scorp_final_assignment.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.scorp_final_assignment.databinding.ItemChatTextBinding
import com.example.scorp_final_assignment.repository.Repository
import com.example.scorp_final_assignment.repository.Repository.Message


class TextDiffCallback(): DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
}

class MessageAdapter: ListAdapter<Message, MessageAdapter.MessageViewHolder>(TextDiffCallback()) {

    class MessageViewHolder(val binding: ItemChatTextBinding): RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val messageTV = holder.binding.textView
        val messageItem = getItem(position)
        messageTV.text = "[${messageItem.time.hour}:${messageItem.time.minute}] ${messageItem.content}"
    }
}
