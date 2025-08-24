package io.nexus.jetbrainsconfigsetup;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.fusesource.jansi.Ansi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fusesource.jansi.Ansi.ansi;

@Slf4j
public class GerenciadorDeInstalacao {

    private static final String DIRETORIO_INSTALADORES = "instaladores";
    private static final String DIRETORIO_BINARIOS = "binarios";
    private static final String DIRETORIO_CONFIGURACOES = "configuracoes";
    private static final String DIRETORIO_ATALHOS = "atalhos";

    private static final Pattern VERSAO_PATTERN = Pattern.compile("(\\d{4}\\.\\d+(\\.\\d+)?)");
    private static final Gson gson = new Gson();
    private final Scanner scanner = new Scanner(System.in);
    private final GerenciadorDeConfiguracao gerenciadorDeConfiguracao = new GerenciadorDeConfiguracao();


    public void instalar(String caminhoRaiz) {
        Path pastaInstaladores = Paths.get(caminhoRaiz, DIRETORIO_INSTALADORES);
        if (Files.notExists(pastaInstaladores)) {
            log.warn("Diretório de instaladores não encontrado: {}", pastaInstaladores);
            return;
        }

        try (Stream<Path> stream = Files.list(pastaInstaladores)) {
            List<Path> arquivosTarGz = stream
                    .filter(p -> p.toString().endsWith(".tar.gz"))
                    .collect(Collectors.toList());

            if (arquivosTarGz.isEmpty()) {
                log.info("Nenhum arquivo de instalação (.tar.gz) encontrado em '{}'.", pastaInstaladores);
                return;
            }

            List<IdeInfo> idesParaInstalar = exibirMenuDeSelecao(arquivosTarGz, caminhoRaiz);
            if (idesParaInstalar.isEmpty()) {
                log.info("Nenhuma IDE selecionada para instalação.");
                return;
            }

            boolean gerarAtalhos = perguntarSobreAtalhos();
            Path diretorioAtalhos = null;
            if (gerarAtalhos) {
                diretorioAtalhos = escolherLocalAtalhos(caminhoRaiz);
            }

            for (IdeInfo ide : idesParaInstalar) {
                processarArquivo(ide, caminhoRaiz, diretorioAtalhos);
            }

        } catch (IOException e) {
            log.error("Erro ao listar arquivos no diretório de instaladores.", e);
        }
    }

    private List<IdeInfo> exibirMenuDeSelecao(List<Path> arquivos, String caminhoRaiz) {
        System.out.println(ansi().fg(Ansi.Color.CYAN).a("\n--- Menu de Instalação de IDEs JetBrains ---").reset());
        System.out.println("Foram encontrados os seguintes arquivos em sua pasta 'instaladores':\n");

        for (int i = 0; i < arquivos.size(); i++) {
            System.out.println(ansi().fg(Ansi.Color.YELLOW).a(String.format("  [%2d] ", i + 1)).reset().a(arquivos.get(i).getFileName()));
        }

        System.out.println(ansi().fg(Ansi.Color.YELLOW).a(String.format("  [%2d] ", arquivos.size() + 1)).reset().fg(Ansi.Color.GREEN).a("Instalar Todos").reset());
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a(String.format("  [%2d] ", 0)).reset().fg(Ansi.Color.RED).a("Sair").reset());
        System.out.print("\nDigite os números dos arquivos que deseja instalar (separados por vírgula) ou uma das opções: ");

        String input = scanner.nextLine().trim();

        if (input.equals(String.valueOf(arquivos.size() + 1))) {
            return arquivos.stream()
                    .map(this::extrairIdeInfo)
                    .filter(ide -> confirmarSubstituicao(ide, caminhoRaiz))
                    .collect(Collectors.toList());
        }
        if (input.equals("0")) {
            return Collections.emptyList();
        }

        try {
            return Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .filter(i -> i > 0 && i <= arquivos.size())
                    .mapToObj(i -> arquivos.get(i - 1))
                    .distinct()
                    .map(this::extrairIdeInfo)
                    .filter(ide -> confirmarSubstituicao(ide, caminhoRaiz))
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            System.out.println(ansi().fg(Ansi.Color.RED).a("Entrada inválida. Por favor, insira apenas números.").reset());
            return Collections.emptyList();
        }
    }

    private boolean confirmarSubstituicao(IdeInfo ideInfo, String caminhoRaiz) {
        Path diretorioBinario = Paths.get(caminhoRaiz, DIRETORIO_BINARIOS, ideInfo.getNome(), ideInfo.getVersao());
        if (Files.exists(diretorioBinario)) {
            System.out.print(ansi().fg(Ansi.Color.YELLOW).a("A IDE " + ideInfo.getNome() + " versão " + ideInfo.getVersao() + " já existe. Deseja substituir? (S/n): ").reset());
            String resposta = scanner.nextLine().trim().toLowerCase();
            if ("n".equals(resposta)) {
                System.out.println("Instalação de " + ideInfo.getNome() + " ignorada.");
                return false;
            }
            // Se for substituir, limpa os diretórios
            try {
                Path diretorioConfig = Paths.get(caminhoRaiz, DIRETORIO_CONFIGURACOES, ideInfo.getNome(), ideInfo.getVersao());
                verificarExistenciaDiretorio(diretorioBinario);
                verificarExistenciaDiretorio(diretorioConfig);
                log.info("Diretórios antigos de {} removidos.", ideInfo.getNome());
            } catch (IOException e) {
                log.error("Falha ao limpar diretórios antigos para {}", ideInfo.getNome(), e);
                System.out.println(ansi().fg(Ansi.Color.RED).a("Erro ao limpar instalação existente. Verifique os logs.").reset());
                return false;
            }
        }
        return true;
    }

    private void verificarExistenciaDiretorio(Path diretorio) throws IOException {
        if (Files.exists(diretorio)) {
            try (Stream<Path> walk = Files.walk(diretorio)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.error("Falha ao deletar {}", path, e);
                    }
                });
            }
        }
    }

    private boolean perguntarSobreAtalhos() {
        System.out.print(ansi().fg(Ansi.Color.CYAN).a("\nDeseja gerar atalhos para as IDEs selecionadas? (S/n): ").reset());
        String resposta = scanner.nextLine().trim().toLowerCase();
        return !"n".equals(resposta);
    }

    private Path escolherLocalAtalhos(String caminhoRaiz) {
        System.out.println(ansi().fg(Ansi.Color.CYAN).a("\nOnde você deseja criar os atalhos?").reset());
        Path atalhosPath = Paths.get(caminhoRaiz, DIRETORIO_ATALHOS);
        System.out.println("  [1] Na pasta 'atalhos' do projeto (" + atalhosPath.toAbsolutePath() + ")");
        System.out.println("  [2] No diretório de aplicações do sistema (~/.local/share/applications/)");
        System.out.print("Escolha uma opção: ");
        String escolha = scanner.nextLine().trim();

        if ("2".equals(escolha)) {
            return Paths.get(System.getProperty("user.home"), ".local", "share", "applications");
        }
        return atalhosPath;
    }

    private void processarArquivo(IdeInfo ideInfo, String caminhoRaiz, Path diretorioAtalhos) {
        log.info("Processando arquivo: {}", ideInfo.getCaminhoArquivo().getFileName());

        try {
            Files.createDirectories(ideInfo.getCaminhoBinario());
            System.out.println(ansi().fg(Ansi.Color.BLUE).a("-> Descompactando " + ideInfo.getNome() + " para " + ideInfo.getCaminhoBinario()).reset());
            descompactarTarGz(ideInfo.getCaminhoArquivo(), ideInfo.getCaminhoBinario());
            System.out.println(ansi().fg(Ansi.Color.GREEN).a("✓ IDE " + ideInfo.getNome() + " versão " + ideInfo.getVersao() + " instalada com sucesso.").reset());

            // Chama o gerenciador de configuração
            gerenciadorDeConfiguracao.configurarIde(ideInfo, caminhoRaiz, diretorioAtalhos);

        } catch (IOException e) {
            log.error("Falha ao criar diretório ou descompactar o arquivo {}", ideInfo.getCaminhoArquivo().getFileName(), e);
        }
    }

    private IdeInfo extrairIdeInfo(Path arquivo) {
        String nomeArquivo = arquivo.getFileName().toString();
        String nomeIde = determinarNomeIde(nomeArquivo);
        String versao = extrairVersaoDoNome(nomeArquivo)
                .or(() -> extrairVersaoDoProductInfo(arquivo))
                .orElseGet(() -> new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()));

        return new IdeInfo(arquivo, nomeIde, versao);
    }

    private String determinarNomeIde(String nomeArquivo) {
        String nomeLimpo = nomeArquivo.toLowerCase().replace(".tar.gz", "");
        if (nomeLimpo.startsWith("pycharm")) return "pycharm";
        if (nomeLimpo.startsWith("datagrip")) return "datagrip";
        if (nomeLimpo.startsWith("ideaiu") || nomeLimpo.startsWith("ideaic")) return "intellij-idea";
        if (nomeLimpo.startsWith("rubymine")) return "rubymine";
        if (nomeLimpo.startsWith("webstorm")) return "webstorm";
        return nomeLimpo.replaceAll("[-_].*", ""); // Fallback
    }

    private Optional<String> extrairVersaoDoNome(String nomeArquivo) {
        Matcher matcher = VERSAO_PATTERN.matcher(nomeArquivo);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    private Optional<String> extrairVersaoDoProductInfo(Path arquivo) {
        try (InputStream fis = Files.newInputStream(arquivo);
             GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(fis);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                if (entry.getName().endsWith("product-info.json")) {
                    log.debug("Encontrado product-info.json em: {}", entry.getName());
                    String jsonContent = new BufferedReader(new InputStreamReader(tarIn, StandardCharsets.UTF_8))
                            .lines()
                            .collect(Collectors.joining("\n"));

                    ProductInfo productInfo = gson.fromJson(jsonContent, ProductInfo.class);
                    return Optional.ofNullable(productInfo.getVersion());
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            log.error("Não foi possível ler o product-info.json do arquivo {}", arquivo.getFileName(), e);
        }
        return Optional.empty();
    }

    private void descompactarTarGz(Path arquivoOrigem, Path diretorioDestino) throws IOException {
        try (InputStream fis = Files.newInputStream(arquivoOrigem);
             GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(fis);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                String entryName = entry.getName();
                int firstSlash = entryName.indexOf('/');
                if (firstSlash != -1) {
                    entryName = entryName.substring(firstSlash + 1);
                }
                if (entryName.isEmpty()) {
                    continue;
                }

                Path destinoArquivo = diretorioDestino.resolve(entryName).normalize();

                if (!destinoArquivo.startsWith(diretorioDestino)) {
                    throw new IOException("Entrada de TAR maliciosa (Zip Slip) detectada: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(destinoArquivo);
                } else {
                    Files.createDirectories(destinoArquivo.getParent());
                    try (OutputStream out = Files.newOutputStream(destinoArquivo)) {
                        tarIn.transferTo(out);
                    }
                }

                // Preserva as permissões do arquivo, se não estiver no Windows
                if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                    try {
                        int mode = entry.getMode();
                        Set<PosixFilePermission> permissions = modeToPermissionsSet(mode);
                        Files.setPosixFilePermissions(destinoArquivo, permissions);
                    } catch (UnsupportedOperationException e) {
                        log.warn("Não foi possível definir as permissões POSIX para {}: {}", destinoArquivo, e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Converte um modo de permissão numérico (estilo Unix) para um Set de PosixFilePermission.
     *
     * @param mode O modo numérico do arquivo.
     * @return Um Set contendo as permissões POSIX correspondentes.
     */
    private Set<PosixFilePermission> modeToPermissionsSet(int mode) {
        Set<PosixFilePermission> permissions = new HashSet<>();

        // Permissões do Dono (Owner)
        if ((mode & 256) > 0) permissions.add(PosixFilePermission.OWNER_READ);
        if ((mode & 128) > 0) permissions.add(PosixFilePermission.OWNER_WRITE);
        if ((mode & 64) > 0) permissions.add(PosixFilePermission.OWNER_EXECUTE);

        // Permissões do Grupo (Group)
        if ((mode & 32) > 0) permissions.add(PosixFilePermission.GROUP_READ);
        if ((mode & 16) > 0) permissions.add(PosixFilePermission.GROUP_WRITE);
        if ((mode & 8) > 0) permissions.add(PosixFilePermission.GROUP_EXECUTE);

        // Permissões de Outros (Others)
        if ((mode & 4) > 0) permissions.add(PosixFilePermission.OTHERS_READ);
        if ((mode & 2) > 0) permissions.add(PosixFilePermission.OTHERS_WRITE);
        if ((mode & 1) > 0) permissions.add(PosixFilePermission.OTHERS_EXECUTE);

        return permissions;
    }
}