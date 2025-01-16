package com.example.myongjiproject

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myongjiproject.databinding.FragmentMenuBinding
import com.google.firebase.database.*


class MenuFragment : Fragment() {

        private var _binding: FragmentMenuBinding? = null
        private val binding get() = _binding!!
        private val database = FirebaseDatabase.getInstance()
        private val menuRef = database.getReference("menu")
        private val daysOfWeek = listOf("월요일", "화요일", "수요일", "목요일", "금요일")

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            _binding = FragmentMenuBinding.inflate(inflater, container, false)

            fetchMenuDataFromFirebase()

            return binding.root
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }

        // 파이어베이스에서 메뉴 데이터 베이스 가져오기
        private fun fetchMenuDataFromFirebase() {
            menuRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dayMenus = mutableListOf<Pair<String, List<String>>>()

                    for (daySnapshot in snapshot.children) {
                        val day = daySnapshot.child("day").getValue(String::class.java)
                        val items = daySnapshot.child("items").children.mapNotNull { it.getValue(String::class.java) }

                        if (day != null && items.isNotEmpty()) {
                            dayMenus.add(Pair(day, items))
                        }
                    }

                    val sortedMenus = dayMenus.sortedBy { pair ->
                        val dayOnly = extractDay(pair.first)
                        daysOfWeek.indexOf(dayOnly).takeIf { it >= 0 } ?: Int.MAX_VALUE
                    }

                    if (sortedMenus.isNotEmpty()) {
                        val adapter = MenuPagerAdapter(sortedMenus)
                        binding.viewPager2.adapter = adapter
                    } else {
                        Log.d("MenuFragment", "No menus found.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    error.toException().printStackTrace()
                }
            })
        }

        private fun extractDay(day: String): String {
            val regex = """([가-힣]+)""".toRegex()
            val matchResult = regex.find(day)
            return matchResult?.value ?: day
        }
    }