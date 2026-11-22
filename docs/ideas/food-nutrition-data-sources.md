# Feature Specification: Food & Nutrition Data Source (Ireland Focus)

**Project:** One (All-in-One Android Fitness, Nutrition, Diary & Calendar App)  
**Target Platform:** Android (Kotlin / Android Studio)  
**Current Backend:** Firebase Firestore  
**Status:** Backlog / Future Implementation
**Origin:** notes from a design conversation (2026-11-22), not yet decided. Board idea: "Meals: food lookup with calories and macros". Conflicts with repo rules (Retrofit, Room, a shared `foods` collection, ODbL licence) are listed on that card.

---

## 1. Objective
Implement a food and nutrition lookup system for the meal planning and food diary modules in **One** that provides **calories, protein, carbohydrates, and fats**, prioritizing **products available in Ireland**. The solution must avoid bloating local phone storage/RAM while allowing a clean migration path to a custom cloud database later.

---

## 2. Architectural Strategy (Phased Rollout)

### Phase 1: MVP - Live Open Food Facts Integration (No Bundled DB)
* **Data Source:** Connect directly via HTTP/REST to **Open Food Facts** (`https://ie.openfoodfacts.org` / `https://world.openfoodfacts.org`) rather than bundling a multi-gigabyte database inside the APK.
* **API Requirements:**
  * Include a custom `User-Agent` header on all requests: `One/1.0 (+https://github.com/ClaudiaMarques1/one)` - app name and the public repo URL, never a personal email.
  * Use `https://world.openfoodfacts.net` (Basic Auth: `off` / `off`) during development/staging.
* **Rate-Limit Protection:**
  * Open Food Facts limits requests (~10 req/min for search, ~15 req/min for product lookups).
  * **Do not** trigger API calls on every keystroke; use a search debounce or explicit search button submission.
* **Data Normalization:**
  * Map API responses immediately into an internal `FoodItem` domain model (`id`, `name`, `brand`, `country`, `servingGrams`, `calories`, `proteinG`, `carbsG`, `fatG`, `source`) to handle inconsistent crowd-sourced units (e.g., converting `kJ` to `kcal`).

### Phase 2: Migration / Hybrid - Custom Database on Existing Firestore
* **Zero Additional Infrastructure:** Leverage the existing Firebase Firestore setup rather than adding a separate database provider.
* **Storage & Memory Impact:**
  * Storing food documents in Firestore lives in the cloud and does **not** consume significant phone RAM or APK storage (text/numeric macro records are tiny; ~50k items take < 50 MB).
  * Main constraint to manage is **Firestore daily read quotas** (Spark free tier: 50,000 reads/day, 1 GiB storage).
* **Firestore Optimization Rules:**
  * Always cap search queries (e.g., `.limit(15)` or `.limit(20)`).
  * Use one-time `.get()` calls instead of real-time `addSnapshotListener` for static food catalog searches.
  * Keep documents compact (store macro numbers and metadata only; avoid heavy image blobs).
* **Hybrid Fallback Option:** Check custom Firestore `foods` collection first for curated/verified Irish items, and fall back to Open Food Facts for barcodes or unindexed products.

---

## 3. Core Design Requirements for Easy Migration
To ensure switching from Open Food Facts to Firestore (or combining both) requires zero UI rewrites:

1. **Repository Abstraction:**
   * All UI/ViewModels must communicate strictly with a `FoodRepository` interface (e.g., `searchFoods(query: String): List<FoodItem>`, `getFoodByBarcode(barcode: String): FoodItem?`).
   * Swap or chain `OpenFoodFactsDataSource` and `FirestoreFoodDataSource` inside the repository layer.
2. **Immutable Diary Snapshots:**
   * When a user logs a food item into their daily diary, store a snapshot of its name, serving size, and macros directly in the diary entry so past logs never break if external data sources change.
3. **Lightweight Local Cache (Recent/Favorites):**
   * Cache only recently searched or frequently logged items locally on the device so repeat logging is instant, works offline, and reduces API/Firestore read counts.

---

## 4. Suggested Backlog Ticket Breakdown
* **Ticket 1 (Architecture):** Define `FoodItem` domain model, `DiaryFoodEntry` snapshot model, and `FoodRepository` interface.
* **Ticket 2 (Phase 1 API):** Implement `OpenFoodFactsDataSource` (Retrofit/OkHttp) with required `User-Agent`, debounced search, barcode lookup, and `kJ` -> `kcal` normalization.
* **Ticket 3 (Caching):** Add lightweight local caching for recent/logged foods to minimize network calls and support offline re-logging.
* **Ticket 4 (Phase 2 Firestore):** Create curated `foods` collection in Firestore (focused on Irish staples & supermarket items) with `.limit()` `.get()` queries and implement `FirestoreFoodDataSource` behind `FoodRepository`.