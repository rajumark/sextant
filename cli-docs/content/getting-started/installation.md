# Installation

Sextant is a single command-line program. It runs on **macOS, Linux and Windows** and needs **Java 17 or newer**.

## Check Java

```bash
java -version
```

If Java is missing or older than 17, install a current JDK, for example [Eclipse Temurin](https://adoptium.net/) or, on macOS, `brew install openjdk`.

## Download a release

1. Open the [latest release](https://github.com/rajumark/sextant/releases/latest).
2. Download `sextant-<version>.zip`.
3. Unzip it anywhere. It contains:

    ```text
    sextant-<version>/
      bin/sextant        # macOS and Linux launcher
      bin/sextant.bat    # Windows launcher
      lib/sextant-<version>.jar
    ```

4. Add the `bin` folder to your `PATH`.

=== "macOS / Linux"

    ```bash
    unzip sextant-1.0.0.zip -d ~/tools
    echo 'export PATH="$HOME/tools/sextant-1.0.0/bin:$PATH"' >> ~/.zshrc   # or ~/.bashrc
    source ~/.zshrc
    sextant --version
    ```

=== "Windows (PowerShell)"

    ```powershell
    Expand-Archive sextant-1.0.0.zip -DestinationPath $HOME\tools
    [Environment]::SetEnvironmentVariable("Path", $env:Path + ";$HOME\tools\sextant-1.0.0\bin", "User")
    # open a new terminal
    sextant --version
    ```

The same release page also has the **Sextant desktop app** for macOS (`.dmg`), Windows (`.msi`) and Linux (`.deb`). It uses the same engine as the CLI.

## Build from source

```bash
git clone https://github.com/rajumark/sextant.git
cd sextant
./gradlew :cli:installShadowDist
cli/build/install/sextant/bin/sextant --help
```

The build produces `cli/build/install/sextant/` (launchers plus one jar) and, with `./gradlew :cli:shadowDistZip`, the same zip that is published on the releases page.

## Verify the install

```bash
sextant --version
sextant --help
```

```text
Usage: sextant [<options>] <command> [<args>]...

  Turn an Android APK into structured data for people and AI agents.

Commands:
  extract    Extract structured data from an APK into a folder.
  info       Show a short summary of an APK or an extracted folder.
  ...
```

Next: [Quickstart](quickstart.md).
