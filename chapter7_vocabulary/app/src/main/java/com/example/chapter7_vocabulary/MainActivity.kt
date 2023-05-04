package com.example.chapter7_vocabulary

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chapter7_vocabulary.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), WordAdapter.ItemClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var wordAdpater: WordAdapter
    private var selectedWord: Word? = null

    private val imageLoadLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) {

    }

    private val updateAddWordResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val isUpdated = result.data?.getBooleanExtra("isUpdated", false)
        if (result.resultCode == RESULT_OK && isUpdated == true) {
            updateAddWord()
        }
    }

    private val updateEditWordResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val editWord = result.data?.getParcelableExtra<Word>("editWord")
        if (result.resultCode == RESULT_OK && editWord != null) {
            updateEditWord(editWord)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRecyclerView()

        binding.addButton.setOnClickListener {
            Intent(this, AddActivity::class.java).let {
                updateAddWordResult.launch(it)
            }
        }

        binding.deleteImageView.setOnClickListener {
            delete()
        }

        binding.editImageView.setOnClickListener {
            edit()
        }
    }

    private fun initRecyclerView() {

        wordAdpater = WordAdapter(mutableListOf(), this)
        binding.wordRecyclerView.apply {
            adapter = wordAdpater
            layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
            val dividerItemDecoration = DividerItemDecoration(applicationContext, LinearLayoutManager.VERTICAL)
            addItemDecoration(dividerItemDecoration)
        }

        Thread {
            val list = AppDatabase.getInstance(this)?.wordDao()?.getAll() ?: emptyList()
            wordAdpater.list.addAll(list)
            runOnUiThread { wordAdpater.notifyDataSetChanged() }
        }.start()
    }

    override fun onClick(word: Word) {
        selectedWord = word
        binding.textTextView.text = word.text
        binding.meanTextView.text = word.mean
    }

    private fun updateAddWord() {
        Thread {
            AppDatabase.getInstance(this)?.wordDao()?.getLatestWord()?.let { word ->
                wordAdpater.list.add(0, word)
                runOnUiThread { wordAdpater.notifyDataSetChanged() }
            }
        }.start()
    }

    private fun updateEditWord(word: Word) {
        val index = wordAdpater.list.indexOfFirst { it.id == word.id }
        wordAdpater.list.set(index, word)
        runOnUiThread {
            selectedWord = word
            wordAdpater.notifyItemChanged(index)
            binding.textTextView.text = word.text
            binding.meanTextView.text = word.mean
        }
    }

    private fun delete() {
        if (selectedWord == null) return

        Thread {
            selectedWord?.let { word ->
                AppDatabase.getInstance(this)?.wordDao()?.delete(word)
                runOnUiThread {
                    wordAdpater.list.remove(word)
                    wordAdpater.notifyDataSetChanged()
                    binding.textTextView.text = ""
                    binding.meanTextView.text = ""
                    Toast.makeText(this, "삭제가 완료됐습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun edit() {
        if (selectedWord == null) return

        val intent = Intent(this, AddActivity::class.java).putExtra("originWord", selectedWord)
        updateEditWordResult.launch(intent)
    }
}