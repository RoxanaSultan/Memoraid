package com.example.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.memoraid.databinding.FragmentFullScreenImageBinding

class FullScreenImageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_full_screen_image, container, false)
        val imageView = view.findViewById<ImageView>(R.id.full_screen_image_view)

        val imageUri = arguments?.getString("image")
        Glide.with(this)
            .load(imageUri)
            .into(imageView)

        return view
    }
}
