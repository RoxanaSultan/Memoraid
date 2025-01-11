package com.example.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentJournalDetailsBinding
import com.example.memoraid.models.Journal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class JournalDetailsFragment : Fragment() {

    private var _binding: FragmentJournalDetailsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private var journalId: String? = null
    private var journal: Journal? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        journalId = arguments?.getString("journalId")
        if (journalId != null) {
            loadJournalDetails(journalId!!)
        }

        binding.saveButton.setOnClickListener {
            saveJournalDetails()
        }

        binding.pictureButton.setOnClickListener {

        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
            }
        })
    }

    private fun loadJournalDetails(journalId: String) {
        db.collection("journals").document(journalId).get()
            .addOnSuccessListener { document ->
                journal = document.toObject(Journal::class.java)
                journal?.let {
                    binding.title.setText(it.title)
                    binding.content.setText(it.text)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load journal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveJournalDetails() {
        journal?.let {
            // Update the journal details from the UI
            it.title = binding.title.text.toString()
            it.text = binding.content.text.toString()

            // Save the updated journal back to Firestore
            db.collection("journals").document(journalId!!)
                .set(it)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Journal saved", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to save journal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}