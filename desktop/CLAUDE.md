# Sextant Desktop

The desktop app is this `desktop` module of the Sextant repo. It's built with Compose Multiplatform (JVM) and Material 3, and reads APKs through `:core`.

- Run: `gradle :desktop:run` · Test: `gradle :desktop:test` (use the system `gradle`, not `./gradlew`)
- Screenshots of every screen: `gradle :desktop:test --tests '*ScreenshotTest*'` → `desktop/build/screenshots/`
- Code is in `src/main/kotlin/io/github/rajumark/sextant/desktop/`: UI in `ui/`, state in `AppState.kt` and `ui/workspace/WorkspaceState.kt`, data in `data/SextantRepository.kt` and `CoreMapper.kt`, findings in `data/Insights.kt`, theme in `theme/Theme.kt`.

## Product direction

The goal is a workbench for understanding APKs, not a GUI wrapper over the CLI. Primary users, in order: mobile AppSec reviewers, Android release developers, privacy reviewers. Easy on the first day, fast on the hundredth.

## Usability Check (required for UI work)

- Before designing or changing any screen, load the `usability-check` skill (`.claude/skills/usability-check/` at the repo root) and follow its rules.
- After a UI change, run a Usability Check on the changed scope and save the report in `desktop/docs/usability/`.
- For larger design or review work, use the `sextant-designer` agent.

**Design skills:** for Material 3 and expressive design (color, shape, motion, components) load `.claude/skills/m3-expressive/` (start with `SKILL.md`) together with the usability check. Color comes from the seed color in Settings; `theme/Theme.kt` builds the full scheme from it.

**Design rule, binding:** the page never scrolls. Sextant is a desktop app, not a web page. Only lists, code views and panes scroll; screens fit the window. Home is two panes (start on the left, recent analyses on the right). Read `docs/design-rules.md` before any UI change.

Non-negotiables (details in the skill):
1. Data and verdicts match the CLI for the same APK.
2. Layered depth (verdict → why → raw → query), never a beginner/expert mode switch.
3. Keyboard-complete, with a command palette.
4. Local only: no network, no telemetry.
5. Theme roles and `ui/common` components only.
