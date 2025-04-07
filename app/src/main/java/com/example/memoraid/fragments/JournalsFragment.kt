package com.example.memoraid.fragments

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.memoraid.JournalType
import com.example.memoraid.R
import com.example.memoraid.adapters.JournalAdapter
import com.example.memoraid.adapters.JournalModalAdapter
import com.example.memoraid.databinding.FragmentJournalBinding
import com.example.memoraid.models.Journal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JournalsFragment : Fragment() {

    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private lateinit var journalAdapter: JournalAdapter
    private val journalList = mutableListOf<Journal>()

    private lateinit var journalModalAdapter: JournalModalAdapter

    private val currentUser = FirebaseAuth.getInstance().currentUser?.uid
    private val database = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createNotificationChannel()  // Create notification channel

        setupRecyclerView()
        loadJournals()

        binding.newJournalImageButton.setOnClickListener {
            if (currentUser == null) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.modalContainer.visibility = View.VISIBLE
            setupModal()
        }

        binding.modalContainer.setOnClickListener {
            binding.modalContainer.visibility = View.GONE
        }

        binding.cancelButton.setOnClickListener{
            findNavController().navigate(R.id.action_journalModal_to_journalFragment)
        }

    }

    private fun sendNotification() {
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = 1

        val notificationBuilder = NotificationCompat.Builder(requireContext(), "journal_creation_channel")
            .setSmallIcon(R.drawable.notification)
            .setContentTitle("New Journal Created")
            .setContentText("Your new journal entry was created successfully!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    // TODO: add change journal type after creation

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "journal_creation_channel"
            val channelName = "Journal Notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Channel for journal creation notifications"
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupRecyclerView() {
        journalAdapter = JournalAdapter(requireContext(), journalList,
            onJournalClick = { journal ->
                val bundle = Bundle().apply {
                    putString("journalId", journal.id)
                }
                findNavController().navigate(R.id.action_journalFragment_to_journalDetailsFragment, bundle)
            },
            onJournalDelete = { deletedJournal ->
                binding.progressContainer.visibility = View.VISIBLE
                checkIfJournalDeleted(deletedJournal.id)
            }
        )

        binding.journalRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = journalAdapter
        }
    }

    private fun checkIfJournalDeleted(journalId: String) {
        database.collection("journals").document(journalId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val imageUris = document.get("imageUris") as? List<String> ?: emptyList()

                    if (imageUris.isNotEmpty()) {
                        deleteImagesFromStorage(imageUris) {
                            deleteJournalFromFirestore(journalId)
                        }
                    } else {
                        deleteJournalFromFirestore(journalId)
                    }
                } else {
                    binding.progressContainer.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error checking journal deletion", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteImagesFromStorage(imageUris: List<String>, onComplete: () -> Unit) {
        var deletedCount = 0
        for (imageUri in imageUris) {
            val storageRef = storage.getReferenceFromUrl(imageUri)
            storageRef.delete()
                .addOnSuccessListener {
                    deletedCount++
                    if (deletedCount == imageUris.size) {
                        onComplete()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to delete an image", Toast.LENGTH_SHORT).show()
                }
        }

        if (imageUris.isEmpty()) {
            onComplete()
        }
    }

    private fun deleteJournalFromFirestore(journalId: String) {
        database.collection("journals").document(journalId).delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Journal deleted successfully", Toast.LENGTH_SHORT).show()
                binding.progressContainer.visibility = View.GONE
                loadJournals()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to delete journal", Toast.LENGTH_SHORT).show()
                binding.progressContainer.visibility = View.GONE
            }
    }


    private fun setupModal() {
        journalModalAdapter = JournalModalAdapter { position ->
            createJournal(position)
        }
        binding.modalRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = journalModalAdapter
        }
    }

    private fun createJournal(selectedImageIndex: Int) {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = sdf.format(Date())

        val journalType = when (selectedImageIndex) {
            0 -> JournalType.JOURNAL_PINK.type
            1 -> JournalType.JOURNAL_BLUE.type
            2 -> JournalType.JOURNAL_YELLOW.type
            else -> {
                Toast.makeText(requireContext(), "Invalid selection", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val journalRef = database.collection("journals").document()
        val journalInfo = hashMapOf(
            "userId" to currentUser,
            "entryDate" to formattedDate,
            "title" to "Untitled",
            "text" to "",
            "imageUris" to listOf<String>(),
            "type" to journalType
        )

        journalRef.set(journalInfo).addOnSuccessListener {
            Toast.makeText(requireContext(), "Journal created successfully", Toast.LENGTH_SHORT).show()
            sendNotification()

            val bundle = Bundle().apply {
                putString("journalId", journalRef.id)
            }
            findNavController().navigate(R.id.action_journalFragment_to_journalDetailsFragment, bundle)

            binding.modalContainer.visibility = View.GONE
        }.addOnFailureListener { e ->
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to create new journal: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }



    private fun loadJournals() {
        journalList.clear()
        database.collection("journals")
            .whereEqualTo("userId", currentUser)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val journal = document.toObject(Journal::class.java).copy(id = document.id)
                    journalList.add(journal)
                }
                journalAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                // Handle error
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        journalList.clear()
    }
}

