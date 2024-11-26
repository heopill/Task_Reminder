package com.example.myongjiproject

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myongjiproject.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase 데이터베이스 참조 설정
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // 드롭다운 리스트 항목 설정
        val grades = listOf("1학년", "2학년", "3학년", "전공심화")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, grades)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGrade.adapter = adapter

        // 비밀번호와 비밀번호 확인 editText 확인 후 textView controll TextWatcher 이용
        val passwordWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = binding.etSignupPassword.text.toString()
                val passwordConfirm = binding.etSignupPasswordConfirm.text.toString()

                if (password == passwordConfirm) {
                    binding.tvPasswordCheck.apply {
                        text = "비밀번호가 일치합니다."
                        visibility = View.VISIBLE
                    }
                } else {
                    binding.tvPasswordCheck.apply {
                        text = "비밀번호가 일치하지 않습니다."
                        visibility = View.VISIBLE
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        // 두 비밀번호 필드에 TextWatcher 설정
        binding.etSignupPassword.addTextChangedListener(passwordWatcher)
        binding.etSignupPasswordConfirm.addTextChangedListener(passwordWatcher)

        // 회원가입 버튼 클릭 이벤트
        binding.btnSignupComplete.setOnClickListener {
            val email = binding.etSignupId.text.toString()
            val password = binding.etSignupPassword.text.toString()
            val passwordConfirm = binding.etSignupPasswordConfirm.text.toString()
            val name = binding.etName.text.toString()
            val grade = binding.spinnerGrade.selectedItem.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && password == passwordConfirm && name.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: ""
                            val user = User(email, password, name, grade, "")
                            saveUserToDatabase(userId, user)
                        } else {
                            Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "모든 필드를 올바르게 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 사용자 정보를 데이터베이스에 저장하는 함수
    private fun saveUserToDatabase(userId: String, user: User) {
        database.child("users").child(userId).setValue(user)
            .addOnCompleteListener { dbTask ->
                if (dbTask.isSuccessful) {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "회원가입 실패: ${dbTask.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

}

data class User(
    val id: String = "",
    val password: String = "",
    val name: String = "",
    val grade: String = "",
    val profileImageUrl: String = ""
)
