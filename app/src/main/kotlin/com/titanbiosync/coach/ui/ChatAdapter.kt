package com.titanbiosync.coach.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.databinding.ItemChatAssistantBinding
import com.titanbiosync.databinding.ItemChatUserBinding

class ChatAdapter : ListAdapter<UiChatMessage, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).role == AiCoachViewModel.ROLE_USER) VIEW_TYPE_USER
        else VIEW_TYPE_ASSISTANT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            UserViewHolder(ItemChatUserBinding.inflate(inflater, parent, false))
        } else {
            AssistantViewHolder(ItemChatAssistantBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is UserViewHolder -> holder.bind(msg)
            is AssistantViewHolder -> holder.bind(msg)
        }
    }

    // ── ViewHolders ────────────────────────────────────────────────────────

    class UserViewHolder(
        private val binding: ItemChatUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: UiChatMessage) {
            binding.messageText.text = msg.content
        }
    }

    class AssistantViewHolder(
        private val binding: ItemChatAssistantBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: UiChatMessage) {
            binding.messageText.text = msg.content
        }
    }

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_ASSISTANT = 1

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UiChatMessage>() {
            override fun areItemsTheSame(a: UiChatMessage, b: UiChatMessage) =
                a === b

            override fun areContentsTheSame(a: UiChatMessage, b: UiChatMessage) =
                a == b
        }
    }
}
