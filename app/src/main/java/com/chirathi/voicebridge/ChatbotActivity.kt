package com.chirathi.voicebridge

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chirathi.voicebridge.repository.AIRepository
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
)

class ChatbotActivity : AppCompatActivity() {

    private lateinit var recyclerMessages: RecyclerView
    private lateinit var editMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var progressLoading: ProgressBar
    private lateinit var quickRepliesLayout: LinearLayout

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val aiRepository = AIRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        initViews()
        setupRecyclerView()
        setupClickListeners()

        // Welcome message
        addBotMessage("Hello! I'm your therapy assistant. I'm here to support you. How are you feeling today?")
    }

    private fun initViews() {
        recyclerMessages = findViewById(R.id.recyclerMessages)
        editMessage = findViewById(R.id.editMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        progressLoading = findViewById(R.id.progressLoading)
        quickRepliesLayout = findViewById(R.id.quickRepliesLayout)
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
        recyclerMessages.adapter = adapter
        recyclerMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnSend.setOnClickListener { sendMessage() }

        editMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }

        // Quick reply chips
        findViewById<Chip>(R.id.chipAnger).setOnClickListener {
            sendQuickReply("I need help with anger management")
        }
        findViewById<Chip>(R.id.chipAnxiety).setOnClickListener {
            sendQuickReply("I'm feeling anxious")
        }
        findViewById<Chip>(R.id.chipSelfEsteem).setOnClickListener {
            sendQuickReply("I'm struggling with my self-esteem")
        }
        findViewById<Chip>(R.id.chipRelationship).setOnClickListener {
            sendQuickReply("I need relationship advice")
        }
        findViewById<Chip>(R.id.chipHelp).setOnClickListener {
            sendQuickReply("I need help")
        }
    }

    private fun sendMessage() {
        val messageText = editMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        // Add user message
        messages.add(ChatMessage(messageText, isUser = true))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerMessages.scrollToPosition(messages.size - 1)

        editMessage.text.clear()

        // Get bot response
        getBotResponse(messageText)
    }

    private fun sendQuickReply(message: String) {
        // Add user message
        messages.add(ChatMessage(message, isUser = true))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerMessages.scrollToPosition(messages.size - 1)

        // Get bot response
        getBotResponse(message)
    }

    private fun getBotResponse(userMessage: String) {
        progressLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = aiRepository.sendChatMessage(userMessage)

                progressLoading.visibility = View.GONE

                if (response != null && response.response.isNotEmpty()) {
                    addBotMessage(response.response)
                } else {
                    addBotMessage("I apologize, I'm having trouble connecting. Please try again.")
                }

            } catch (e: Exception) {
                progressLoading.visibility = View.GONE
                addBotMessage("I'm having connection issues. Please make sure the AI server is running.")
            }
        }
    }

    private fun addBotMessage(message: String) {
        messages.add(ChatMessage(message, isUser = false))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerMessages.scrollToPosition(messages.size - 1)
    }

    // Adapter for chat messages
    inner class ChatAdapter(private val messages: List<ChatMessage>) :
        RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

        inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val userMessageLayout: LinearLayout = view.findViewById(R.id.userMessageLayout)
            val botMessageLayout: LinearLayout = view.findViewById(R.id.botMessageLayout)
            val textUserMessage: TextView = view.findViewById(R.id.textUserMessage)
            val textBotMessage: TextView = view.findViewById(R.id.textBotMessage)
            val textTimestamp: TextView = view.findViewById(R.id.textTimestamp)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): MessageViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message, parent, false)
            return MessageViewHolder(view)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val message = messages[position]

            if (message.isUser) {
                holder.userMessageLayout.visibility = View.VISIBLE
                holder.botMessageLayout.visibility = View.GONE
                holder.textUserMessage.text = message.text
            } else {
                holder.userMessageLayout.visibility = View.GONE
                holder.botMessageLayout.visibility = View.VISIBLE
                holder.textBotMessage.text = message.text
            }

            holder.textTimestamp.text = message.timestamp
        }

        override fun getItemCount() = messages.size
    }
}
