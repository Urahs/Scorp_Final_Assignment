package com.example.scorp_final_assignment.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.scorp_final_assignment.databinding.ItemChatTextBinding

class TextAdapter() : RecyclerView.Adapter<TextAdapter.TextViewHolder>(){

    class TextViewHolder(val binding: ItemChatTextBinding) : RecyclerView.ViewHolder(binding.root)

    private val diffCallBack = object : DiffUtil.ItemCallback<String>(){
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, diffCallBack)
    var messages: MutableList<String>
        get() = differ.currentList
        set(value) {differ.submitList(value)}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
        return TextViewHolder(
            ItemChatTextBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {

        holder.binding.textView.text = messages[position]
        /*
        holder.binding.categoryNameTV.text = categories[position]

        holder.binding.cardView.setOnClickListener {
            clickListener(position)
        }

        val isSelected = isCategorySelected(categories[position])
        val colorStr = if (isSelected) selectedCategoryCardBackgroundColor else "#FFFFFF"
        holder.binding.cardView.setCardBackgroundColor(Color.parseColor(colorStr))
        */
    }

    override fun getItemCount(): Int = messages.size
}



