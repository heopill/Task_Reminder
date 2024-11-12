package com.example.myongjiproject

import androidx.recyclerview.widget.DiffUtil
import com.example.myongjiproject.Task

class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem.id == newItem.id // 과제의 ID가 같으면 동일한 항목으로 간주
    }

    override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem == newItem // 과제의 모든 내용이 같으면 동일한 항목으로 간주
    }
}

