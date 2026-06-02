package com.example.help360.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.help360.R
import com.example.help360.databinding.FragmentMapBinding
import com.example.help360.models.ServiceProvider
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val allServices = mutableListOf<ServiceProvider>()
    private val filteredList = mutableListOf<ServiceProvider>()
    
    private var selectedCategory = "ALL"
    private var selectedDistrict = "All Regions"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.rvDirectory.layoutManager = LinearLayoutManager(context)
        
        // Handle incoming category from HomeFragment
        arguments?.getString("selected_category")?.let { category ->
            selectedCategory = category.uppercase()
        }

        setupLanguageToggle()
        setupSearch()
        setupFilters()
        
        // Sync Chip UI with selectedCategory
        updateChipSelection()
        
        fetchDirectoryData()
    }

    private fun updateChipSelection() {
        when (selectedCategory) {
            "ALL" -> binding.chipAll.isChecked = true
            "POLICE" -> binding.chipPolice.isChecked = true
            "AMBULANCE" -> binding.chipAmbulance.isChecked = true
            "FIRE" -> binding.chipFire.isChecked = true
            "HOSPITAL" -> binding.chipHospital.isChecked = true
            "HELPLINE" -> binding.chipHelpline.isChecked = true
            "BLOOD" -> binding.chipBlood.isChecked = true
            "LEGAL" -> binding.chipLegal.isChecked = true
        }
    }

    private fun setupFilters() {
        // Filter Visibility Toggle
        binding.btnFilterToggle.setOnClickListener {
            binding.filterSection.visibility = if (binding.filterSection.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Category Selection using new CheckedStateChangeListener
        binding.chipGroupCategory.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds[0])
                if (chip != null) {
                    selectedCategory = chip.text.toString().uppercase()
                    applyFilters()
                }
            }
        }

        // District Spinner
        val districts = arrayOf("All Regions", "Dhaka", "Chittagong", "Rajshahi", "Khulna", "Barisal", "Sylhet", "Rangpur", "Mymensingh", "National")
        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, districts) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as TextView).setTextColor(android.graphics.Color.WHITE)
                return v
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                v.setBackgroundColor(android.graphics.Color.parseColor("#171717"))
                (v as TextView).setTextColor(android.graphics.Color.WHITE)
                v.setPadding(32, 32, 32, 32)
                return v
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDistrict.adapter = adapter

        binding.spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDistrict = districts[position]
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun applyFilters() {
        val searchText = binding.etSearch.text.toString().lowercase()
        filteredList.clear()
        
        for (item in allServices) {
            val matchesSearch = item.name.lowercase().contains(searchText) || 
                               item.phone.contains(searchText)
            
            val matchesCategory = selectedCategory == "ALL" || 
                                 item.categoryName.uppercase() == selectedCategory
            
            val matchesDistrict = selectedDistrict == "All Regions" || 
                                 item.district.lowercase() == selectedDistrict.lowercase()

            if (matchesSearch && matchesCategory && matchesDistrict) {
                filteredList.add(item)
            }
        }
        
        if (binding.rvDirectory.adapter != null) {
            binding.rvDirectory.adapter!!.notifyDataSetChanged()
        }
    }

    private fun setupLanguageToggle() {
        binding.btnLanguage.setOnClickListener {
            Toast.makeText(context, "Language switcher coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchDirectoryData() {
        allServices.clear()
        // Add hardcoded national helplines
        allServices.add(ServiceProvider("id_999", "National Emergency", "999", "ALL", "Bangladesh", "National"))
        allServices.add(ServiceProvider("id_333", "National Information", "333", "HELPLINE", "Bangladesh", "National"))
        allServices.add(ServiceProvider("id_16263", "Health Helpline", "16263", "HOSPITAL", "Bangladesh", "National"))
        allServices.add(ServiceProvider("id_109", "Women & Children", "109", "HELPLINE", "Bangladesh", "National"))
        allServices.add(ServiceProvider("id_1098", "Child Helpline", "1098", "HELPLINE", "Bangladesh", "National"))
        allServices.add(ServiceProvider("id_106", "Anti-Corruption", "106", "POLICE", "Bangladesh", "National"))
        allServices.add(ServiceProvider("id_16430", "Legal Aid", "16430", "HELPLINE", "Bangladesh", "National"))
        allServices.add(ServiceProvider("id_01766699999", "Cyber Help", "01766699999", "POLICE", "Bangladesh", "National"))

        // Regional Hospitals
        // Dhaka
        allServices.add(ServiceProvider("h_dmc", "Dhaka Medical College", "02-55165088", "HOSPITAL", "Dhaka", "Dhaka"))
        allServices.add(ServiceProvider("h_bsmmu", "BSMMU (PG Hospital)", "02-9661068", "HOSPITAL", "Dhaka", "Dhaka"))
        allServices.add(ServiceProvider("h_suhrawardy", "Suhrawardy Hospital", "02-9130800", "HOSPITAL", "Dhaka", "Dhaka"))
        
        // Chittagong
        allServices.add(ServiceProvider("h_cmc", "Chittagong Medical College", "031-619400", "HOSPITAL", "Chittagong", "Chittagong"))
        allServices.add(ServiceProvider("h_ustc", "BMBH (USTC) Chittagong", "031-659070", "HOSPITAL", "Chittagong", "Chittagong"))

        // Rajshahi
        allServices.add(ServiceProvider("h_rmc", "Rajshahi Medical College", "0721-772150", "HOSPITAL", "Rajshahi", "Rajshahi"))

        // Khulna
        allServices.add(ServiceProvider("h_kmc", "Khulna Medical College", "041-760350", "HOSPITAL", "Khulna", "Khulna"))

        // Sylhet
        allServices.add(ServiceProvider("h_mag_osmani", "MAG Osmani Medical College", "0821-713667", "HOSPITAL", "Sylhet", "Sylhet"))

        // Barisal
        allServices.add(ServiceProvider("h_sbmch", "Sher-e-Bangla Medical College", "0431-2173500", "HOSPITAL", "Barisal", "Barisal"))

        // Rangpur
        allServices.add(ServiceProvider("h_rpmc", "Rangpur Medical College", "0521-65000", "HOSPITAL", "Rangpur", "Rangpur"))

        // Mymensingh
        allServices.add(ServiceProvider("h_mmc", "Mymensingh Medical College", "091-66063", "HOSPITAL", "Mymensingh", "Mymensingh"))

        // Show local data immediately
        applyFilters()
        updateRecyclerView()

        db.collection("service_providers")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val service = doc.toObject(ServiceProvider::class.java)
                    if (service != null && allServices.none { it.id == service.id }) {
                        allServices.add(service)
                    }
                }
                applyFilters()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Cloud data unavailable, showing local contacts", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRecyclerView() {
        binding.rvDirectory.adapter = object : RecyclerView.Adapter<DirectoryViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectoryViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_directory_service, parent, false)
                return DirectoryViewHolder(view)
            }

            override fun onBindViewHolder(holder: DirectoryViewHolder, position: Int) {
                val service = filteredList[position]
                holder.tvName.text = service.name.uppercase()
                holder.tvSubInfo.text = "${service.categoryName.uppercase()} • ${service.district.uppercase()}"
                
                holder.btnCall.setOnClickListener {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${service.phone}"))
                    startActivity(intent)
                }

                holder.btnFavorite.setOnClickListener {
                    Toast.makeText(context, "Added to Favorites", Toast.LENGTH_SHORT).show()
                }
            }

            override fun getItemCount() = filteredList.size
        }
    }

    class DirectoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvSubInfo: TextView = view.findViewById(R.id.tvSubInfo)
        val btnCall: View = view.findViewById(R.id.btnCall)
        val btnFavorite: ImageButton = view.findViewById(R.id.btnFavorite)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
