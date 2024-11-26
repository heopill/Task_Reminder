package com.example.myongjiproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myongjiproject.databinding.MenuCardViewBinding

class MenuPagerAdapter(private val menus: List<Pair<String, List<String>>>) : RecyclerView.Adapter<MenuPagerAdapter.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = MenuCardViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val (day, items) = menus[position]
        holder.bind(day, items)
    }

    override fun getItemCount(): Int = menus.size

    class MenuViewHolder(private val binding: MenuCardViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(day: String, items: List<String>) {
            binding.dayText.text = day
            binding.menuText.text = items.joinToString("\n")
        }
    }
}