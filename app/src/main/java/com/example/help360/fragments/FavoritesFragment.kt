package com.example.help360.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.help360.R
import com.example.help360.databinding.FragmentFavoritesBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class FavoriteContact(
    var id: String,
    var name: String,
    var phone: String
)

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    
    private var allFavorites = mutableListOf<FavoriteContact>()
    private var filteredFavorites = mutableListOf<FavoriteContact>()
    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadFavorites()
        setupRecyclerView()
        setupSearch()
        
        binding.addFavoriteFab.setOnClickListener {
            showContactDialog(null)
        }
    }

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(filteredFavorites, 
            onEdit = { showContactDialog(it) },
            onDelete = { deleteContact(it) }
        )
        binding.favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = favoritesAdapter
        }
        updateEmptyState()
    }

    private fun setupSearch() {
        binding.searchEditText.doAfterTextChanged { text ->
            val query = text.toString().lowercase()
            filteredFavorites.clear()
            if (query.isEmpty()) {
                filteredFavorites.addAll(allFavorites)
            } else {
                filteredFavorites.addAll(allFavorites.filter { 
                    it.name.lowercase().contains(query) || it.phone.contains(query)
                })
            }
            favoritesAdapter.notifyDataSetChanged()
            updateEmptyState()
        }
    }

    private fun showContactDialog(contact: FavoriteContact?) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_contact, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editContactName)
        val phoneInput = dialogView.findViewById<EditText>(R.id.editContactPhone)
        val titleText = dialogView.findViewById<TextView>(R.id.dialogTitle)

        if (contact != null) {
            titleText.text = "Edit Contact"
            nameInput.setText(contact.name)
            phoneInput.setText(contact.phone)
        }

        AlertDialog.Builder(context, R.style.Theme_Help360_Dialog)
            .setView(dialogView)
            .setPositiveButton(if (contact == null) "Add" else "Update") { _, _ ->
                val name = nameInput.text.toString()
                val phone = phoneInput.text.toString()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    if (contact == null) {
                        addContact(name, phone)
                    } else {
                        updateContact(contact.id, name, phone)
                    }
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addContact(name: String, phone: String) {
        val newContact = FavoriteContact(System.currentTimeMillis().toString(), name, phone)
        allFavorites.add(newContact)
        saveFavorites()
        refreshList()
    }

    private fun updateContact(id: String, name: String, phone: String) {
        val index = allFavorites.indexOfFirst { it.id == id }
        if (index != -1) {
            allFavorites[index].name = name
            allFavorites[index].phone = phone
            saveFavorites()
            refreshList()
        }
    }

    private fun deleteContact(contact: FavoriteContact) {
        AlertDialog.Builder(context, R.style.Theme_Help360_Dialog)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${contact.name}?")
            .setPositiveButton("Delete") { _, _ ->
                allFavorites.remove(contact)
                saveFavorites()
                refreshList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshList() {
        val query = binding.searchEditText.text.toString().lowercase()
        filteredFavorites.clear()
        if (query.isEmpty()) {
            filteredFavorites.addAll(allFavorites)
        } else {
            filteredFavorites.addAll(allFavorites.filter { 
                it.name.lowercase().contains(query) || it.phone.contains(query)
            })
        }
        favoritesAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        binding.emptyState.visibility = if (filteredFavorites.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun saveFavorites() {
        val sharedPref = requireActivity().getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
        val json = Gson().toJson(allFavorites)
        sharedPref.edit().putString("favorites_list", json).apply()
    }

    private fun loadFavorites() {
        val sharedPref = requireActivity().getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
        val json = sharedPref.getString("favorites_list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<FavoriteContact>>() {}.type
            allFavorites = Gson().fromJson(json, type)
        }
        filteredFavorites.clear()
        filteredFavorites.addAll(allFavorites)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class FavoritesAdapter(
        private val contacts: List<FavoriteContact>,
        private val onEdit: (FavoriteContact) -> Unit,
        private val onDelete: (FavoriteContact) -> Unit
    ) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val initialsText: TextView = view.findViewById(R.id.initialsText)
            val nameText: TextView = view.findViewById(R.id.contactName)
            val phoneText: TextView = view.findViewById(R.id.contactPhone)
            val editBtn: View = view.findViewById(R.id.editBtn)
            val deleteBtn: View = view.findViewById(R.id.deleteBtn)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite_contact, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val contact = contacts[position]
            holder.nameText.text = contact.name
            holder.phoneText.text = contact.phone
            holder.initialsText.text = if (contact.name.isNotEmpty()) contact.name.take(2).uppercase() else "?"
            
            holder.editBtn.setOnClickListener { onEdit(contact) }
            holder.deleteBtn.setOnClickListener { onDelete(contact) }
        }

        override fun getItemCount() = contacts.size
    }
}
