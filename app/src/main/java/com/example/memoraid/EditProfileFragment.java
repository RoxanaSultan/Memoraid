package com.example.memoraid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.example.memoraid.databinding.FragmentEditProfileBinding;

public class EditProfileFragment extends Fragment {

    // Declare the binding object
    private FragmentEditProfileBinding binding;

    public EditProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout using View Binding
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);

        // Set up button listeners using the binding object
        binding.editProfileButton.setOnClickListener(v -> saveChanges());
        binding.logoutButton.setOnClickListener(v -> changePassword());

        return binding.getRoot(); // Return the root view of the binding
    }

    // Handle saving profile changes
    private void saveChanges() {
        String username = binding.changeUsername.getText().toString();
        String email = binding.changeEmail.getText().toString();
        String firstName = binding.changeFirstName.getText().toString();
        String lastName = binding.changeLastName.getText().toString();
        String phoneNumber = binding.changePhoneNumber.getText().toString();
        String birthdate = binding.changeBirthdate.getText().toString();


    }

    // Handle changing the password
    private void changePassword() {
        // You can implement password change functionality here
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up binding when the view is destroyed
        binding = null;
    }
}
