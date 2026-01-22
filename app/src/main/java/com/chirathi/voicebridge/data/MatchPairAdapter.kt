package com.chirathi.voicebridge.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chirathi.voicebridge.R

class MatchPairAdapter(
    private val pairs: List<MatchPairModel>,
    private val onPairTouched: (MatchPairModel) -> Unit
) : RecyclerView.Adapter<MatchPairAdapter.PairVH>() {

    inner class PairVH(v: View) : RecyclerView.ViewHolder(v) {
        val left: TextView = v.findViewById(R.id.tvLeft)
        val right: TextView = v.findViewById(R.id.tvRight)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PairVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_lesson_match_pair, parent, false)
        return PairVH(v)
    }

    override fun onBindViewHolder(holder: PairVH, position: Int) {
        val pair = pairs[position]
        holder.left.text = pair.left
        holder.right.text = pair.right

        holder.itemView.setOnClickListener {
            onPairTouched(pair)
        }
    }

    override fun getItemCount(): Int = pairs.size
}