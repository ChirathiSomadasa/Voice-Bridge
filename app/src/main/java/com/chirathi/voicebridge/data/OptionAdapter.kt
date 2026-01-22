package com.chirathi.voicebridge.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chirathi.voicebridge.R

class OptionAdapter(
    private val options: List<OptionModel>,
    private val onChosen: (OptionModel) -> Unit
) : RecyclerView.Adapter<OptionAdapter.OptionVH>() {

    private var chosenId: String? = null

    inner class OptionVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv: TextView = itemView.findViewById(R.id.tvOptionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lesson_option, parent, false)
        return OptionVH(view)
    }

    override fun onBindViewHolder(holder: OptionVH, position: Int) {
        val opt = options[position]
        holder.tv.text = opt.text

        val ctx = holder.itemView.context
        val isChosen = chosenId == opt.id
        val bg = if (isChosen) R.color.light_orange else android.R.color.white
        holder.itemView.setBackgroundColor(ContextCompat.getColor(ctx, bg))

        holder.itemView.setOnClickListener {
            chosenId = opt.id
            notifyDataSetChanged()
            onChosen(opt)
        }
    }

    override fun getItemCount(): Int = options.size
}