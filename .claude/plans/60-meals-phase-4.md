# Plan: #60 - Meals phase 4 (Recipes - Owned - and Shopping list)

Issue: https://github.com/ClaudiaMarques1/one/issues/60 (idea #12, phase 4 of 4)
Branch: `feature/cmarques/60-meals-phase-4` from `develop` (`6619d88`, after #58 phase 3).
Type: backend + data + front-facing. Builds on #53 / #56 / #58.

## Handover

`OneApp-designs/Meals/design_handoff_meals/README.md` - **1c** (Recipes tab) and **1e** (Shopping list).

## Decisions (user, 2026-11-22)

- **Meals tabs** like Diary's Journal | Goals: **Food Diary** (weekly view) | **Recipes**. Inside
  Recipes: **Owned** | **Search Recipe** (placeholder; lookup is the Todo card "Meals: Search Recipe").
- **Recipe** (`users/{uid}/recipes`): name, portions, cookMinutes, kcal / proteinG / carbsG / fatG per
  portion, ingredients `RecipeIngredient(name, qty, unit, aisle)`. Aisles fixed: Fruit & veg, Meat &
  fish, Dairy & eggs, Cupboard, Frozen, Bakery, Other. Editor = full-screen dialog (create / edit / delete).
- **PlannedMeal.recipeId** ("" when not from a recipe); a recipe pick = one portion.
- **Add sheet**: Favourites | Recipes | New; picks from both tabs add together (`Add N to Slot`).
- **Shopping list** (route `meals/shopping`): from today (or the week's first day if later) to the
  week's last day; planned meals with a recipeId -> that recipe's ingredients / portions, summed by
  name (case-insensitive) + unit + aisle; groups in aisle order; tick + own items persisted per week
  at `users/{uid}/shopping/{weekStart}` (`ShoppingState(checked, manual)`); share unticked as text.
  Opened from the Week plan footer and a cart icon in the Food Diary header.
- Empty-slot copy back to `Add from favourites or recipes`.
- Pure + tested: `buildShoppingList`, quantity formatting, recipe macro line with cook time.

## Checklist

- [x] Models + repositories (recipes, shopping state) + fakes
- [x] `buildShoppingList` + labels, tests
- [x] ViewModels: recipes (list / save / delete), add-from-recipe, shopping list (ticks, own items, share text), tests
- [x] Meals tab row (Food Diary | Recipes); Recipes: Owned list + editor, Search placeholder
- [x] Add sheet Recipes tab
- [x] Shopping list screen (1e) + route + entry points (Week footer, Food Diary cart icon)
- [x] Empty-slot copy (parked checklist only)
- [ ] Dark check (Manual 11); `/verify`, screenshots, PR `Closes #60`

## Next step

User runs the 11 Manual steps on #60 (they create real recipes, meals and ticks); then PR `Closes #60`.
Related Todo cards: "Settings: split into sections" and "Meals: Search Recipe".
