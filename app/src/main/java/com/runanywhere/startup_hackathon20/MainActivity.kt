package com.runanywhere.startup_hackathon20

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var statusMessage: TextView
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var modelSelectorRecyclerView: RecyclerView
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button

    private var showModelSelector = false

    private val chatViewModel: ChatViewModel by viewModels()

    private val messagesAdapter = MessagesAdapter()
    private val modelsAdapter = ModelsAdapter(
        onDownload = { modelId -> chatViewModel.downloadModel(modelId) },
        onLoad = { modelId -> chatViewModel.loadModel(modelId) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            title = "Translexa"
        }


        statusMessage = findViewById(R.id.statusMessage)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        modelSelectorRecyclerView = findViewById(R.id.modelSelectorRecyclerView)
        inputEditText = findViewById(R.id.inputEditText)
        sendButton = findViewById(R.id.sendButton)

        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesRecyclerView.adapter = messagesAdapter

        modelSelectorRecyclerView.layoutManager = LinearLayoutManager(this)
        modelSelectorRecyclerView.adapter = modelsAdapter

        // Collect StateFlow messages with lifecycle awareness
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatViewModel.messages.collect { messages ->
                    messagesAdapter.submitList(messages)
                    if (messages.isNotEmpty()) {
                        messagesRecyclerView.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatViewModel.statusMessage.collect { message ->
                    statusMessage.text = message
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatViewModel.availableModels.collect { models ->
                    modelsAdapter.submitList(models)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatViewModel.currentModelId.collect { modelId ->
                    modelsAdapter.setCurrentModel(modelId)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatViewModel.isLoading.collect { isLoading ->
                    sendButton.isEnabled = !isLoading && inputEditText.text.isNotBlank()
                }
            }
        }

        inputEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sendButton.isEnabled = !s.isNullOrBlank() && chatViewModel.currentModelId.value != null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        sendButton.setOnClickListener {
            val text = inputEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                chatViewModel.sendMessage(text)
                inputEditText.text.clear()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_models -> {
                toggleModelSelector()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleModelSelector() {
        showModelSelector = !showModelSelector
        modelSelectorRecyclerView.visibility = if (showModelSelector) View.VISIBLE else View.GONE
    }
}
