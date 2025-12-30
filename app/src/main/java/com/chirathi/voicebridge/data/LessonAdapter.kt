package com.chirathi.voicebridge.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chirathi.voicebridge.R

class LessonAdapter(
    private val lessons: List<LessonModel>,
    private val onClick: (LessonModel) -> Unit
) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    class LessonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgLessonIcon)
        val title: TextView = view.findViewById(R.id.tvLessonTitle)
        val content: TextView = view.findViewById(R.id.tvLessonContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        // NOTE: Ensure you have a layout file named 'item_lesson.xml'
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]
        holder.title.text = lesson.lessonTitle
        holder.content.text = lesson.lessonContent

        // Handle Icon
        val iconId = lesson.getIconResId(holder.itemView.context)
        if (iconId != 0) {
            holder.icon.setImageResource(iconId)
        } else {
            // Default icon if not found
            holder.icon.setImageResource(R.drawable.lesson_icon)
        }

        holder.itemView.setOnClickListener { onClick(lesson) }
    }

    override fun getItemCount() = lessons.size
}