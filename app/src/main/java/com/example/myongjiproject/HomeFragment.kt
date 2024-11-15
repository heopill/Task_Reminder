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
import com.example.myongjiproject.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>() // 과제 목록

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        // RecyclerView 설정
        taskAdapter = TaskAdapter(
            taskList = taskList,
            onCompleteClick = { taskId, isCompleted ->
                // 완료된 과제를 대기중으로 처리
                if (isCompleted) {
                    markTaskAsPending(taskId)
                }
            },
            onDeleteClick = { taskId ->
                deleteTaskFromFirebase(taskId)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = taskAdapter

        // 과제 추가 버튼 클릭 시 다이얼로그 호출
        binding.btnAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        // 과제 목록 불러오기 함수
        fetchTasksFromFirebase()

        return binding.root
    }

    // 과제를 Firebase에서 가져오는 함수
    private fun fetchTasksFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "로그인 되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 사용자 이름 가져오기
        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(uid)

        userRef.child("name").get().addOnSuccessListener { nameSnapshot ->
            val userName = nameSnapshot.getValue(String::class.java) ?: "사용자"
            Log.d("HomeFragment", "사용자 이름: $userName")

            // 과제 목록 가져오기
            userRef.child("tasks").get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val taskList = mutableListOf<Task>()
                    var remainingTaskCount = 0

                    for (taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(Task::class.java)
                        task?.let {
                            if (!it.completed) {
                                // 완료되지 않은 과제만 추가
                                taskList.add(it)
                                remainingTaskCount++
                            }
                        }
                    }

                    // RecyclerView에 과제 목록 설정
                    taskAdapter.submitList(taskList)

                    // tvLeftTask 업데이트
                    binding.tvLeftTask.text = if (remainingTaskCount > 0) {
                        "$userName 님의 남은 과제는 $remainingTaskCount 개 입니다"
                    } else {
                        "$userName 님의 남은 과제가 없습니다"
                    }
                } else {
                    // 과제가 없을 경우에도 문구 업데이트
                    binding.tvLeftTask.text = "$userName 님의 남은 과제가 없습니다"
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(context, "데이터 로드 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(context, "사용자 이름 가져오기 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 과제 추가 다이얼로그 표시
    private fun showAddTaskDialog() {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogBinding.root)

        builder.setPositiveButton("확인") { dialog, _ ->
            val title = dialogBinding.etTaskTitle.text.toString().trim()
            val dueDate = dialogBinding.etDueDate.text.toString().trim()
            val courseName = dialogBinding.etCourseName.text.toString().trim()

            if (title.isNotEmpty() && dueDate.isNotEmpty() && courseName.isNotEmpty()) {
                addTaskToFirebase(title, dueDate, courseName)
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

    // 과제 정보를 Firebase에 추가
    private fun addTaskToFirebase(title: String, dueDate: String, courseName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "로그인 되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val taskId = FirebaseDatabase.getInstance().reference.push().key ?: return
        val task = Task(id = taskId, title = title, dueDate = dueDate, courseName = courseName)

        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("tasks")
            .child(taskId)
            .setValue(task)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    fetchTasksFromFirebase() // 과제 추가 후 목록 갱신
                } else {
                    Toast.makeText(context, "과제 추가 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }


    // 과제를 삭제 처리하는 함수
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
                fetchTasksFromFirebase() // 과제 삭제 후 목록 갱신
            } else {
                Toast.makeText(context, "과제 삭제 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
        taskRef.child("completed").setValue(true)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "과제가 완료상태로 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    fetchTasksFromFirebase() // 과제 목록 갱신
                } else {
                    Toast.makeText(context, "과제 대기중 처리 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}