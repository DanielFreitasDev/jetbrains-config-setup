package io.nexus.jetbrainsconfigsetup;

import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import static org.fusesource.jansi.Ansi.ansi;

@Slf4j
public class GerenciadorDeConfiguracao {

    private static final String INICIO_BLOCO_CUSTOMIZADO = "# ===== INICIO CONFIGURACAO CUSTOMIZADA =====\n";
    private static final String FIM_BLOCO_CUSTOMIZADO = "# ===== FIM CONFIGURACAO CUSTOMIZADA =====\n";
    private final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    private final Map<String, String> VM_OPTIONS_MAP = Map.of(
            "rubymine", isWindows ? "rubymine64.exe.vmoptions" : "rubymine64.vmoptions",
            "pycharm", isWindows ? "pycharm64.exe.vmoptions" : "pycharm64.vmoptions",
            "intellij-idea", isWindows ? "idea64.exe.vmoptions" : "idea64.vmoptions",
            "datagrip", isWindows ? "datagrip64.exe.vmoptions" : "datagrip64.vmoptions",
            "webstorm", isWindows ? "webstorm64.exe.vmoptions" : "webstorm64.vmoptions"
    );

    private static final Map<String, String> KEY_FILES_MAP = Map.of(
            "rubymine", "rubymine.key",
            "pycharm", "pycharm.key",
            "intellij-idea", "idea.key",
            "datagrip", "datagrip.key",
            "webstorm", "webstorm.key"
    );

    public void configurarIde(IdeInfo ideInfo,
                              String caminhoRaiz,
                              Path diretorioAtalhos,
                              boolean usarChavePadraoDoProduto,
                              TipoFerramenta tipoFerramentaSelecionada) {
        log.info("Iniciando configuração para a IDE: {} {}", ideInfo.getNome(), ideInfo.getVersao());
        try {
            configurarProperties(ideInfo, caminhoRaiz);
            if (usarChavePadraoDoProduto) {
                configurarVmOptions(ideInfo, caminhoRaiz, tipoFerramentaSelecionada);
                if (tipoFerramentaSelecionada.deveCopiarChavePadrao()) {
                    copiarChave(ideInfo, caminhoRaiz);
                } else {
                    log.info("Ferramenta {} selecionada para {}. A cópia do arquivo .key será ignorada.",
                            tipoFerramentaSelecionada.getNomeExibicao(),
                            ideInfo.getNome());
                }
            } else {
                log.info("Aplicação da chave padrão desativada para {}. Pulando .vmoptions customizado e arquivo .key.", ideInfo.getNome());
            }

            if (diretorioAtalhos != null) {
                if (isWindows) {
                    gerarAtalhoWindows(ideInfo, diretorioAtalhos);
                } else {
                    gerarAtalho(ideInfo, caminhoRaiz, diretorioAtalhos);
                }
            }
            System.out.println(ansi().fg(Ansi.Color.GREEN).a("✓ Configuração finalizada com sucesso para " + ideInfo.getNome() + " " + ideInfo.getVersao()).reset());
        } catch (IOException e) {
            log.error("Erro ao configurar a IDE {}", ideInfo.getNome(), e);
            System.out.println(ansi().fg(Ansi.Color.RED).a("✗ Falha na configuração de " + ideInfo.getNome() + ". Verifique os logs.").reset());
        }
    }

    private void configurarProperties(IdeInfo ideInfo, String caminhoRaiz) throws IOException {
        Path propertiesFile = ideInfo.getCaminhoBinario().resolve("bin/idea.properties");
        if (Files.notExists(propertiesFile)) {
            log.warn("Arquivo 'idea.properties' não encontrado para {}. Ignorando.", ideInfo.getNome());
            return;
        }

        String content = Files.readString(propertiesFile, StandardCharsets.UTF_8);
        if (content.contains(INICIO_BLOCO_CUSTOMIZADO)) {
            log.info("'idea.properties' já configurado para {}. Nenhuma alteração necessária.", ideInfo.getNome());
            return;
        }

        String newContent = generateCustomConfig(ideInfo, caminhoRaiz, content);
        Files.writeString(propertiesFile, newContent, StandardCharsets.UTF_8);
        log.info("Arquivo 'idea.properties' atualizado com sucesso para {}.", ideInfo.getNome());
    }

    private static String generateCustomConfig(IdeInfo ideInfo, String caminhoRaiz, String content) {
        Path configPath = Path.of(caminhoRaiz, "configuracoes", ideInfo.getNome(), ideInfo.getVersao(), "config");
        Path systemPath = Path.of(caminhoRaiz, "configuracoes", ideInfo.getNome(), ideInfo.getVersao(), "system");

        String customConfig = INICIO_BLOCO_CUSTOMIZADO +
                "idea.config.path=" + configPath.toAbsolutePath().toString().replace("\\", "/") + "\n" +
                "idea.system.path=" + systemPath.toAbsolutePath().toString().replace("\\", "/") + "\n" +
                "idea.plugins.path=${idea.config.path}/plugins\n" +
                "idea.log.path=${idea.system.path}/log\n" +
                FIM_BLOCO_CUSTOMIZADO + "\n";

        return customConfig + content;
    }

    private void configurarVmOptions(IdeInfo ideInfo, String caminhoRaiz, TipoFerramenta tipoFerramentaSelecionada) throws IOException {
        String vmOptionsFileName = VM_OPTIONS_MAP.get(ideInfo.getNome());
        if (vmOptionsFileName == null) {
            log.warn("Arquivo VMOptions não mapeado para {}. Ignorando.", ideInfo.getNome());
            return;
        }

        Path vmOptionsFile = ideInfo.getCaminhoBinario().resolve("bin").resolve(vmOptionsFileName);
        if (Files.notExists(vmOptionsFile)) {
            log.warn("Arquivo '{}' não encontrado. Ignorando.", vmOptionsFileName);
            return;
        }

        List<String> lines = Files.readAllLines(vmOptionsFile, StandardCharsets.UTF_8);
        if (lines.stream().anyMatch(line -> line.contains(INICIO_BLOCO_CUSTOMIZADO))) {
            log.info("'{}' já configurado. Nenhuma alteração necessária.", vmOptionsFileName);
            return;
        }

        Path toolPath = Path.of(caminhoRaiz, "ferramentas", "ferramenta-atual", tipoFerramentaSelecionada.getNomeArquivoJavaAgent());
        String customConfig = "\n" + INICIO_BLOCO_CUSTOMIZADO +
                "--add-opens=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED\n" +
                "--add-opens=java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED\n" +
                "-javaagent:" + toolPath.toAbsolutePath().toString().replace("\\", "/") + "\n" +
                FIM_BLOCO_CUSTOMIZADO;

        Files.writeString(vmOptionsFile, customConfig, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
        log.info("Arquivo '{}' atualizado com sucesso.", vmOptionsFileName);
    }

    private void copiarChave(IdeInfo ideInfo, String caminhoRaiz) throws IOException {
        String keyFileName = KEY_FILES_MAP.get(ideInfo.getNome());
        if (keyFileName == null) {
            log.warn("Arquivo de chave não mapeado para {}. Ignorando.", ideInfo.getNome());
            return;
        }

        Path chaveOrigem = Path.of(caminhoRaiz, "ferramentas", "ferramenta-atual", "chaves", keyFileName);
        if (Files.notExists(chaveOrigem)) {
            log.warn("Arquivo de chave '{}' não encontrado na origem. Ignorando.", keyFileName);
            return;
        }

        Path configDir = Path.of(caminhoRaiz, "configuracoes", ideInfo.getNome(), ideInfo.getVersao(), "config");
        Files.createDirectories(configDir);
        Path chaveDestino = configDir.resolve(keyFileName);

        if (Files.exists(chaveDestino)) {
            log.info("Arquivo de chave '{}' já existe no destino. Nenhuma alteração necessária.", keyFileName);
            return;
        }

        Files.copy(chaveOrigem, chaveDestino, StandardCopyOption.REPLACE_EXISTING);
        log.info("Chave '{}' copiada com sucesso para {}.", keyFileName, configDir);
    }

    private void gerarAtalho(IdeInfo ideInfo, String caminhoRaiz, Path diretorioAtalhos) throws IOException {
        AtalhoInfo atalho = new AtalhoInfo(ideInfo);
        if (atalho.getDesktopExec() == null) {
            log.warn("Sem modelo de atalho para {}. Ignorando...", ideInfo.getNome());
            return;
        }

        String desktopContent = generateDesktopEntryContent(ideInfo, caminhoRaiz, atalho);

        Path desktopFile = diretorioAtalhos.resolve(atalho.getDesktopFileName());
        Files.createDirectories(diretorioAtalhos);
        Files.writeString(desktopFile, desktopContent, StandardCharsets.UTF_8);

        try {
            if (!isWindows) {
                Files.setPosixFilePermissions(desktopFile, java.nio.file.attribute.PosixFilePermissions.fromString("rwxrwxr-x"));
            }
        } catch (UnsupportedOperationException e) {
            // Ignora se o sistema de arquivos não suportar POSIX
        }

        System.out.println(ansi().fg(Ansi.Color.CYAN).a("✓ Atalho criado: " + desktopFile.toAbsolutePath()).reset());
    }

    private static String generateDesktopEntryContent(IdeInfo ideInfo, String caminhoRaiz, AtalhoInfo atalho) {
        Path binariosDir = Path.of(caminhoRaiz, "binarios");
        Path idePath = binariosDir.resolve(ideInfo.getNome()).resolve(ideInfo.getVersao());

        return String.format(
                """
                        [Desktop Entry]
                        Version=1.0
                        Type=Application
                        Name=%s
                        Icon=%s
                        Exec="%s" %%f
                        Comment=%s
                        Categories=Development;IDE;
                        Terminal=false
                        StartupWMClass=%s
                        StartupNotify=true
                        """,
                atalho.getDesktopName(),
                idePath.resolve("bin").resolve(atalho.getDesktopIcon()).toAbsolutePath(),
                idePath.resolve("bin").resolve(atalho.getDesktopExec()).toAbsolutePath(),
                atalho.getDesktopComment(),
                atalho.getDesktopWmClass()
        );
    }

    private void gerarAtalhoWindows(IdeInfo ideInfo, Path diretorioAtalho) {
        String executavel = ideInfo.getNome().equals("intellij-idea") ? "idea64.exe" : ideInfo.getNome() + "64.exe";
        Path caminhoExecutavel = ideInfo.getCaminhoBinario().resolve("bin").resolve(executavel);

        if (Files.notExists(caminhoExecutavel)) {
            log.warn("Executável '{}' não encontrado. Não é possível criar o atalho.", caminhoExecutavel);
            return;
        }

        String nomeAtalho = new AtalhoInfo(ideInfo).getDesktopName() + ".lnk";
        Path caminhoAtalho = diretorioAtalho.resolve(nomeAtalho);

        log.info("Tentando criar atalho em: {}", caminhoAtalho);

        String powerShellCommand =
                "$ws = New-Object -ComObject WScript.Shell; " +
                        "$s = $ws.CreateShortcut('" + caminhoAtalho.toAbsolutePath() + "'); " +
                        "$s.TargetPath = '" + caminhoExecutavel.toAbsolutePath() + "'; " +
                        "$s.Save();";

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("powershell.exe", "-Command", powerShellCommand);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println(ansi().fg(Ansi.Color.CYAN).a("✓ Atalho criado com sucesso em: " + caminhoAtalho.toAbsolutePath()).reset());
            } else {
                String stderr = new String(process.getErrorStream().readAllBytes());
                log.error("Falha ao criar atalho. Código de saída: {}. Erro: {}", exitCode, stderr);
                System.out.println(ansi().fg(Ansi.Color.RED).a("✗ Falha ao criar o atalho.").reset());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Exceção ao tentar criar atalho para {}", ideInfo.getNome(), e);
            Thread.currentThread().interrupt();
        }
    }
}
