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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fusesource.jansi.Ansi.ansi;

@Slf4j
public class GerenciadorDeInstalacao {

    private static final String DIRETORIO_INSTALADORES = "instaladores";
    private static final String DIRETORIO_BINARIOS = "binarios";
    private static final Pattern VERSAO_PATTERN = Pattern.compile("(\\d{4}\\.\\d+(\\.\\d+)?)");
    private static final Gson gson = new Gson();

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

            List<Path> arquivosSelecionados = exibirMenuDeSelecao(arquivosTarGz);
            if (arquivosSelecionados.isEmpty()) {
                log.info("Nenhum arquivo selecionado para instalação.");
                return;
            }

            for (Path arquivo : arquivosSelecionados) {
                processarArquivo(arquivo, Paths.get(caminhoRaiz, DIRETORIO_BINARIOS));
            }

        } catch (IOException e) {
            log.error("Erro ao listar arquivos no diretório de instaladores.", e);
        }
    }

    private List<Path> exibirMenuDeSelecao(List<Path> arquivos) {
        System.out.println(ansi().fg(Ansi.Color.CYAN).a("\n--- Menu de Instalação de IDEs JetBrains ---").reset());
        System.out.println("Foram encontrados os seguintes arquivos em sua pasta 'instaladores':\n");

        for (int i = 0; i < arquivos.size(); i++) {
            System.out.println(ansi().fg(Ansi.Color.YELLOW).a(String.format("  [%2d] ", i + 1)).reset().a(arquivos.get(i).getFileName()));
        }

        System.out.println(ansi().fg(Ansi.Color.YELLOW).a(String.format("  [%2d] ", arquivos.size() + 1)).reset().fg(Ansi.Color.GREEN).a("Instalar Todos").reset());
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a(String.format("  [%2d] ", 0)).reset().fg(Ansi.Color.RED).a("Sair").reset());
        System.out.print("\nDigite os números dos arquivos que deseja instalar (separados por vírgula) ou uma das opções: ");

        try (Scanner scanner = new Scanner(System.in)) {
            String input = scanner.nextLine().trim();

            if (input.equals(String.valueOf(arquivos.size() + 1))) {
                return arquivos;
            }
            if (input.equals("0")) {
                return Collections.emptyList();
            }

            return Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .filter(i -> i > 0 && i <= arquivos.size())
                    .mapToObj(i -> arquivos.get(i - 1))
                    .distinct()
                    .collect(Collectors.toList());

        } catch (NumberFormatException e) {
            System.out.println(ansi().fg(Ansi.Color.RED).a("Entrada inválida. Por favor, insira apenas números.").reset());
            return Collections.emptyList();
        }
    }

    private void processarArquivo(Path arquivo, Path pastaBinarios) {
        log.info("Processando arquivo: {}", arquivo.getFileName());
        IdeInfo ideInfo = extrairIdeInfo(arquivo);
        Path diretorioDestino = pastaBinarios.resolve(ideInfo.getNome()).resolve(ideInfo.getVersao());

        try {
            Files.createDirectories(diretorioDestino);
            log.info("Descompactando {} para {}", arquivo.getFileName(), diretorioDestino);
            descompactarTarGz(arquivo, diretorioDestino);
            log.info("IDE {} versão {} instalada com sucesso em {}", ideInfo.getNome(), ideInfo.getVersao(), diretorioDestino);
        } catch (IOException e) {
            log.error("Falha ao criar diretório ou descompactar o arquivo {}", arquivo.getFileName(), e);
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
                // O conteúdo do tar.gz geralmente está dentro de uma pasta raiz, ex: "idea-IU-231.8109.175/".
                // Precisamos remover esse primeiro nível para descompactar o conteúdo diretamente.
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
            }
        }
    }
}