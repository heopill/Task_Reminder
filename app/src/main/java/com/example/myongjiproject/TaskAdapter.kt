package com.example.myongjiproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myongjiproject.databinding.ItemTaskBinding


class TaskAdapter(private val taskList: MutableList<Task>, private val onCompleteClick: (String) -> Unit) :
    ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        // item_task.xml 레이아웃 파일을 인플레이트하여 ViewHolder 생성
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        // 현재 위치의 Task 데이터를 ViewHolder에 바인딩
        val task = getItem(position)
        holder.bind(task)
    }

    override fun submitList(list: List<Task>?) {
        super.submitList(list) // List<Task> 타입을 받아서 처리
    }


    // ViewHolder 내부 클래스
    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            // ViewBinding을 사용하여 TextView와 CheckBox에 데이터 바인딩
            binding.tvTaskTitle.text = task.title
            binding.tvDueDate.text = task.dueDate
            binding.tvCourseName.text = task.courseName

            // CheckBox 초기 상태 설정
            binding.cbCompleted.isChecked = task.isCompleted
            binding.cbCompleted.setOnClickListener {
                // CheckBox가 클릭되었을 때 과제 완료 처리
                if (binding.cbCompleted.isChecked) {
                    onCompleteClick(task.id) // 콜백 함수를 통해 완료 처리
                }
            }
        }
    }
}
