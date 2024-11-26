package com.example.myongjiproject

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.myongjiproject.databinding.ActivityProfileImageBinding
import com.google.common.io.Files.getFileExtension
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream

class ProfileImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileImageBinding
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val database = FirebaseDatabase.getInstance().reference

    private var selectedImageUri: Uri? = null
    private val userId = auth.currentUser?.uid ?: "default_user_id"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 사용자 프로필 이미지 URL이 비어있을 경우 기본 이미지 로드
        loadUserProfileImage()

        // 이미지 선택 버튼 클릭 시 갤러리 열기
        binding.ivSelectImage.setOnClickListener {
            openGallery()
        }

        // 저장 버튼 클릭 시 이미지 업로드
        binding.btnSave.setOnClickListener {
            if (selectedImageUri != null) {
                Log.d("Uri : ", "$selectedImageUri")
                uploadImageToFirebase(selectedImageUri!!)
            } else {
                Toast.makeText(this, "이미지를 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        // 나중에 설정 버튼 클릭 시 ProfileFragment로 돌아가기
        binding.btnSkip.setOnClickListener {
            finish() // ProfileFragment로 돌아가기 위해 Activity 종료
        }
    }

    // 사용자의 프로필 이미지 로드 (기본 이미지 or 이미 업로드된 이미지)
    private fun loadUserProfileImage() {
        // 기본 이미지 URL 설정
        val defaultImageUri = Uri.parse("android.resource://${packageName}/${R.drawable.profile_default}")
        Glide.with(this).load(defaultImageUri).into(binding.ivSelectImage)

        // 기존 프로필 이미지 URL을 Realtime Database에서 가져오기
        val userRef = database.child("users").child(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
            if (profileImageUrl != null && profileImageUrl.isNotEmpty()) {
                Glide.with(this).load(profileImageUrl).into(binding.ivSelectImage)
            }
        }.addOnFailureListener {
            // 데이터베이스에서 이미지를 가져오는 데 실패한 경우
            Log.e("ProfileImageActivity", "Failed to load profile image from database")
        }
    }

    // 갤러리 열기
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // 이미지 선택 후 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data // 선택된 이미지의 URI
            binding.ivSelectImage.setImageURI(selectedImageUri) // 선택된 이미지를 ImageView에 표시
        }
    }

    // 이미지 업로드
    private fun uploadImageToFirebase(imageUri: Uri) {
        // Firebase Storage에 업로드할 경로 설정
        val storage = FirebaseStorage.getInstance()
        val storageRef: StorageReference = storage.reference.child("profiles/$userId.jpg")
        Log.d("Storage Path", "Uploading to path: profiles/$userId.jpg")


        if (imageUri != null) {
            Log.d("Uri", "Uploading image from: $imageUri")
        storageRef.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                Log.d("Upload", "Upload successful: ${taskSnapshot.metadata?.path}")
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    Log.d("ProfileImageActivity", "File uploaded successfully: $uri")
                    // 다운로드 URL을 Firebase Realtime Database에 저장
                    saveProfileImageUrl(uri.toString())
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Upload Error", "Error uploading file: ${exception.message}")
                // 업로드 실패 시 처리
                Log.e("ProfileImageActivity", "File upload failed", exception)
                Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("Upload Error", "No image selected")
        }

}

    // 다운로드 URL을 Firebase Realtime Database에 저장
    private fun saveProfileImageUrl(imageUrl: String) {
        val userRef = database.child("users").child(userId)
        val userMap = hashMapOf<String, Any>("profileImageUrl" to imageUrl)

        userRef.updateChildren(userMap)
            .addOnSuccessListener {
                Log.d("ProfileImageActivity", "Profile image URL saved successfully")
                finish() // 작업 완료 후 Activity 종료
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileImageActivity", "Failed to save profile image URL", exception)
                Toast.makeText(this, "프로필 이미지 URL 저장 실패", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }
}