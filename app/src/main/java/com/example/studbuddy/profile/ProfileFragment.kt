package com.example.studbuddy.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.studbuddy.R
import com.example.studbuddy.core.models.AuthStatus
import com.example.studbuddy.core.workers.SyncWorker
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var imgProfile: ShapeableImageView
    private lateinit var editDisplayName: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnChangePhoto: MaterialButton
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var btnSignOut: MaterialButton

    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            imgProfile.setImageURI(it)
        }
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(context, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgProfile = view.findViewById(R.id.imgProfile)
        editDisplayName = view.findViewById(R.id.editDisplayName)
        btnSave = view.findViewById(R.id.btnSave)
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto)
        btnGoogleSignIn = view.findViewById(R.id.btnGoogleSignIn)
        btnSignOut = view.findViewById(R.id.btnSignOut)

        lifecycleScope.launch {
            viewModel.userState.collect { user ->
                user ?: return@collect
                editDisplayName.setText(user.displayName)
                
                imgProfile.load(user.profileImageUri) {
                    crossfade(true)
                    placeholder(R.drawable.ic_person_24)
                    error(R.drawable.ic_person_24)
                    transformations(CircleCropTransformation())
                }
                
                if (user.authStatus == AuthStatus.SIGNED_IN) {
                    btnGoogleSignIn.visibility = View.GONE
                    btnSignOut.visibility = View.VISIBLE
                } else {
                    btnGoogleSignIn.visibility = View.VISIBLE
                    btnSignOut.visibility = View.GONE
                }
            }
        }

        btnGoogleSignIn.setOnClickListener {
            signIn()
        }

        btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            viewModel.signOut()
            Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
        }

        btnChangePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            val newName = editDisplayName.text.toString()
            if (newName.isBlank()) {
                Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.updateDisplayName(newName)
            selectedImageUri?.let {
                viewModel.updateProfileImage(it.toString())
            }
            Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        lifecycleScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    viewModel.setUser(
                        com.example.studbuddy.core.models.User(
                            id = firebaseUser.uid,
                            displayName = firebaseUser.displayName ?: "User",
                            email = firebaseUser.email,
                            profileImageUri = firebaseUser.photoUrl?.toString(),
                            authStatus = AuthStatus.SIGNED_IN
                        )
                    )
                    // Trigger an immediate sync now that the user is signed in
                    val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()
                    WorkManager.getInstance(requireContext()).enqueue(syncRequest)
                    Toast.makeText(context, "Welcome ${firebaseUser.displayName}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
