# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
mvn clean package                # Compiles Java 21, produces fat JAR in target/
mvn test                         # No test suite committed yet; use as CI guard

java -jar target/jetbrains-config-setup-2.0.jar                    # Uses current dir as root
java -jar target/jetbrains-config-setup-2.0.jar /path/to/base-dir  # Explicit root dir
java -jar target/jetbrains-config-setup-2.0.jar --verbose           # Verbose logging (INFO level)
java -jar target/jetbrains-config-setup-2.0.jar --help              # Prints setup guide from resources
```

## Architecture

Single-module Maven CLI app (`io.nexus.jetbrainsconfigsetup`). Entry point is `Main`, which runs a loop-based interactive menu delegating to manager classes:

- **Main** - CLI arg parsing (`ConfiguracaoDeExecucao` record), main menu loop, download/install orchestration
- **GerenciadorDePastas** - Creates the standard directory structure: `atalhos/`, `binarios/`, `configuracoes/`, `ferramentas/`, `instaladores/`
- **GerenciadorDeFerramentas** - Decodes Base64 resources from classpath (`*.base64` files in `src/main/resources/`) into tool JARs and activation keys under `ferramentas/ferramenta-atual/`
- **GerenciadorDeDownloads** - Downloads latest IDE versions from JetBrains via HTTP with proxy support (`http_proxy`/`https_proxy`/`no_proxy`), progress bar output
- **GerenciadorDeInstalacao** - Lists local `.tar.gz`/`.zip` installers, interactive selection menu, extracts archives into `binarios/<ide>/<version>/`, delegates to configuration. Extracts version from filename regex or `product-info.json` inside the archive
- **GerenciadorDeConfiguracao** - Per-IDE post-install: modifies `idea.properties` for portable config paths, adds `-javaagent` to `.vmoptions`, copies activation keys, generates `.desktop` shortcuts (Linux) or `.lnk` shortcuts (Windows via PowerShell)
- **GerenciadorDeRegistrosJetBrains** - Linux-only: removes JetBrains directories from `~/.cache`, `~/.local/share`, `~/.config`

**DTOs**: `IdeInfo` (archive path + name + version + resolved binary path), `ProductInfo` (parsed from `product-info.json`), `AtalhoInfo` (desktop shortcut metadata per IDE)

**Enums**: `TipoIde` (supported IDEs with product codes for download), `TipoFerramenta` (legacy vs 2026 tool variant - controls which JAR is used as javaagent and whether `.key` files are copied)

## Cross-Platform

The app supports both Linux (`.tar.gz`, `.desktop` shortcuts, POSIX permissions) and Windows (`.zip`, `.lnk` via PowerShell). OS detection uses `System.getProperty("os.name")`. Menu option 4 (clean JetBrains registries) is Linux-only.

## Language & Naming Conventions

- **Always communicate in Brazilian Portuguese.**
- Class names: `PascalCase`, typically in Portuguese (e.g., `GerenciadorDePastas`)
- Methods/variables: `camelCase` in Portuguese without accents (e.g., `criarEstrutura`, `caminhoRaiz`)
- Constants: `UPPER_SNAKE_CASE`
- New methods must include detailed JavaDoc and comments explaining both "what" and "why"
- Use `@Slf4j` (SLF4J/Log4j2) for operational logging, not `System.out` (user-facing prompts use Jansi `ansi()`)

## Coding Guidelines

- Java 21, 4-space indentation
- Priorities: Correctness > Clarity > Simplicity > Extensibility > Optimization
- SOLID principles where applicable; prefer composition over inheritance
- Constructor injection for dependencies when possible
- Tests go under `src/test/java/io/nexus/jetbrainsconfigsetup/` with `*Test` suffix

## Commit Messages

Concise summaries in Portuguese following the repo history style: `Adicionado ...`, `Refatoração ...`, `Ajuste ...`. One logical change per commit.
