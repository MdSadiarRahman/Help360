package com.example.help360

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.help360.models.ServiceProvider
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var rvServices: RecyclerView
    private lateinit var db: FirebaseFirestore
    private val servicesList = mutableListOf<Pair<String, ServiceProvider>>()
    private val categories = arrayOf("Police", "Ambulance", "Fire", "Hospital", "Blood")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        db = FirebaseFirestore.getInstance()
        rvServices = findViewById(R.id.rvServices)
        rvServices.layoutManager = LinearLayoutManager(this)
        
        // Initialize adapter early
        updateRecyclerView()

        findViewById<ImageButton>(R.id.btnLogout).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.fabAddService).setOnClickListener {
            showAddServiceDialog()
        }

        fetchServices()
        checkAndSeedDefaults()
    }

    private fun checkAndSeedDefaults() {
        db.collection("service_providers").limit(1).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    seedDefaultServices()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Rules Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun seedDefaultServices() {
        val defaults = listOf(
            // DHAKA
            ServiceProvider(name = "Dhaka Metropolitan Police", phone = "01320-040100", categoryName = "Police", district = "Dhaka"),
            ServiceProvider(name = "Dhaka Medical College Hospital", phone = "02-55165001", categoryName = "Hospital", district = "Dhaka"),
            ServiceProvider(name = "Dhaka Fire Service HQ", phone = "02-9555555", categoryName = "Fire", district = "Dhaka"),
            ServiceProvider(name = "Anwer Khan Modern Ambulance", phone = "01711-625173", categoryName = "Ambulance", district = "Dhaka"),
            
            // CHITTAGONG
            ServiceProvider(name = "CMP Headquarters", phone = "01320-050100", categoryName = "Police", district = "Chittagong"),
            ServiceProvider(name = "Chittagong Medical College", phone = "031-619400", categoryName = "Hospital", district = "Chittagong"),
            ServiceProvider(name = "Agrabad Fire Station", phone = "031-710333", categoryName = "Fire", district = "Chittagong"),
            
            // SYLHET
            ServiceProvider(name = "Sylhet Metropolitan Police", phone = "01320-060100", categoryName = "Police", district = "Sylhet"),
            ServiceProvider(name = "Sylhet MAG Osmani Hospital", phone = "0821-713300", categoryName = "Hospital", district = "Sylhet"),
            ServiceProvider(name = "Sylhet Fire Service", phone = "0821-716323", categoryName = "Fire", district = "Sylhet"),

            // RAJSHAHI
            ServiceProvider(name = "Rajshahi Police Line", phone = "01320-070100", categoryName = "Police", district = "Rajshahi"),
            ServiceProvider(name = "Rajshahi Medical Hospital", phone = "0721-772150", categoryName = "Hospital", district = "Rajshahi"),
            
            // NATIONAL HELPLINES
            ServiceProvider(name = "National Emergency Service", phone = "999", categoryName = "Helpline", district = "National"),
            ServiceProvider(name = "Women & Children Helpline", phone = "109", categoryName = "Helpline", district = "National"),
            ServiceProvider(name = "Health Window", phone = "16263", categoryName = "Helpline", district = "National"),
            ServiceProvider(name = "Disaster Management", phone = "1098", categoryName = "Helpline", district = "National")
        )

        val batch = db.batch()
        defaults.forEach { service ->
            val docRef = db.collection("service_providers").document()
            batch.set(docRef, service)
        }
        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "All Bangladesh Default services added!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Seed Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun fetchServices() {
        db.collection("service_providers")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                servicesList.clear()
                snapshot?.documents?.forEach { doc ->
                    val service = doc.toObject(ServiceProvider::class.java)
                    if (service != null) {
                        servicesList.add(doc.id to service)
                    }
                }
                rvServices.adapter?.notifyDataSetChanged()
            }
    }

    private fun updateRecyclerView() {
        rvServices.adapter = object : RecyclerView.Adapter<ServiceViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_service, parent, false)
                return ServiceViewHolder(view)
            }

            override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
                val (id, service) = servicesList[position]
                holder.tvName.text = "${service.name} (${service.categoryName})"
                holder.tvPhone.text = service.phone
                holder.btnDelete.setOnClickListener {
                    deleteService(id)
                }
            }

            override fun getItemCount() = servicesList.size
        }
    }

    private fun deleteService(id: String) {
        db.collection("service_providers").document(id).delete()
            .addOnSuccessListener { Toast.makeText(this, "Service Deleted", Toast.LENGTH_SHORT).show() }
    }

    private fun showAddServiceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_service, null)
        val dialog = AlertDialog.Builder(this, R.style.Theme_Help360_Dialog)
            .setView(dialogView)
            .create()

        val etName = dialogView.findViewById<EditText>(R.id.etServiceName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etServicePhone)
        val etLat = dialogView.findViewById<EditText>(R.id.etServiceLat)
        val etLng = dialogView.findViewById<EditText>(R.id.etServiceLng)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveService)

        // Setup Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val phone = etPhone.text.toString()
            val category = spinner.selectedItem.toString()
            val lat = etLat.text.toString().toDoubleOrNull()
            val lng = etLng.text.toString().toDoubleOrNull()

            if (name.isNotEmpty() && phone.isNotEmpty()) {
                val service = ServiceProvider(
                    name = name,
                    phone = phone,
                    categoryName = category,
                    latitude = lat,
                    longitude = lng
                )
                db.collection("service_providers").add(service)
                    .addOnSuccessListener {
                        dialog.dismiss()
                        Toast.makeText(this, "New Service Added", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Please fill name and phone", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvServiceName)
        val tvPhone: TextView = view.findViewById(R.id.tvServicePhone)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }
}
