package com.example.myongjiproject

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myongjiproject.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class TaskAdapter(
    private val taskList: MutableList<Task>,
    private val onCompleteClick: (String, Boolean) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

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

    // ViewHolder 내부 클래스
    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.tvTaskTitle.text = "과제 제목 : " + task.title
            binding.tvDueDate.text = "과제 마감일 : " + task.dueDate
            binding.tvCourseName.text = "강의명 : " + task.courseName

            // 남은 기간 계산 및 표시
            val remainingTime = getRemainingTime(task.dueDate)
            binding.tvLeftTime.text = remainingTime

            // 남은 기간에 따른 텍스트 색상 설정
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            try {
                val dueDateWithTime = "${task.dueDate} 23:59"
                val dueDateParsed = dateFormat.parse(dueDateWithTime)
                val currentDate = Date()

                if (dueDateParsed != null) {
                    val diffInMillis = dueDateParsed.time - currentDate.time

                    if (diffInMillis > 0) {
                        val daysLeft = diffInMillis / (1000 * 60 * 60 * 24)
                        when {
                            daysLeft >= 7 -> {
                                binding.tvLeftTime.setTextColor(binding.root.context.getColor(R.color.black)) // 7일 이상
                            }
                            daysLeft in 3 until 7 -> {
                                binding.tvLeftTime.setTextColor(binding.root.context.getColor(R.color.yellow)) // 3일 이상 7일 미만
                            }
                            daysLeft < 3 -> {
                                binding.tvLeftTime.setTextColor(binding.root.context.getColor(R.color.red)) // 3일 미만
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // ImageView 클릭 시 삭제 처리
            binding.ibDelete.setOnClickListener {
                onDeleteClick(task.id)
            }

            // CheckBox 상태 변경 시 완료 처리
            binding.cbCompleted.isChecked = task.completed // completed로 업데이트
            binding.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                onCompleteClick(task.id, isChecked) // onCompleteClick 함수에 isChecked 전달
            }
        }

        // 남은 기간을 계산하여 반환하는 함수
        private fun getRemainingTime(dueDate: String): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) // 마감일 포맷 수정 (시간 포함)
            try {
                val dueDateWithTime = "$dueDate 23:59" // 시간 부분을 23:59로 설정
                val dueDateParsed = dateFormat.parse(dueDateWithTime)
                val currentDate = Date()

                if (dueDateParsed != null) {
                    val diffInMillis = dueDateParsed.time - currentDate.time

                    if (diffInMillis > 0) {
                        val daysLeft = diffInMillis / (1000 * 60 * 60 * 24)
                        if (daysLeft >= 1) {
                            val hoursLeft = (diffInMillis % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
                            return "제출 마감까지 ${daysLeft}일 ${hoursLeft}시간 남았습니다"
                        } else {
                            val hoursLeft = (diffInMillis / (1000 * 60 * 60))
                            val minutesLeft = (diffInMillis % (1000 * 60 * 60)) / (1000 * 60)

                            return if (hoursLeft < 1) {
                                "제출 마감까지 ${minutesLeft}분 남았습니다"
                            } else {
                                "제출 마감까지 ${hoursLeft}시간 ${minutesLeft}분 남았습니다"
                            }
                        }
                    } else {
                        return "과제 마감!"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "날짜 오류"
        }
    }
}