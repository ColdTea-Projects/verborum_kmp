# Adding an icon

Answers one decision: **how does a new glyph get into the app without breaking the bundle or
rendering blank?**

Icons live in `VerborumIcons` in `core:designsystem`. Add there rather than pulling
`material-icons-extended` into a feature: the set is small, and that dependency ships every Material
glyph into a Wasm bundle.

Icons are `ImageVector` declared in Kotlin, not files. That is the Compose Multiplatform norm for
tintable glyphs — nothing to load, nothing to package per target, and **zero HTTP fetches on web**,
where every `composeResources` file is a separate request. `composeResources` is for the other
category: illustrations, logos, fonts, photos.

To add one: run the SVG through a converter (`com.android.tools:sdk-common`'s `Svg2Vector`, which is
the code behind Android Studio's Vector Asset importer, or an SVG-to-Compose site), then paste the
result into `materialIcon { … }`. The converter is what flattens `<circle>`/`<rect>` into path data
and bakes in `transform`; hand-pasting a `d` attribute only works for SVGs that are already a single
`<path>`.

Three rules, each learned the hard way:

- **Every `path(…)` must state a `fill` or a `stroke`.** Compose's `path` defaults `fill = null`,
  which paints nothing — an invisible icon, with no warning and no error.
- **Never add a local helper named `path` to that file.** An explicit import outranks a same-file
  top-level declaration in Kotlin, so `import androidx.compose.ui.graphics.vector.path` silently
  captures *every* `path { … }` call in the file. That once blanked eighteen icons at once.
- Set `materialIcon(viewport = …)` to the source SVG's `viewBox` when it is not 24.

The fill colour itself is arbitrary: `Icon` tints with a colour filter and replaces it. Black is
used throughout only for consistency. An icon that must keep its own colours is not an icon — use
`Image(painterResource(…))` with the asset in `composeResources`.
