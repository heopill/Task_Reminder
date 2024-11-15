package com.example.myongjiproject

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myongjiproject.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기 프래그먼트 설정
        supportFragmentManager.beginTransaction().replace(binding.fragmentContainer.id, HomeFragment()).commit()

        // 바텀 네비게이션 부분
        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.navigation_home -> HomeFragment()
                R.id.navigation_completed -> CompletedFragment()
                R.id.navigation_menu -> MenuFragment()
                R.id.navigation_notification -> NotificationsFragment()
                R.id.navigation_profile -> ProfileFragment()
                else -> HomeFragment()
            }
            supportFragmentManager.beginTransaction().replace(binding.fragmentContainer.id, selectedFragment).commit()
            true
        }
    }
}