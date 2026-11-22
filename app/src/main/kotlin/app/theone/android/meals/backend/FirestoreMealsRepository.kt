package app.theone.android.meals.backend

import app.theone.android.data.FirebaseModule
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreMealsRepository : MealsRepository {
    private fun meals(): CollectionReference? = FirebaseModule.userCollection("meals")

    private fun favourites(): CollectionReference? = FirebaseModule.userCollection("favourites")

    private fun water(): CollectionReference? = FirebaseModule.userCollection("water")

    private fun mealDays(): CollectionReference? = FirebaseModule.userCollection("mealDays")

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

    override fun observeMealDates(from: String, to: String): Flow<Set<String>> = callbackFlow {
        val collection = meals()
        if (collection == null) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }
        // A range on the one `date` field needs no composite index; ISO keys sort as text.
        val registration = collection
            .whereGreaterThanOrEqualTo("date", from)
            .whereLessThanOrEqualTo("date", to)
            .limit(300)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.getString("date") }?.toSet() ?: emptySet())
            }
        awaitClose { registration.remove() }
    }

    override fun observeMealsBetween(from: String, to: String): Flow<List<PlannedMeal>> = callbackFlow {
        val collection = meals()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = collection
            .whereGreaterThanOrEqualTo("date", from)
            .whereLessThanOrEqualTo("date", to)
            .limit(300)
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

    override fun observeRestDayChoices(from: String, to: String): Flow<Map<String, Boolean>> = callbackFlow {
        val collection = mealDays()
        if (collection == null) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }
        // mealDays documents are keyed by date, so the range runs on the document id.
        val registration = collection
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), from)
            .whereLessThanOrEqualTo(FieldPath.documentId(), to)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(
                    snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(MealDayChoice::class.java)?.restDay?.let { doc.id to it }
                    }?.toMap() ?: emptyMap()
                )
            }
        awaitClose { registration.remove() }
    }

    override suspend fun deleteMeals(ids: List<String>) {
        val collection = meals() ?: return
        if (ids.isEmpty()) return
        val batch = FirebaseModule.firestore.batch()
        ids.filter { it.isNotEmpty() }.forEach { batch.delete(collection.document(it)) }
        batch.commit().await()
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

    override fun observeRestDayChoice(date: String): Flow<Boolean?> = callbackFlow {
        val collection = mealDays()
        if (collection == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = collection.document(date).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(MealDayChoice::class.java)?.restDay)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun setRestDayChoice(date: String, restDay: Boolean?) {
        val doc = mealDays()?.document(date) ?: return
        if (restDay == null) doc.delete().await() else doc.set(MealDayChoice(restDay)).await()
    }

    override suspend fun setWaterGlasses(date: String, glasses: Int) {
        water()?.document(date)?.set(WaterLog(glasses))?.await()
    }
}
