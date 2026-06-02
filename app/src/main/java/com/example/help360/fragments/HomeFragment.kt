package com.example.help360.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.help360.R
import com.example.help360.databinding.FragmentHomeBinding
import com.example.help360.models.ServiceProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var sosTimer: CountDownTimer? = null
    private var isHolding = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupSosButton()
    }

    private fun setupClickListeners() {
        binding.btnPolice.setOnClickListener { navigateToDirectory("POLICE") }
        binding.btnAmbulance.setOnClickListener { navigateToDirectory("AMBULANCE") }
        binding.btnFire.setOnClickListener { navigateToDirectory("FIRE") }
        binding.btnHospital.setOnClickListener { navigateToDirectory("HOSPITAL") }
        binding.btnHelpline.setOnClickListener { navigateToDirectory("HELPLINE") }
        binding.btnWomenChildren.setOnClickListener { navigateToDirectory("HELPLINE") }
        binding.btnInfo333.setOnClickListener { navigateToDirectory("HELPLINE") }

        binding.directoryBtn.setOnClickListener {
            navigateToDirectory("ALL")
        }

        binding.refreshButton.setOnClickListener {
            fetchNearestService()
        }
    }

    private fun navigateToDirectory(category: String) {
        val bundle = Bundle().apply {
            putString("selected_category", category)
        }
        findNavController().navigate(R.id.navigation_map, bundle)
    }

    private fun makeCall(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        startActivity(intent)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSosButton() {
        binding.homeSosBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startSosCounter()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    resetSosCounter()
                    true
                }
                else -> false
            }
        }
    }

    private fun startSosCounter() {
        if (isHolding) return
        isHolding = true
        
        binding.sosProgressBar.visibility = View.VISIBLE
        binding.sosProgressBar.progress = 0
        
        // Scale Animation
        val scaleAnim = ScaleAnimation(
            1f, 1.05f, 1f, 1.05f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 500
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        binding.homeSosBtn.startAnimation(scaleAnim)

        sosTimer = object : CountDownTimer(3000, 30) {
            override fun onTick(millisUntilFinished: Long) {
                val progress = ((3000 - millisUntilFinished) / 30).toInt()
                binding.sosProgressBar.progress = progress
                val secondsLeft = ceil(millisUntilFinished / 1000.0).toInt()
                binding.holdHintText.text = "RELEASE TO CANCEL ($secondsLeft s)"
            }

            override fun onFinish() {
                triggerSos()
            }
        }.start()
    }

    private fun resetSosCounter() {
        isHolding = false
        sosTimer?.cancel()
        binding.sosProgressBar.visibility = View.INVISIBLE
        binding.sosProgressBar.progress = 0
        binding.homeSosBtn.clearAnimation()
        binding.holdHintText.text = "HOLD FOR 3 SECONDS"
    }

    private fun triggerSos() {
        val user = auth.currentUser ?: return
        
        // Fetch user's custom message first
        db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            val customMsg = doc.getString("customSOSMessage") ?: "Emergency! I need help."
            
            val sosRequest = hashMapOf(
                "user_id" to user.uid,
                "user_name" to (doc.getString("name") ?: "Unknown"),
                "message" to customMsg,
                "request_time" to System.currentTimeMillis(),
                "latitude" to 23.8103, // Real GPS would be here
                "longitude" to 90.4125,
                "status" to "Active"
            )
            
            db.collection("sos_requests")
                .add(sosRequest)
                .addOnSuccessListener {
                    Toast.makeText(context, "SOS SIGNAL SENT!", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.navigation_sos)
                }
        }
    }

    private fun fetchNearestService() {
        // Mocking user location for demo if GPS is off, in real app we get from location provider
        val userLat = 23.8103
        val userLng = 90.4125

        db.collection("service_providers")
            .get()
            .addOnSuccessListener { snapshot ->
                var minDistance = Double.MAX_VALUE
                var nearest: ServiceProvider? = null

                for (doc in snapshot.documents) {
                    val lat = doc.getDouble("latitude")
                    val lng = doc.getDouble("longitude")
                    if (lat != null && lng != null) {
                        val dist = calculateDistance(userLat, userLng, lat, lng)
                        if (dist < minDistance) {
                            minDistance = dist
                            nearest = doc.toObject(ServiceProvider::class.java)
                        }
                    }
                }

                nearest?.let { service ->
                    binding.nearestServiceCard.visibility = View.VISIBLE
                    binding.nearestName.text = service.name
                    binding.nearestDistance.text = String.format("%.1f km away", minDistance)
                    binding.callNearestBtn.setOnClickListener {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${service.phone}"))
                        startActivity(intent)
                    }
                }
            }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
