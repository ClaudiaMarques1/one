package app.theone.android.meals.backend

import app.theone.android.data.FirebaseModule
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRecipesRepository : RecipesRepository {
    private fun recipes(): CollectionReference? = FirebaseModule.userCollection("recipes")

    private fun shopping(): CollectionReference? = FirebaseModule.userCollection("shopping")

    override fun observeRecipes(): Flow<List<Recipe>> = callbackFlow {
        val collection = recipes()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = collection
            .orderBy("name", Query.Direction.ASCENDING)
            .limit(300)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(
                    snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Recipe::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                )
            }
        awaitClose { registration.remove() }
    }

    override suspend fun saveRecipe(recipe: Recipe) {
        val collection = recipes() ?: return
        if (recipe.id.isEmpty()) {
            collection.add(recipe).await()
        } else {
            collection.document(recipe.id).set(recipe).await()
        }
    }

    override suspend fun deleteRecipe(id: String) {
        if (id.isEmpty()) return
        recipes()?.document(id)?.delete()?.await()
    }

    override fun observeShoppingState(weekStart: String): Flow<ShoppingState> = callbackFlow {
        val collection = shopping()
        if (collection == null) {
            trySend(ShoppingState())
            close()
            return@callbackFlow
        }
        val registration = collection.document(weekStart).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(ShoppingState::class.java) ?: ShoppingState())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun setShoppingState(weekStart: String, state: ShoppingState) {
        shopping()?.document(weekStart)?.set(state)?.await()
    }
}
