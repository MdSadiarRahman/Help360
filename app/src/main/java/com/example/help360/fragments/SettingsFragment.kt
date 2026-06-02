package com.example.help360.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.help360.LoginActivity
import com.example.help360.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserData()
        
        binding.saveProfileBtn.setOnClickListener {
            saveUserData()
        }

        binding.profileImageCard.setOnClickListener {
            Toast.makeText(context, "Select photo feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        binding.logoutBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: "GUEST USER"
                    val phone = document.getString("phone") ?: ""
                    val address = document.getString("address") ?: ""
                    val sosMessage = document.getString("customSOSMessage") ?: "Emergency! I need help at my current location."

                    binding.userNameDisplay.text = name
                    binding.editName.setText(name)
                    binding.editPhone.setText(phone)
                    binding.editAddress.setText(address)
                    binding.editSosMessage.setText(sosMessage)
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserData() {
        val userId = auth.currentUser?.uid ?: return
        val name = binding.editName.text.toString()
        val phone = binding.editPhone.text.toString()
        val address = binding.editAddress.text.toString()
        val sosMessage = binding.editSosMessage.text.toString()

        if (name.isEmpty()) {
            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val userUpdates = hashMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "address" to address,
            "customSOSMessage" to sosMessage
        )

        db.collection("users").document(userId).update(userUpdates)
            .addOnSuccessListener {
                binding.userNameDisplay.text = name
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // If update fails, try setting the data (in case it's a first-time update for Google users)
                db.collection("users").document(userId).set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        binding.userNameDisplay.text = name
                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
