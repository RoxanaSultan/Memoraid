package com.example.memoraid

import AccountViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide
import androidx.fragment.app.activityViewModels
import com.example.memoraid.models.User

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val accountViewModel: AccountViewModel by activityViewModels()
//    private var user: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize View Binding
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Check if ViewModel already has data, if not fetch from Firestore
        if (accountViewModel.username.value.isNullOrEmpty()) {
            val currentUser = auth.currentUser?.uid ?: ""
            if (currentUser.isNotEmpty()) {
                fetchUserData(currentUser)
            }
        } else {
            // Use data from ViewModel if available
            binding.username.text = accountViewModel.username.value
            binding.email.text = accountViewModel.email.value
            binding.firstName.text = accountViewModel.firstName.value
            binding.lastName.text = accountViewModel.lastName.value
            binding.phoneNumber.text = accountViewModel.phoneNumber.value
            binding.birthdate.text = accountViewModel.birthdate.value

            // Load profile picture using Glide (if stored in ViewModel)
            val profilePictureUrl = accountViewModel.profilePictureUrl.value
            Glide.with(this).load(profilePictureUrl)
                .placeholder(R.drawable.default_profile_picture)
                .into(binding.profilePicture)
        }
//        if (user == null)
//        {
//            val currentUser = auth.currentUser?.uid ?: ""
//            if (currentUser.isNotEmpty()) {
//                fetchUserData(currentUser)
//            }
//        }
//        else
//        {
//            binding.username.text = user!!.username
//            binding.email.text = user!!.email
//            binding.firstName.text = user!!.firstName
//            binding.lastName.text = user!!.lastName
//            binding.phoneNumber.text = user!!.phoneNumber
//            binding.birthdate.text = user!!.birthdate
//
//            // Load profile picture using Glide (if stored in ViewModel)
//            val profilePictureUrl = user!!.profilePictureUrl
//            Glide.with(this).load(profilePictureUrl)
//                .placeholder(R.drawable.default_profile_picture)
//                .into(binding.profilePicture)
//        }

        // Set up logout button
        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            // Navigate back to the login screen or perform other actions
            findNavController().navigate(R.id.fragment_login)
        }

        binding.editProfileButton.setOnClickListener {
            // Navigate to the EditProfileFragment
            // findNavController().navigate(R.id.action_accountFragment_to_editProfileFragment)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
    }

    private fun fetchUserData(userId: String) {
        val firestore = FirebaseFirestore.getInstance()

        // Query Firestore to get user data by UID (use the auth UID directly)
        firestore.collection("users")
            .document(userId)  // Using the UID directly to fetch the document
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (!documentSnapshot.exists()) {
                    Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Extract data from Firestore
                val username = documentSnapshot.getString("username") ?: ""
                val firstName = documentSnapshot.getString("firstName") ?: ""
                val lastName = documentSnapshot.getString("lastName") ?: ""
                val email = documentSnapshot.getString("email") ?: ""
                val phoneNumber = documentSnapshot.getString("phoneNumber") ?: ""
                val birthdate = documentSnapshot.getString("birthdate") ?: ""
                val profilePictureUrl = documentSnapshot.getString("profilePictureUrl") ?: ""
                val patientsList = documentSnapshot.get("patientsList") as? List<String> ?: emptyList()
                val role = documentSnapshot.getString("role") ?: ""

//                user = User(
//                    username,
//                    email,
//                    phoneNumber,
//                    firstName,
//                    lastName,
//                    profilePictureUrl,
//                    role,
//                    birthdate
//                )

                // Set data in ViewModel
                accountViewModel.setFirstName(firstName)
                accountViewModel.setLastName(lastName)
                accountViewModel.setUsername(username)  // Storing the UID (userId)
                accountViewModel.setEmail(email)
                accountViewModel.setPhoneNumber(phoneNumber)
                accountViewModel.setBirthdate(birthdate)
                accountViewModel.setProfilePicture(profilePictureUrl)
                accountViewModel.setPatientsList(patientsList)
                accountViewModel.setRole(role)

                // Update UI with data from Firestore
                binding.username.text = username  // Display the UID or other relevant info
                binding.email.text = email
                binding.firstName.text = firstName
                binding.lastName.text = lastName
                binding.phoneNumber.text = phoneNumber
                binding.birthdate.text = birthdate

                Glide.with(this).load(profilePictureUrl)
                    .placeholder(R.drawable.default_profile_picture)
                    .into(binding.profilePicture)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error fetching user data: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}