package com.example.workman.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Dynamic Category Repository — Single source of truth for all job/worker categories.
 *
 * Categories are fetched from Firestore `config/categories` document so you can
 * add/remove/reorder categories without releasing a new app version.
 *
 * Fallback: If Firestore is unreachable, uses a comprehensive local default list
 * designed for blue-collar / daily-wage workers.
 *
 * Architecture:
 * - Firestore doc: config/categories
 *   {
 *     "groups": [
 *       { "name": "Construction", "categories": ["Mason", "Painter", "Carpenter", ...] },
 *       ...
 *     ],
 *     "flatList": ["Mason", "Painter", "Carpenter", ...],   // auto-generated for dropdowns
 *     "similarityPairs": [
 *       { "a": "Mason", "b": "Painter", "score": 0.5 },
 *       ...
 *     ]
 *   }
 */
object CategoryRepository {

    private const val TAG = "CategoryRepository"

    // ── State ──────────────────────────────────────────────────────────────────

    private val _categories = MutableStateFlow<List<String>>(getDefaultFlatList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _categoryGroups = MutableStateFlow<List<CategoryGroup>>(getDefaultGroups())
    val categoryGroups: StateFlow<List<CategoryGroup>> = _categoryGroups.asStateFlow()

    private val _similarityMap =
        MutableStateFlow<Map<Pair<String, String>, Double>>(buildDefaultSimilarityMap())
    val similarityMap: StateFlow<Map<Pair<String, String>, Double>> = _similarityMap.asStateFlow()

    private var isLoaded = false

    // ── Data Classes ───────────────────────────────────────────────────────────

    data class CategoryGroup(
        val name: String,
        val categories: List<String>,
        val icon: String = "" // optional emoji or icon name
    )

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Call this once at app startup (e.g., in Application.onCreate or first Activity).
     * Loads categories from Firestore. If it fails, defaults are already set.
     */
    suspend fun loadFromFirestore() {
        if (isLoaded) return

        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("config")
                .document("categories")
                .get()
                .await()

            if (doc.exists()) {
                // Parse groups
                @Suppress("UNCHECKED_CAST")
                val groupsRaw = doc.get("groups") as? List<Map<String, Any>>
                if (groupsRaw != null) {
                    val groups = groupsRaw.mapNotNull { map ->
                        val name = map["name"] as? String ?: return@mapNotNull null

                        @Suppress("UNCHECKED_CAST")
                        val cats = map["categories"] as? List<String> ?: return@mapNotNull null
                        val icon = map["icon"] as? String ?: ""
                        CategoryGroup(name = name, categories = cats, icon = icon)
                    }
                    if (groups.isNotEmpty()) {
                        _categoryGroups.value = groups
                    }
                }

                // Parse flat list (or derive from groups)
                @Suppress("UNCHECKED_CAST")
                val flatList = doc.get("flatList") as? List<String>
                if (flatList != null && flatList.isNotEmpty()) {
                    _categories.value = flatList
                } else {
                    // Derive from groups
                    _categories.value = _categoryGroups.value.flatMap { it.categories }
                }

                // Parse similarity pairs
                @Suppress("UNCHECKED_CAST")
                val pairsRaw = doc.get("similarityPairs") as? List<Map<String, Any>>
                if (pairsRaw != null) {
                    val map = mutableMapOf<Pair<String, String>, Double>()
                    for (pair in pairsRaw) {
                        val a = pair["a"] as? String ?: continue
                        val b = pair["b"] as? String ?: continue
                        val score = (pair["score"] as? Number)?.toDouble() ?: continue
                        map[a to b] = score
                        map[b to a] = score // symmetric
                    }
                    if (map.isNotEmpty()) {
                        _similarityMap.value = map
                    }
                }

                isLoaded = true
                Log.d(TAG, "Loaded ${_categories.value.size} categories from Firestore")
            } else {
                // Document doesn't exist yet — seed it with defaults
                seedFirestore()
                isLoaded = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load categories from Firestore, using defaults", e)
            // Defaults are already set, so the app still works
        }
    }

    /**
     * Get categories for UI dropdowns (with optional "All" prefix for filters).
     */
    fun getCategoriesForFilter(): List<String> {
        return listOf("All") + _categories.value
    }

    /**
     * Get categories for selection (profile, job creation) — no "All" prefix.
     */
    fun getCategoriesForSelection(): List<String> {
        return _categories.value
    }

    /**
     * Get the similarity score between two categories.
     * Returns 1.0 for exact match, 0.0-0.8 for related, 0.1 for unrelated.
     */
    fun getSimilarity(catA: String, catB: String): Double {
        if (catA.isBlank() || catB.isBlank()) return 0.5
        if (catA.equals(catB, ignoreCase = true)) return 1.0

        // Check direct similarity
        val direct = _similarityMap.value[catA to catB]
        if (direct != null) return direct

        // Check if they're in the same group (implicit similarity)
        val groupA = _categoryGroups.value.find { group ->
            group.categories.any { it.equals(catA, ignoreCase = true) }
        }
        val groupB = _categoryGroups.value.find { group ->
            group.categories.any { it.equals(catB, ignoreCase = true) }
        }

        if (groupA != null && groupB != null && groupA.name == groupB.name) {
            return 0.5 // Same group = moderate similarity
        }

        return 0.1 // Unrelated
    }

    /**
     * Get the parent group name for a category.
     */
    fun getGroupForCategory(category: String): String? {
        return _categoryGroups.value.find { group ->
            group.categories.any { it.equals(category, ignoreCase = true) }
        }?.name
    }

    // ── Default Data (Comprehensive Blue-Collar Categories) ────────────────────

    private fun getDefaultGroups(): List<CategoryGroup> = listOf(
        CategoryGroup(
            name = "Construction & Building",
            icon = "🏗️",
            categories = listOf(
                "Mason",
                "Painter",
                "Carpenter",
                "Tile Fitter",
                "Roofer",
                "Scaffolder",
                "Demolition Worker",
                "Construction Helper"
            )
        ),
        CategoryGroup(
            name = "Plumbing & Water",
            icon = "🔧",
            categories = listOf(
                "Plumber",
                "Pipe Fitter",
                "Borewell Technician",
                "Water Tank Cleaner"
            )
        ),
        CategoryGroup(
            name = "Electrical & Wiring",
            icon = "⚡",
            categories = listOf(
                "Electrician",
                "Wireman",
                "AC Technician",
                "Inverter/Solar Technician",
                "CCTV Installer"
            )
        ),
        CategoryGroup(
            name = "Home & Cleaning",
            icon = "🏠",
            categories = listOf(
                "House Cleaner",
                "Deep Cleaning",
                "Pest Control",
                "Gardener",
                "Cook",
                "Laundry/Ironing"
            )
        ),
        CategoryGroup(
            name = "Transport & Delivery",
            icon = "🚚",
            categories = listOf(
                "Delivery Boy",
                "Driver",
                "Packer & Mover",
                "Loading/Unloading"
            )
        ),
        CategoryGroup(
            name = "Repairs & Maintenance",
            icon = "🛠️",
            categories = listOf(
                "Furniture Repair",
                "Appliance Repair",
                "Mobile/Laptop Repair",
                "Welder",
                "Blacksmith",
                "Mechanic (Auto)",
                "Mechanic (Bike)"
            )
        ),
        CategoryGroup(
            name = "Beauty & Personal Care",
            icon = "💇",
            categories = listOf(
                "Barber/Hairdresser",
                "Beauty Parlour",
                "Mehendi Artist",
                "Massage Therapist"
            )
        ),
        CategoryGroup(
            name = "Events & Decoration",
            icon = "🎉",
            categories = listOf(
                "Tent & Decoration",
                "Catering Helper",
                "DJ/Sound Setup",
                "Photographer (Event)",
                "Florist"
            )
        ),
        CategoryGroup(
            name = "Agriculture & Farm",
            icon = "🌾",
            categories = listOf(
                "Farm Labour",
                "Tractor Operator",
                "Harvester",
                "Irrigation Worker",
                "Animal Caretaker"
            )
        ),
        CategoryGroup(
            name = "Tailoring & Fabric",
            icon = "🧵",
            categories = listOf(
                "Tailor",
                "Embroidery Worker",
                "Alterations",
                "Upholstery"
            )
        ),
        CategoryGroup(
            name = "Security & Guard",
            icon = "🛡️",
            categories = listOf(
                "Security Guard",
                "Night Watchman",
                "Bouncer"
            )
        ),
        CategoryGroup(
            name = "Other",
            icon = "📋",
            categories = listOf(
                "Data Entry",
                "Shop Helper",
                "Warehouse Worker",
                "Tutor",
                "Other"
            )
        )
    )

    private fun getDefaultFlatList(): List<String> {
        return getDefaultGroups().flatMap { it.categories }
    }

    /**
     * Build default similarity map based on group membership + explicit pairs.
     */
    private fun buildDefaultSimilarityMap(): Map<Pair<String, String>, Double> {
        val map = mutableMapOf<Pair<String, String>, Double>()

        // Explicit cross-group similarities
        val crossGroupPairs = listOf(
            // Construction ↔ Repairs
            Triple("Mason", "Tile Fitter", 0.7),
            Triple("Carpenter", "Furniture Repair", 0.7),
            Triple("Painter", "Tile Fitter", 0.4),
            Triple("Construction Helper", "Loading/Unloading", 0.6),

            // Electrical ↔ Repairs
            Triple("Electrician", "AC Technician", 0.7),
            Triple("Electrician", "Appliance Repair", 0.5),
            Triple("AC Technician", "Inverter/Solar Technician", 0.6),
            Triple("CCTV Installer", "Electrician", 0.5),

            // Plumbing ↔ Construction
            Triple("Plumber", "Mason", 0.3),
            Triple("Plumber", "Tile Fitter", 0.4),

            // Transport ↔ Events
            Triple("Packer & Mover", "Loading/Unloading", 0.8),
            Triple("Delivery Boy", "Packer & Mover", 0.4),
            Triple("Driver", "Delivery Boy", 0.5),

            // Home ↔ Events
            Triple("Cook", "Catering Helper", 0.7),
            Triple("House Cleaner", "Deep Cleaning", 0.8),
            Triple("Gardener", "Florist", 0.5),

            // Repairs ↔ Repairs
            Triple("Welder", "Blacksmith", 0.6),
            Triple("Mechanic (Auto)", "Mechanic (Bike)", 0.7),
            Triple("Mechanic (Auto)", "Welder", 0.4),

            // Events ↔ Beauty
            Triple("Mehendi Artist", "Tent & Decoration", 0.3),
            Triple("Beauty Parlour", "Barber/Hairdresser", 0.6),

            // Agriculture ↔ Construction
            Triple("Farm Labour", "Construction Helper", 0.4),
            Triple("Loading/Unloading", "Farm Labour", 0.4),

            // Tailoring
            Triple("Tailor", "Embroidery Worker", 0.7),
            Triple("Tailor", "Alterations", 0.8),
            Triple("Upholstery", "Furniture Repair", 0.4)
        )

        for ((a, b, score) in crossGroupPairs) {
            map[a to b] = score
            map[b to a] = score
        }

        // Intra-group: all items in same group get baseline 0.4 similarity
        for (group in getDefaultGroups()) {
            for (i in group.categories.indices) {
                for (j in i + 1 until group.categories.size) {
                    val a = group.categories[i]
                    val b = group.categories[j]
                    // Only set if not already explicitly set with a higher value
                    if ((map[a to b] ?: 0.0) < 0.4) {
                        map[a to b] = 0.4
                        map[b to a] = 0.4
                    }
                }
            }
        }

        return map
    }

    // ── Firestore Seeding ──────────────────────────────────────────────────────

    /**
     * Seeds the Firestore `config/categories` document with default data.
     * Called automatically if the document doesn't exist yet.
     */
    private suspend fun seedFirestore() {
        try {
            val groups = getDefaultGroups().map { group ->
                mapOf(
                    "name" to group.name,
                    "icon" to group.icon,
                    "categories" to group.categories
                )
            }

            val similarityPairs = buildDefaultSimilarityMap()
                .entries
                .filter { (key, _) -> key.first < key.second } // Only one direction to avoid duplicates
                .map { (key, score) ->
                    mapOf("a" to key.first, "b" to key.second, "score" to score)
                }

            val data = mapOf(
                "groups" to groups,
                "flatList" to getDefaultFlatList(),
                "similarityPairs" to similarityPairs,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            FirebaseFirestore.getInstance()
                .collection("config")
                .document("categories")
                .set(data)
                .await()

            Log.d(TAG, "Seeded Firestore with ${getDefaultFlatList().size} categories")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to seed categories to Firestore", e)
        }
    }

    /**
     * Force refresh categories from Firestore (e.g., pull-to-refresh).
     */
    suspend fun refresh() {
        isLoaded = false
        loadFromFirestore()
    }
}

