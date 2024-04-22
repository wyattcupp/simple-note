package edu.cs371m.project.simplenote

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.recyclerview.widget.LinearLayoutManager
import edu.cs371m.project.simplenote.auth.AuthUser
import edu.cs371m.project.simplenote.data.models.Folder
import edu.cs371m.project.simplenote.databinding.ActivityMainBinding
import edu.cs371m.project.simplenote.ui.FoldersListAdapter
import edu.cs371m.project.simplenote.ui.MainViewModel
import edu.cs371m.project.simplenote.ui.NotesFragmentDirections

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private val viewModel: MainViewModel by viewModels()
    private lateinit var authUser: AuthUser
    private lateinit var foldersListAdapter: FoldersListAdapter

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Set everything up
        setupNavigation()
        setupAuthUser()
        setupFoldersAdapter()
        setupCreateFolderListener()
        observeSelectedFolder()
        observeLiveMessage()

        binding.defaultFolder.setOnClickListener {
            val defaultFolderId = viewModel.defaultFolderId
            if (defaultFolderId != null) {
                viewModel.selectedFolder.value = Folder(id = defaultFolderId, name = "Default")
                navigateToNotesFragment(defaultFolderId)
                drawerLayout.closeDrawers()
            } else {
                Toast.makeText(this, "Default folder not set.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun navigateToNotesFragment(folderId: String?) {
        folderId?.let {
            val action = NotesFragmentDirections.actionNotesFragmentSelf(it)
            navController.navigate(action)
        }
    }

    // Creates listener for create folder button
    private fun setupCreateFolderListener() {
        binding.createFolder.setOnClickListener {
            val newFolder = Folder(
                name = "New Folder",
                createdBy = viewModel.getCurrentUserId() ?: "defaultUser"
            )
            viewModel.addFolder(newFolder)
            Toast.makeText(this, "Creating new folder...", Toast.LENGTH_SHORT).show()
        }

    }

    // Observes and updates toolbar title based on live data folder name
    private fun observeSelectedFolder() {
        viewModel.selectedFolder.observe(this) { folder ->
            supportActionBar?.title = folder?.name ?: "Notes"
        }
    }

    // Observes the live message when view model posts a value.
    private fun observeLiveMessage() {
        viewModel.messageLiveData.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                viewModel.messageLiveData.value = null  // Reset message to prevent repeated toasts
            }
        }
    }

    // Sets up navigation
    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        drawerLayout = binding.drawerLayout

        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        NavigationUI.setupWithNavController(binding.navigationView, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.editNoteFragment -> supportActionBar?.title = "Edit Note"
                R.id.notesFragment -> supportActionBar?.title =
                    viewModel.selectedFolder.value?.name ?: "Notes"
            }
        }
    }

    // Sets up auth user / observers for both view model and main
    private fun setupAuthUser() {
        authUser = AuthUser(activityResultRegistry)
        viewModel.initializeAuthUser(authUser)
        lifecycle.addObserver(authUser)
        authUser.observeUser().observe(this) { user ->
            if (user != null) {
                Log.d(TAG, "User logged in: ${user.uid}")
                updateNavHeader(user)
            } else {
                Log.d(TAG, "No user logged in.")
            }
        }
    }

    // Sets up folders adapter since we aren't doing a folders fragment class
    private fun setupFoldersAdapter() {
        Log.d(TAG, "setupFolderAdapter")
        foldersListAdapter = FoldersListAdapter { folder ->
            Toast.makeText(this, "Folder clicked: ${folder.name}", Toast.LENGTH_SHORT).show()
            viewModel.selectedFolder.value = folder

            // Navigate to NotesFragment with folder.id as an argument
            val bundle = Bundle()
            bundle.putString("folderId", folder.id)
            navController.navigate(R.id.notesFragment, bundle)

            drawerLayout.closeDrawers()
        }

        binding.foldersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.foldersRecyclerView.adapter = foldersListAdapter

        viewModel.foldersLiveData.observe(this) { folders ->
            foldersListAdapter.submitList(folders)
        }
    }

    // Updates the navigation header based on current user.
    private fun updateNavHeader(user: edu.cs371m.project.simplenote.auth.User) {
        val headerView = binding.navigationView.getHeaderView(0)
        val userNameText = headerView.findViewById<TextView>(R.id.userName)
        val userEmailText = headerView.findViewById<TextView>(R.id.userEmail)

        // check if a user full-name exists, if not, set it to nothing
        userNameText.text = if (user.name == "User logged Out") "" else user.name
        userEmailText.text = user.email
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
