package com.example.memoraid

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
import com.example.memoraid.adapters.JournalAdapter
import com.example.memoraid.databinding.FragmentJournalBinding
import com.example.memoraid.models.Journal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.Locale

class JournalFragment : Fragment() {

    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private lateinit var journalAdapter: JournalAdapter
    private val journalList = mutableListOf<Journal>()

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val db = FirebaseFirestore.getInstance()

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
            if (userId == null) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedDate = sdf.format(Date())

            val journalRef = db.collection("journals").document()
            val journalInfo = hashMapOf(
                "userId" to userId,
                "entryDate" to formattedDate,
                "title" to "Untitled",
                "text" to "",
                "imageUris" to listOf<String>()
            )

            journalRef.set(journalInfo).addOnSuccessListener {
                Toast.makeText(requireContext(), "Journal created successfully", Toast.LENGTH_SHORT).show()
                sendNotification()  // Send notification

                val bundle = Bundle().apply {
                    putString("journalId", journalRef.id)
                }
                findNavController().navigate(R.id.action_journalFragment_to_journalDetailsFragment, bundle)
            }.addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(requireContext(), "Failed to create new journal: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
        journalAdapter = JournalAdapter(journalList) { journal ->
            val bundle = Bundle().apply {
                putString("journalId", journal.id)
            }
            findNavController().navigate(R.id.action_journalFragment_to_journalDetailsFragment, bundle)
        }

        binding.journalRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = journalAdapter
        }
    }

    private fun loadJournals() {
        journalList.clear()
        // Fetch journals for the current user
        db.collection("journals")
            .whereEqualTo("userId", userId)
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

