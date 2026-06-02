package com.example.help360.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.help360.databinding.FragmentSosBinding
import com.example.help360.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SosFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLocationListener()

        binding.cancelSosBtn.setOnClickListener {
            findNavController().navigate(R.id.navigation_home)
        }
    }

    private fun setupLocationListener() {
        val user = auth.currentUser ?: return
        
        // Listen to the profile document for live location updates stored by the service
        db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                if (snapshot != null && snapshot.exists()) {
                    val location = snapshot.getString("current_location")
                    val message = snapshot.getString("customSOSMessage") ?: "Emergency! I need help at my current location."
                    
                    binding.liveCoords.text = location ?: "Calibrating..."
                    binding.sosMessageDisplay.text = message
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
