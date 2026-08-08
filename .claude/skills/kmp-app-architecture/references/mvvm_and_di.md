# MVVM/MVI contract, DI boundaries and expect/actual placement

Answers one decision: **what shape must this view model, Koin binding or platform declaration take?**

## The contract

`BaseViewModel<State, Effect>` in `core:common` is the only base class:

- **State** — one immutable `data class` per screen, defaults for every field, exposed as
  `StateFlow`. Mutated only through `setState { copy(...) }`.
- **Effect** — a `sealed interface` of one-shot events (navigation, snackbars) on a hot
  `SharedFlow`. Never model navigation as state.
- **Intents** — plain public methods on the view model (`search(query)`, `retry()`). No
  `dispatch(Action)` switchboard.
- Work runs in `viewModelScope`; suspending calls return `Outcome`, so `try/catch` never appears in
  a view model.

## Screen split

Every screen is two composables:

```kotlin
@Composable
internal fun DictionaryListScreen(         // stateful: injects the VM, collects state
    onDictionaryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictionaryListViewModel = koinViewModel(),
) { ... }

@Composable
internal fun DictionaryListContent(        // stateless: pure state -> UI, previewable, testable
    state: DictionaryListUiState,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) { ... }
```

Navigation is passed **in** as lambdas from the nav graph; a screen never holds a `NavController`.

## Dependency injection boundaries

- `coreModule` (in `composeApp/di/AppModule.kt`) wires `core:*` only.
- Each feature owns one `Module`; `appModules` is the single list, `initKoin()` the single entry
  point, called exactly once per platform launcher before first composition.
- View models are registered with `viewModelOf(::Vm)` and resolved with `koinViewModel()`.
- Interfaces are bound explicitly: `single<WordRepository> { InMemoryWordRepository() }`.
- No `GlobalContext.get()`, no service locators inside classes — constructor injection only.

## expect/actual placement

Declare the `expect` next to the interface it serves in `commonMain`, and put actuals in the
narrowest source set that works: `webMain` when js and wasmJs share the implementation,
`jsMain`/`wasmJsMain` only for the low-level bridge that cannot be shared (see the `kmp-development`
skill for the `js(...)` constraint), `iosMain` for Apple.
