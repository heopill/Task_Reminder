package com.example.myongjiproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.myongjiproject.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        loadUserProfile()

        // 프로필 이미지 변경 버튼 클릭 시 ProfileImageActivity로 이동
        binding.btnProfileImage.setOnClickListener {
            // 사용자 정보를 Intent로 전달
            val intent = Intent(activity, ProfileImageActivity::class.java)
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val userId = user.uid
                val userRef = database.child("users").child(userId)
                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                        intent.putExtra("USER_ID", userId)
                        intent.putExtra("PROFILE_IMAGE_URL", profileImageUrl)
                        startActivity(intent)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("ProfileFragment", "Failed to fetch user data", error.toException())
                    }
                })
            }
        }

        // 로그아웃 버튼 클릭 리스너 설정
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        binding.btnWithdraw.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.let { user ->
                val userId = user.uid

//                // Firebase Storage에서 프로필 이미지 삭제 아직 프로필 이미지 미구현
//                val profileImageRef = storage.child("profile_images").child(userId)
//                profileImageRef.delete().addOnSuccessListener {
//                    Log.d("ProfileFragment", "Profile image deleted successfully")
//                }.addOnFailureListener { e ->
//                    Log.e("ProfileFragment", "Failed to delete profile image", e)
//                }

                // Firebase Realtime Database에서 사용자 데이터 삭제
                database.child("users").child(userId).removeValue().addOnSuccessListener {
                    Log.d("ProfileFragment", "User data deleted successfully")
                }.addOnFailureListener { e ->
                    Log.e("ProfileFragment", "Failed to delete user data", e)
                }

                // Firebase Authentication에서 사용자 계정 삭제
                user.delete().addOnSuccessListener {
                    Log.d("ProfileFragment", "User account deleted successfully")
                    // 로그아웃 후 로그인 화면으로 이동
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity?.finish()
                }.addOnFailureListener { e ->
                    Log.e("ProfileFragment", "Failed to delete user account", e)
                    // 사용자 계정 삭제 실패 처리 (예: 사용자에게 에러 메시지 표시)
                }
            }
        }

        return binding.root
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid
            val userRef = database.child("users").child(userId)

            // 사용자 정보 가져오기
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Error"
                    val email = currentUser.email ?: "Error"
                    val grade = snapshot.child("grade").getValue(String::class.java) ?: "Error"
                    val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                    binding.tvUserName.text = "사용자 이름 : $name"
                    binding.tvUserEmail.text = "이메일 주소 : $email"
                    binding.tvUserGrade.text = "학년 : $grade"

                }

                override fun onCancelled(error: DatabaseError) {
                    // 데이터 로드 실패 처리
                }
            })
        }
    }
}