# Investigating an app

Once an APK is extracted, every investigation follows the same loop: **find** something, **look** at it, **follow** the code from there. All commands below take the `.sqlite` file from `extract`, or the APK itself.

## Annotations, lines and exception handlers

Beyond structure, the code's own metadata is in SQL:

```bash
sextant q annotated app.sqlite --arg 'type=Ljavax/inject/Inject;'   # every injection point, with values
sextant q broad-catches app.sqlite                                  # methods that catch Throwable or Exception
sextant sql app.sqlite "SELECT first_line, last_line, line_table FROM method_lines l JOIN methods m ON m.id = l.method WHERE m.descriptor LIKE 'Lcom/app/Login;->auth%'"
```

`line_table` maps code offsets to source lines, so an offset from a stack trace or from `calls.offsets` gives the line it is on.

## Every instruction

`instruction_view` lists every instruction of every method in code order, the same sequence `smali` prints. Registers are raw v numbers: smali's `p0`, `p1`, … are the method's last registers.

```bash
sextant sql app.sqlite "SELECT op, ref, literal FROM instruction_view WHERE method LIKE 'Lcom/app/Login;->auth%'"
sextant q literal-users app.sqlite --arg value=8443      # every method that loads the number 8443
sextant q opcode-stats app.sqlite                        # how often each opcode is used
```

## Follow links across kinds

`links` connects things of different kinds, so one command answers "what does this touch?":

```bash
sextant links app.sqlite .MainActivity --depth 2   # component → class → superclass and methods; permissions, deep links, resources
sextant links app.sqlite @string/app_name          # the layouts, menus and methods that use it
sextant links app.sqlite @menu/main                # its items' strings and icons, and the method that inflates it
sextant links app.sqlite permission:android.permission.CAMERA
sextant links app.sqlite example.com                # every URL on that host, and where each comes from
sextant links app.sqlite "Firebase Crashlytics"     # the packages that make up an SDK
```

`→` lines are outgoing links, `←` lines incoming ones, and the text in brackets is the evidence: the attribute that holds a reference, or the call a resource id is passed to. Keys are readable (`component:…`, `class:L…;`, `method:…`, `@type/name`, `url:…`), and the same edges are in the `links` table for SQL.

## Names and IDs

Sextant identifies code with **smali descriptors**, the format `dexdump`, `baksmali` and most Android tools use:

| Kind | Example |
|---|---|
| Class | `Lcom/example/Login;` |
| Method | `Lcom/example/Login;->auth(Ljava/lang/String;)Z` |
| Field | `Lcom/example/Login;->token:Ljava/lang/String;` |

You rarely have to type full descriptors. Commands accept shorter forms:

| Command input | Matches |
|---|---|
| `com.example.Login` or `Login` | a class |
| `Login.auth`, `com.example.Login.auth` | method `auth` of that class (all overloads) |
| `Lcom/example/Login;->auth` | every overload of `auth` |
| `auth` | every method named `auth` (you get a list if several classes have one) |

## Find

`find` is a substring search over everything with a name: classes, methods, fields, string constants, components, permissions, deep links, resource values, endpoints, SDKs, libraries, native symbols and findings.

```bash
sextant find app.sqlite "api.example.com"
sextant find app.sqlite "token" --kind field
sextant find app.sqlite "Login" --kind class --limit 20
sextant find app.sqlite "Java_com_" --kind native_symbol
```

Three or more characters use the full-text (trigram) index; shorter text falls back to a slower scan. For string hits, `find` shows the methods that load the string.

## Look at a class

```bash
sextant class app.sqlite CameraActivity
```

You get access flags, superclass, interfaces, subclasses, fields (with constant initial values), every method with its instruction count, calls out and call sites into it, callers from outside the class, and the strings it uses. `--json` gives the same as one object.

## Follow calls

```bash
sextant callers app.sqlite "Landroid/app/Activity;->getIntent" --depth 1 --fan-out 3
```

```text
Landroid/app/Activity;->getIntent()Landroid/content/Intent;  (external)
├── Lde/markusfisch/android/binaryeye/activity/PickActivity;->onCreate(Landroid/os/Bundle;)V  ×6
├── Lde/markusfisch/android/binaryeye/activity/DecodeActivity;->onCreate(Landroid/os/Bundle;)V  ×4
├── Landroidx/activity/a;->c()Lor0;  ×3
└── … 4 more (raise the fan-out limit)
```

- `callers` walks up (who calls this), `callees` walks down (what this calls).
- `×6` is the number of call sites.
- `(external)` marks framework or library methods that are referenced but not defined in the APK; they are leaves in a callee tree.
- `(cycle)` marks a method already on the current path.
- `--depth` (default 3) and `--fan-out` (default 15) keep trees readable.

!!! info "Virtual calls"
    Call edges are static. A call made through an interface or superclass is recorded against that base method. To see calls into an override, also ask about the base method, or use `reach`, which follows overrides.

## Find a path between two points

`reach` answers "can this entry point get to that code?": a breadth-first search from every method of a component (or from one method) to targets.

```bash
sextant reach app.sqlite de.markusfisch.android.binaryeye.activity.CameraActivity 'SharedPreferences$Editor.putBoolean'
```

```text
Path 1 (1 calls)
  Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V
    → Landroid/content/SharedPreferences$Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences$Editor;
1 path(s) · 23 entry methods · 6967 methods visited
```

Targets can be a descriptor prefix (`Ljavax/crypto/Cipher;->getInstance`), `Class.method` (`Cipher.getInstance`), a package fragment (`okhttp3/`) or a method name. Virtual and interface calls dispatch into overrides in subclasses (class-hierarchy analysis). Signatures with more than 25 implementations (`Runnable.run`, `Function1.invoke`) are not expanded, because that connects unrelated code. You get one shortest path per target method.

## Who uses a class or field

```bash
sextant xref app.sqlite 'Lde/markusfisch/android/binaryeye/activity/CameraActivity;->c1'
```

```text
read Lde/markusfisch/android/binaryeye/activity/CameraActivity;->c1:Z
    Lwh;->run()V  ×1
write Lde/markusfisch/android/binaryeye/activity/CameraActivity;->c1:Z
    Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V  ×2
```

For a class, `xref` lists instantiations (`new`), casts, `instanceof`, `const-class`, calls into its methods and uses of its fields from other classes.

## Read the code

```bash
sextant source app.apk com.example.Login.auth     # decompiled Java (jadx)
sextant smali  app.apk 'Lcom/example/Login;->auth' # bytecode assembly (baksmali)
```

Both take the APK or the `.sqlite` file: the file stores the dex files, so it is rebuilt into a zip once and cached. `source` keeps the original (possibly obfuscated) names so they match the index; anonymous and lambda classes that jadx inlines are shown inside their enclosing class. `smali` works even when decompiling fails, and its `#@54` comments are hex code-unit offsets that match the decimal `offsets` in `code/calls.jsonl` (0x54 = 84).

## URLs

```bash
sextant endpoints app.sqlite --host example.com
```

```text
http://markusfisch.de/BinaryEye
    code Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V
http://markusfisch.de/BinaryEye*
    deep_link de.markusfisch.android.binaryeye.activity.CameraActivity
```

Sources are `code` (with the method), `resource`, `deep_link`, `manifest`, `asset` (JS bundles and other text assets) and `native` (`.so` files). URLs found in binary files are reduced to `scheme://host`, because packed string tables don't keep string boundaries.
