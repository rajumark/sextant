# Comparing versions

`sextant diff` compares two versions of an app (two APKs or two `.sqlite` files) and reports what was added, removed and changed.

```bash
sextant extract app-8.31.2.apk -o old/
sextant extract app-8.32.3.apk -o new/
sextant diff old.apk new.apk
sextant diff old.apk new.apk --show 50     # list more items per section
sextant diff old.apk new.apk --json        # full report
```

Real example, Signal 8.31.2 → 8.32.3 (both official releases):

```text
# org.thoughtcrime.securesms 8.31.2 (176601)  →  org.thoughtcrime.securesms 8.32.3 (176902)

## app  (+0 −0 ~2)
  ~ version_code: 176601  →  176902
  ~ version_name: 8.31.2  →  8.32.3

## components  (+3 −3 ~0)
  + activity org.thoughtcrime.securesms.mediaviewer.MediaViewerActivity = exported=0(explicit) …
  − activity org.thoughtcrime.securesms.contactshare.SelectContactActivity = exported=0(explicit) …

## packages (classes)  (+11 −4 ~168)
  + org.signal.mediaviewer = 48
  ~ org.thoughtcrime.securesms.shakereport: 3  →  46

## resources  (+53 −17 ~0)
  + string/AssistantScreenSecurityBottomSheet__title = Screen security is enabled

Unchanged: permissions, deep links, frameworks, sdks, libraries, hosts, native libraries, jni methods
```

## Sections

| Section | Compared by |
|---|---|
| app | version, SDK levels, label, application class, debug/backup/cleartext flags, signing schemes and certificates, dex count, ABIs, build tool |
| permissions, components | name, plus their attributes (`~` when exported, permissions or intent filters change) |
| deep links, frameworks, SDKs | presence |
| libraries | `group:artifact`, version changes shown as `~` |
| hosts, URLs | presence |
| native libraries | path, with a `~` when the file's sha256 changed |
| JNI methods | presence of `Class.method` implemented natively |
| packages (classes) | class count per package, ordered by the size of the change |
| classes, strings | presence |
| resources | `type/name`, value changes shown as `~` |

Totals in each header are exact; item lists are capped (15 by default, `--show` to change).

!!! warning "Obfuscated names"
    R8 can rename classes differently in every build, so a renamed class shows as one removed plus one added. Package counts, resources, strings, components and library versions are the more stable signals.
