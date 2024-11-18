package com.example.myongjiproject

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myongjiproject.databinding.FragmentNotificationsBinding


class NotificationsFragment : Fragment() {

    private lateinit var binding: FragmentNotificationsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.loadUrl("https://www.mjc.ac.kr/bbs/data/list.do?menu_idx=66") // 학교 공지사항 URL

        return binding.root
    }
}