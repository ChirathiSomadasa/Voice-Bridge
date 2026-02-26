package com.chirathi.voicebridge

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InsightAdapter(private val list: List<InsightItem>) :
    RecyclerView.Adapter<InsightAdapter.InsightViewHolder>() {

    class InsightViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val tvWord: TextView = view.findViewById(R.id.tvWeakWord)
        val tvDiagnosis: TextView = view.findViewById(R.id.tvSuggestion)
        val tvScore: TextView = view.findViewById(R.id.tvWeakScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InsightViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weakness_row, parent, false)
        return InsightViewHolder(view)
    }

    override fun onBindViewHolder(holder: InsightViewHolder, position: Int) {
        val item = list[position]
        holder.tvWord.text = "Target: ${item.content}"
        holder.tvDiagnosis.text = item.diagnosis
        holder.tvScore.text = "${item.score}%"

        // Color coding score
        if (item.score < 40) {
            holder.tvScore.setTextColor(Color.RED)
        } else {
            holder.tvScore.setTextColor(Color.parseColor("#FF9800")) // Orange
        }
    }

    override fun getItemCount() = list.size
}