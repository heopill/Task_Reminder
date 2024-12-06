package com.example.myongjiproject

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myongjiproject.databinding.DialogAddTaskBinding
import com.example.myongjiproject.databinding.FragmentHomeBinding
import com.google.android.material.datepicker.DateValidatorPointBackward.before
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>() // 과제 목록

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        // 알림 권한 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // RecyclerView 설정
        taskAdapter = TaskAdapter(
            taskList = taskList,
            onCompleteClick = { taskId, isCompleted ->
                // 완료된 과제를 대기중으로 처리
                if (isCompleted) {
                    markTaskAsCompleted(taskId)
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

        // 알림 함수
        createNotificationChannel()

        // 백그라운드 실행 관련 함수
        setupWorkManagerForNotifications()

        return binding.root
    }

    private fun setupWorkManagerForNotifications() {
        // 현재 시간
        val now = Calendar.getInstance()

        // 자정 시간
        val midnight = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // 만약 현재 시간이 자정을 지난 경우 다음 자정으로 설정
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // 자정까지 남은 시간 계산
        val initialDelay = midnight.timeInMillis - now.timeInMillis

        // WorkRequest 생성
        val workRequest = PeriodicWorkRequestBuilder<TaskNotificationWorker>(
            1, TimeUnit.DAYS // 매일 반복
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS) // 처음 실행 시간 설정
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "TaskNotificationWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }


    // 과제를 Firebase에서 가져오는 함수
    private fun fetchTasksFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            //Toast.makeText(context, "로그인 되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
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
                    val currentDate = Date()

                    for (taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(Task::class.java)
                        task?.let {
                            if (!it.completed) {
                                // 완료되지 않은 과제만 추가
                                taskList.add(it)
                                remainingTaskCount++

                                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val currentDateWithoutTime = simpleDateFormat.parse(simpleDateFormat.format(currentDate))
                                val dueDateWithoutTime = simpleDateFormat.parse(it.dueDate)

                                if (currentDateWithoutTime != null && dueDateWithoutTime != null) {
                                    val daysLeft = ((dueDateWithoutTime.time - currentDateWithoutTime.time) / (1000 * 60 * 60 * 24)).toInt()

                                    when {
                                        daysLeft == 3 && !it.notified3Days -> {
                                            sendNotification(it, "[${it.title}] 제출 마감까지 3일 남았습니다.")
                                            updateTaskNotificationStatus(it.id, notified3Days = true, notified1Day = it.notified1Day)
                                        }
                                        daysLeft == 1 && !it.notified1Day -> {
                                            sendNotification(it, "[${it.title}] 제출 마감이 하루 남았습니다!")
                                            updateTaskNotificationStatus(it.id, notified3Days = it.notified3Days, notified1Day = true)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // RecyclerView에 과제 목록 설정
                    taskAdapter.submitList(taskList)

                    // tvLeftTask 업데이트
                    binding.tvLeftTask.text = if (remainingTaskCount > 0) {
                        "$userName"+"님의 남은 과제는 $remainingTaskCount"+"개 입니다"
                    } else {
                        "$userName"+"님의 남은 과제가 없습니다"
                    }
                } else {
                    // 과제가 없을 경우에도 TextView 업데이트
                    binding.tvLeftTask.text = "$userName"+"님의 남은 과제가 없습니다"
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

        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var dueDateObj: Date? = null
        try {
            dueDateObj = dateFormat.parse(dueDate)
        } catch (e: ParseException) {
            Toast.makeText(context, "잘못된 날짜 형식입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (dueDateObj == null) {
            Toast.makeText(context, "날짜 오류", Toast.LENGTH_SHORT).show()
            return
        }

        val daysLeft = (dueDateObj.time - currentDate.time) / (1000 * 60 * 60 * 24)
        if (daysLeft <= 1){
            val task = Task(
                id = taskId,
                title = title,
                dueDate = dueDate,
                courseName = courseName,
                notified3Days = true,
                notified1Day = false
            )
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
        } else {
            val task = Task(
                id = taskId,
                title = title,
                dueDate = dueDate,
                courseName = courseName,
                notified3Days = false,
                notified1Day = false
            )
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

    // 완료상태로 체크처리하는 함수
    private fun markTaskAsCompleted(taskId: String) {
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

    // 에뮬레이터 알림 권환 확인
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용됨
                Log.d("HomeFragment", "알림 권한 허용됨")
            } else {
                // 권한이 거부됨
                Log.d("HomeFragment", "알림 권한 거부됨")
            }
        }
    }

    // 알림 채널 생성하는 함수
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "TASK_DEADLINE_CHANNEL",
                "과제 마감 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "과제 마감 알림 채널입니다."
            }
            val notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 알림 생성하는 함수
    private fun sendNotification(task: Task, message: String) {
        val notificationId = task.id.hashCode()

        val notification = NotificationCompat.Builder(requireContext(), "TASK_DEADLINE_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("마감 기간 안내")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        Log.d("NotificationTest", "알림 발송") // 알림 발송 확인 로그
        //NotificationManagerCompat.from(requireContext()).notify(notificationId, notification)
        NotificationManagerCompat.from(requireContext()).notify(notificationId, notification)

    }

    // 알림 후 파이어베이스에 isNotified 값을 true로 변경하는 함수
    private fun updateTaskNotificationStatus(taskId: String, notified3Days: Boolean, notified1Day: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val taskRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("tasks")
            .child(taskId)

        val updates = mutableMapOf<String, Any>()
        updates["notified3Days"] = notified3Days
        updates["notified1Day"] = notified1Day


        taskRef.updateChildren(updates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("NotificationTest", "알림 상태 변경")
                } else {
                    Log.d("NotificationTest", "알림 상태 변경 실패 : ${task.exception?.message}")
                }
            }
    }

    class TaskNotificationWorker(
        context: Context,
        workerParams: WorkerParameters
    ) : Worker(context, workerParams) {

        override fun doWork(): Result {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()

            val userRef = FirebaseDatabase.getInstance().reference.child("users").child(uid).child("tasks")

            userRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val currentDate = Date()
                    for (taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(Task::class.java)
                        task?.let {
                            if (!it.completed) {
                                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val currentDateWithoutTime = simpleDateFormat.parse(simpleDateFormat.format(currentDate))
                                val dueDateWithoutTime = simpleDateFormat.parse(it.dueDate)

                                if (currentDateWithoutTime != null && dueDateWithoutTime != null) {
                                    val daysLeft = ((dueDateWithoutTime.time - currentDateWithoutTime.time) / (1000 * 60 * 60 * 24)).toInt()

                                    when {
                                        daysLeft == 3 && !it.notified3Days -> {
                                            sendNotification(it, "[${it.title}] 제출 마감까지 3일 남았습니다.")
                                            updateTaskNotificationStatus(uid, it.id, notified3Days = true, notified1Day = it.notified1Day)
                                        }
                                        daysLeft == 1 && !it.notified1Day -> {
                                            sendNotification(it, "[${it.title}] 제출 마감이 하루 남았습니다!")
                                            updateTaskNotificationStatus(uid, it.id, notified3Days = it.notified3Days, notified1Day = true)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }.addOnFailureListener {
                Log.e("TaskNotificationWorker", "Firebase 데이터 로드 실패: ${it.message}")
            }

            return Result.success()
        }

        private fun sendNotification(task: Task, message: String) {
            val notificationId = task.id.hashCode()
            val notification = NotificationCompat.Builder(applicationContext, "TASK_DEADLINE_CHANNEL")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("과제 알림")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            val notificationManager = NotificationManagerCompat.from(applicationContext)
            notificationManager.notify(notificationId, notification)
        }

        private fun updateTaskNotificationStatus(uid: String, taskId: String, notified3Days: Boolean, notified1Day: Boolean) {
            val taskRef = FirebaseDatabase.getInstance().reference.child("users").child(uid).child("tasks").child(taskId)

            taskRef.child("notified3Days").setValue(notified3Days)
            taskRef.child("notified1Day").setValue(notified1Day)
        }
    }
}