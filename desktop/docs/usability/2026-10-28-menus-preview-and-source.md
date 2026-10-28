# Usability Check: Menus page details pane (Preview and Source sections)
Date: 2026-10-28 · Mode: Review · Build: uncommitted (on a5586d6)
Data: real APKs binaryeye 1.75.5, AnkiDroid 2.25.3, Nextcloud 35.0.1 (screenshots: `desktop/build/screenshots/menus/`, from `MenuPreviewShotTest`)

## Summary
The new Preview shows a menu the way the app shows it: app bar or toolbar with action buttons and the ⋮ list opened, bottom-navigation tabs, or drawer rows, with checks, radios, group dividers, sub-menus and disabled items. The biggest gap is that icons are initials in circles, because core cannot draw drawables yet. The best quick win was naming untitled items the same way in the preview and the item list, which is fixed in this change.

## Task scripts
| Script | Persona | Budget | Actual | Result |
|---|---|---|---|---|
| "What does this menu look like, and what is hidden?" | P1 | 2 actions | 2 (open Menus, pick menu) | Pass |
| "Which menu items does code fill in or hide?" | P1, P2 | 2 actions | 2: the untitled count and the hidden list are under the preview | Pass |
| "Show me the menu XML" | P1 | 3 actions | 3 (Source is in the same pane; scroll the pane) | Pass |
| "Where is this menu shown?" | P2 | 2 actions | 2: "shown by layout/…", and Used by opens the layout | Pass |

## Findings
### [Minor] Icons are placeholders — EV-01
Where: MenuPreview.kt › Glyph
What happens: action buttons and tabs show the item's initial, not its drawable. The real `@drawable/…` is listed in the item list below.
Who it hurts: P2 (recognising their own toolbar).
Fix: needs data from core: render vector drawables to images ("Drawable image previews", still pending in core).

### [Minor] Host is inferred — EV-03
Where: core MenuPreviews.of
What happens: a menu that no layout references is shown as an app bar. It could also be a popup or context menu inflated in code. The pane says so in one sentence above the preview.
Who it hurts: P1 (could misread a context menu as the toolbar).
Fix: needs data from core: find `R.menu.x` uses in code (the pending resource-id scan) and tell apart onCreateOptionsMenu, PopupMenu and onCreateContextMenu.

### [Polish] Placeholder screen title — LG-03
Where: MenuPreview.kt › AppBar
What happens: the app bar shows the menu name where the screen title would be, dimmed.
Fix: keep it until code analysis can say which activity inflates the menu; then show that activity's label.

### [Minor, fixed] Untitled items had two names — LG-03
Where: ResourceScreens.kt › menusSpec items
What happened: the item list showed "(item)" while the preview showed the id. Both now show the id, and the list adds "title set in code".

## Layered depth
| View | L0 verdict | L1 why | L2 raw | L3 query | Notes |
|---|---|---|---|---|---|
| Menu preview | what users see: bar, ⋮ list, tabs or drawer | "shown by layout/…", hidden and untitled notes | Items list and Source XML in the same pane | `sextant res <apk> menus NAME` prints the same preview | Same core code as the CLI |

## Passed
EV-02 (the preview and the CLI use the same `MenuPreviews`), EV-06 (untitled and hidden items are shown, not dropped), VA-01 (theme roles only, light and dark checked), VA-04 (disabled is dimmed and the state also shows in the item list), XD-01, XD-02 (Source XML next to the preview), AC-01 (copy the menu name and the Source).

## Not checked
Keyboard: sub-menu rows open on click. They are `clickable`, so they get focus, but I did not check visible focus by keyboard (KB-01). The 1024×700 window size (VA-05): the preview is at most 400 dp wide and should fit, but no screenshot was taken at that size.
