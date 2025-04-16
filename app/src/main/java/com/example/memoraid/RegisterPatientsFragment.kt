package com.example.memoraid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentRegisterPatientsBinding
import com.example.memoraid.viewmodel.RegisterSharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class RegisterPatientsFragment : Fragment() {

    private lateinit var binding: FragmentRegisterPatientsBinding
    private val sharedViewModel: RegisterSharedViewModel by activityViewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val storage = FirebaseStorage.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.registerFinishButton.setOnClickListener {
            if (!binding.checkboxTerms.isChecked) {
                Toast.makeText(requireContext(), "You must agree to the terms and conditions.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val emailValue = sharedViewModel.email.value
            val passwordValue = sharedViewModel.password.value

            registerUser(emailValue!!, passwordValue!!)
        }

        binding.linkTermsConditions.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.terms_conditions_url)))
            startActivity(intent)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sharedViewModel.clearData()
                findNavController().navigateUp()
            }
        })
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserDetails()
                    sharedViewModel.clearData()
                    findNavController().navigate(R.id.register_finish_button)
                } else {
                    Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserDetails() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)

        val username = sharedViewModel.username.value
        val email = sharedViewModel.email.value
        val firstName = sharedViewModel.firstName.value
        val lastName = sharedViewModel.lastName.value
        val phoneNumber = sharedViewModel.phoneNumber.value
        val birthdate = sharedViewModel.birthdate.value
        val profilePictureUrl = sharedViewModel.profilePictureUrl.value

        val userInfo = hashMapOf(
            "username" to username,
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phoneNumber,
            "birthdate" to birthdate
        )

        binding.progressContainer.visibility = View.VISIBLE
        if (profilePictureUrl != null) {
            uploadImageToFirebase(profilePictureUrl) { url ->
                userInfo["profilePictureUrl"] = url
                saveToFirestore(userRef, userInfo)
            }
        } else {
            saveToFirestore(userRef, userInfo)
        }
    }

    private fun saveToFirestore(userRef: DocumentReference, userInfo: HashMap<String, String?>) {
        userRef.set(userInfo)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Registration complete", Toast.LENGTH_SHORT).show()
                binding.progressContainer.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error updating user info: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressContainer.visibility = View.GONE
            }
    }

    private fun uploadImageToFirebase(imageUri: String, onSuccess: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_pictures/${FirebaseAuth.getInstance().currentUser?.uid}.jpg")
        val uploadTask = storageRef.putFile(Uri.parse(imageUri))

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            storageRef.downloadUrl
        }.addOnSuccessListener { uri ->
            onSuccess(uri.toString())
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
