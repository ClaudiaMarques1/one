package app.theone.android.meals.backend

import app.theone.android.data.FirebaseModule
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreMealsRepository : MealsRepository {
    private fun meals(): CollectionReference? = FirebaseModule.userCollection("meals")

    private fun favourites(): CollectionReference? = FirebaseModule.userCollection("favourites")

    private fun water(): CollectionReference? = FirebaseModule.userCollection("water")

    override fun observeMeals(date: String): Flow<List<PlannedMeal>> = callbackFlow {
        val collection = meals()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        // Equality filter only, sorted on the client: no composite index needed.
        val registration = collection
            .whereEqualTo("date", date)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PlannedMeal::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list.sortedBy { it.createdAt })
            }
        awaitClose { registration.remove() }
    }

    override fun observeFavourites(): Flow<List<FavouriteMeal>> = callbackFlow {
        val collection = favourites()
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
                        doc.toObject(FavouriteMeal::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                )
            }
        awaitClose { registration.remove() }
    }

    override fun observeWaterGlasses(date: String): Flow<Int> = callbackFlow {
        val collection = water()
        if (collection == null) {
            trySend(0)
            close()
            return@callbackFlow
        }
        val registration = collection.document(date).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(WaterLog::class.java)?.glasses ?: 0)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addMeals(meals: List<PlannedMeal>, newFavourite: FavouriteMeal?) {
        val mealCollection = meals() ?: return
        val batch = FirebaseModule.firestore.batch()
        meals.forEach { batch.set(mealCollection.document(), it) }
        if (newFavourite != null) {
            favourites()?.let { batch.set(it.document(), newFavourite) }
        }
        batch.commit().await()
    }

    override suspend fun setEaten(id: String, eaten: Boolean) {
        if (id.isEmpty()) return
        meals()?.document(id)?.update("eaten", eaten)?.await()
    }

    override suspend fun deleteMeal(id: String) {
        if (id.isEmpty()) return
        meals()?.document(id)?.delete()?.await()
    }

    override suspend fun setWaterGlasses(date: String, glasses: Int) {
        water()?.document(date)?.set(WaterLog(glasses))?.await()
    }
}
