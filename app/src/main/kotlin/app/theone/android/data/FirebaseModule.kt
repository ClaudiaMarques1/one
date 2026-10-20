package app.theone.android.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseModule {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Every per-user collection lives at users/{uid}/<name> - see .claude/rules/architecture.md.
    // Returns null when signed out, matching each repository's previous null-safe uid lookup.
    fun userCollection(name: String): CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection("users").document(uid).collection(name)
    }
}
