package com.example.myongjiproject

import android.app.AlertDialog
import android.os.Bundle
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
    private lateinit var taskList: MutableList<Task>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        // RecyclerView 설정
        taskList = mutableListOf()
        taskAdapter = TaskAdapter(taskList) { taskId ->
            // 과제가 완료 처리될 때 Firebase 업데이트
            markTaskAsCompleted(taskId)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = taskAdapter

        // 과제 추가 버튼 클릭 시 다이얼로그 호출
        binding.btnAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        // 과제 목록 불러오기
        fetchTasksFromFirebase()

        return binding.root
    }

    // 과제를 Firebase에서 가져오는 부분
    private fun fetchTasksFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "로그인 되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("tasks")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val taskList = mutableListOf<Task>()
                    for (taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(Task::class.java)
                        task?.let { taskList.add(it) }
                    }

                    taskAdapter.submitList(taskList) // List<Task>로 처리
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "데이터 로드 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }



    // 과제 추가 다이얼로그 표시
    private fun showAddTaskDialog() {
        // DialogAddTaskBinding을 통해 다이얼로그 레이아웃에 접근
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)

        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setView(dialogBinding.root)  // 다이얼로그 레이아웃 설정

        builder.setPositiveButton("확인") { dialog, _ ->
            val title = dialogBinding.etTaskTitle.text.toString().trim()
            val dueDate = dialogBinding.etDueDate.text.toString().trim()
            val courseName = dialogBinding.etCourseName.text.toString().trim()

            if (title.isNotEmpty() && dueDate.isNotEmpty() && courseName.isNotEmpty()) {
                // Firebase에 과제 추가
                addTaskToFirebase(title, dueDate, courseName)
            } else {
                Toast.makeText(context, "모든 필드를 입력해 주세요.", Toast.LENGTH_SHORT).show()
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

        // 사용자별로 과제를 저장
        FirebaseDatabase.getInstance().reference
            .child("users") // users 노드를 기준으로
            .child(uid) // 현재 로그인한 유저의 UID를 사용
            .child("tasks") // tasks 하위에 저장
            .child(taskId) // 각 과제의 고유 ID로 저장
            .setValue(task)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "과제가 추가되었습니다.", Toast.LENGTH_SHORT).show()
                    fetchTasksFromFirebase() // 과제 추가 후 목록 갱신
                } else {
                    Toast.makeText(context, "과제 추가 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // 과제를 완료 처리하는 함수
    private fun markTaskAsCompleted(taskId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "로그인 되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val taskRef = FirebaseDatabase.getInstance().reference
            .child("users") // users 노드
            .child(uid) // 현재 로그인한 유저의 UID
            .child("tasks") // tasks 노드
            .child(taskId) // 특정 taskId

        // 과제 완료 상태 업데이트
        taskRef.child("isCompleted").setValue(true)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "과제가 완료 처리되었습니다.", Toast.LENGTH_SHORT).show()
                    fetchTasksFromFirebase() // 완료 처리 후 과제 목록 갱신
                } else {
                    Toast.makeText(context, "과제 완료 처리 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}