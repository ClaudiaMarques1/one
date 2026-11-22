---
name: android-conventions
description: Detailed Kotlin/Compose/Firestore conventions for this app — repository read/write shape, ViewModel state pattern, error handling, model shape, naming. Auto-loads when editing .kt files under app/src/main, especially a ViewModel, Repository, or Screen composable. Consult before adding a new ViewModel, Repository, or Firestore model.
---

# Android Conventions

Expanded reference for the invariants in `.claude/rules/architecture.md` — read that first for the
short version; this is the "how exactly" version with runnable-shaped examples.

## New Repository

```kotlin
class FooRepository {
    private fun uid() = FirebaseModule.auth.currentUser?.uid

    fun observeFoos(): Flow<List<Foo>> = callbackFlow {
        val uid = uid() ?: run { close(); return@callbackFlow }
        val reg = FirebaseModule.firestore
            .collection("users/$uid/foos")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents.orEmpty().mapNotNull {
                    it.toObject(Foo::class.java)?.copy(id = it.id)
                })
            }
        awaitClose { reg.remove() }
    }

    suspend fun addFoo(foo: Foo) {
        val uid = uid() ?: return
        FirebaseModule.firestore.collection("users/$uid/foos").add(foo).await()
    }
}
```

## New ViewModel

```kotlin
class FooViewModel(
    private val repository: FooRepository = FooRepository()
) : ViewModel() {
    val foos: StateFlow<List<Foo>> = repository.observeFoos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _fooError = MutableStateFlow<String?>(null)
    val fooError: StateFlow<String?> = _fooError

    fun addFoo(foo: Foo) = viewModelScope.launch {
        runCatching { repository.addFoo(foo) }
            .onFailure { _fooError.value = it.message }
    }
}
```

## New Firestore Model

All fields defaulted, `id` defaulted too (filled in by the repository, not the constructor call site):

```kotlin
data class Foo(
    val name: String = "",
    val createdAt: Long = 0L,
    val id: String = "",
)
```

## Composables

Render only — no repository or ViewModel construction inside a `@Composable` beyond
`viewModel()`/hoisted state. Screen functions and any composable are `PascalCase`; members and
locals are `lowerCamelCase`.

## Multi-write operations

If a single user action needs more than one Firestore write (e.g. saving a session *and* updating
a template), use `firestore.runTransaction { }` or a `WriteBatch` — don't issue two independent
sequential `.await()` calls for what's logically one atomic operation.

## New Screen + Navigation

Three-step registration (see `.claude/rules/architecture.md`):

```kotlin
// 0. ui/theme/Color.kt - add the section accent with its light and dark value
//    enum class SectionAccent { ...; Foo(Color(0xFF00ACC1), Color(0xFF<dark>)) }

// 1. navigation/Destination.kt — add a data object
data object Foo : Destination("foo", "Foo", Icons.Outlined.SomeIcon, Icons.Filled.SomeIcon, SectionAccent.Foo)

// 2. navigation/Destination.kt — add it to the list
val bottomNavDestinations = listOf(Destination.Home, /* ... */, Destination.Foo)

// 3. navigation/OneNavHost.kt — add a composable entry inside NavHost
composable(Destination.Foo.route) {
    SectionTheme(SectionAccent.Foo) { FooScreen() }
}
```

## New manifest permission

If you add a dangerous/runtime permission, add the actual runtime request
(`ActivityResultContracts.RequestPermission` + `checkSelfPermission`) in the same change — a
manifest declaration alone does nothing on API 23+.
