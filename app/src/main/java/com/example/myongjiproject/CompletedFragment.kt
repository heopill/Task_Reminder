package com.example.myongjiproject

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myongjiproject.databinding.DialogAddTaskBinding
import com.example.myongjiproject.databinding.FragmentCompletedBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class CompletedFragment : Fragment() {
    private lateinit var binding: FragmentCompletedBinding
    private lateinit var completedTaskAdapter: TaskAdapter
    private val completedTaskList = mutableListOf<Task>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCompletedBinding.inflate(inflater, container, false)

        // RecyclerView 설정
        completedTaskAdapter = TaskAdapter(
            taskList = completedTaskList,
            onCompleteClick = { taskId, isCompleted ->
                // 완료된 과제를 대기중으로 처리
                if (isCompleted == false) {
                    markTaskAsPending(taskId)
                }
            },
            onDeleteClick = { taskId ->
                deleteTaskFromFirebase(taskId)
            },onEditClick = { task ->
                editTaskInFirebase(task)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = completedTaskAdapter

        // Firebase에서 완료된 과제 목록 불러오기
        fetchCompletedTasksFromFirebase()

        return binding.root
    }

    // 과제를 대기중으로 변경하는 함수
    private fun markTaskAsPending(taskId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "로그인 되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val taskRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("tasks")
            .child(taskId)

        // 과제 상태를 대기중으로 변경
        taskRef.child("completed").setValue(false)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "과제가 대기중으로 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    fetchCompletedTasksFromFirebase() // 과제 목록 갱신
                } else {
                    Toast.makeText(context, "과제 대기중 처리 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Firebase에서 완료된 과제 목록을 가져오는 함수
    private fun fetchCompletedTasksFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val taskRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(uid)
                .child("tasks")

            taskRef.orderByChild("completed").equalTo(true)  // "completed" 값이 true인 과제만 가져옴
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val completedTasks = mutableListOf<Task>()
                        for (data in snapshot.children) {
                            val task = data.getValue(Task::class.java)
                            if (task != null) {
                                completedTasks.add(task)
                            }
                        }
                        completedTaskAdapter.submitList(completedTasks)  // CompletedFragment에서 과제 리스트 갱신

                        val completedCount = completedTasks.size
                        binding.tvCompletedTask.text = "지금까지 $completedCount"+"개의 과제를 완료했어요!"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "데이터 로딩 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    // Firebase에서 과제를 삭제하는 함수
    private fun deleteTaskFromFirebase(taskId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "로그인 되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val taskRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("tasks")
            .child(taskId)

        taskRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "과제가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                fetchCompletedTasksFromFirebase() // 과제 삭제 후 목록 갱신
            } else {
                Toast.makeText(context, "과제 삭제 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 과제를 수정하는 함수
    private fun editTaskInFirebase(task: Task) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "로그인 되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)

        // 다이얼로그에 기존 과제 정보 설정
        val taskRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("tasks")
            .child(task.id)

        taskRef.get().addOnSuccessListener { snapshot ->
            val currentTask = snapshot.getValue(Task::class.java)
            if (currentTask != null) {
                // 기존 과제 정보로 다이얼로그 초기화
                dialogBinding.etTaskTitle.setText(currentTask.title)
                dialogBinding.etDueDate.setText(currentTask.dueDate)
                dialogBinding.etCourseName.setText(currentTask.courseName)

                // 다이얼로그 생성
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("과제 수정")
                    .setView(dialogBinding.root)

                builder.setPositiveButton("저장") { dialog, _ ->
                    val title = dialogBinding.etTaskTitle.text.toString().trim()
                    val dueDate = dialogBinding.etDueDate.text.toString().trim()
                    val courseName = dialogBinding.etCourseName.text.toString().trim()

                    if (title.isNotEmpty() && dueDate.isNotEmpty() && courseName.isNotEmpty()) {
                        val updatedTask = Task(
                            id = task.id,
                            title = title,
                            dueDate = dueDate,
                            courseName = courseName,
                            notified3Days = currentTask.notified3Days,
                            notified1Day = currentTask.notified1Day,
                            completed = currentTask.completed  // completed 상태 유지
                        )
                        updateTaskInDatabase(updatedTask)
                    } else {
                        Toast.makeText(context, "모든 정보를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }

                builder.setNegativeButton("취소") { dialog, _ ->
                    dialog.dismiss()
                }

                builder.create().show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "과제 정보를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTaskInDatabase(updatedTask: Task) {
        val taskRef = FirebaseDatabase.getInstance().getReference("users")
            .child(FirebaseAuth.getInstance().currentUser?.uid ?: "")
            .child("tasks")
            .child(updatedTask.id)

        taskRef.setValue(updatedTask).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "과제가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                fetchCompletedTasksFromFirebase() // 수정 후 과제 목록 갱신
            } else {
                Toast.makeText(context, "수정 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


}