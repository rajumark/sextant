# Desktop design rules

These rules apply to every screen of Sextant Desktop. They are binding for people and agents. If a design conflicts with a rule, change the design.

## 1. The page never scrolls (most important)

Sextant is a desktop app, not a web page.

- **The window, the main content and every screen's root never scroll.** No `verticalScroll` on a screen's outer container. A screen fits the window.
- **Scrolling is allowed only inside a pane:** a list (`LazyColumn`), a code or XML/JSON view, or a side panel that holds its own content (a detail pane, a card that is taller than the space left). The pane scrolls; the window does not.
- **Lay out with weights.** The header and summary strips take their natural height. The main area takes the rest with `Modifier.weight(1f)`. Panes inside it scroll on their own.
- **If content does not fit, shrink or split it, don't scroll the page.** Move detail into a pane, use a list, or show the top items with "Show all" that opens a pane.
- **Narrow windows** (below 860dp) stack the same panes vertically. Each pane still keeps its own scroll; the window still does not scroll.

Check: resize the window to 1024×700. Nothing outside a list, code view or pane may move.

## 2. Home: two panes, half and half

The Home screen is a 50 / 50 split on a wide window:

- **Left (50%): start.** The title, the drop zone (drop an APK or an output folder), "Choose APK…" and "Open output folder…". The drop zone takes the space it is given.
- **Right (50%): Recent analyses.** A list of recent analyses, newest first, scrolling inside its pane. Each row: app icon, name, package, version, counts (permissions, dangerous, exported), when analyzed. "Clear" sits in the pane header. Empty state explains what will appear here.

Any extra explanation (what Sextant reads, "runs locally") goes into the start pane, never below the fold.

## 3. Analysis screens

- Overview: summary strip at the top (fixed). Below it, panels (Worth a look, Entry points, Flags, Component mix). Each panel scrolls on its own when it is taller than its space.
- Lists are the scrolling unit: Permissions, Components, Manifest tree, Output file list.
- Detail panes (right of a list) scroll inside the pane.

## 4. Keyboard and window

- Every action is reachable from the menu bar and, once built, the command palette (⌘K / Ctrl+K).
- Window size and last screen should be remembered (not built yet).

## 5. Look

- Colors only from `theme/Theme.kt` roles. No raw colors.
- Use `ui/common` components for tags, cards, avatars, copy buttons.
