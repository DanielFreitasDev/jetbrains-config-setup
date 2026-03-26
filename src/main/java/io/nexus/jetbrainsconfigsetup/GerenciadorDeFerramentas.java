package io.nexus.jetbrainsconfigsetup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

/**
 * Classe responsável por configurar as ferramentas e chaves da JetBrains.
 */
@Slf4j
public class GerenciadorDeFerramentas {

    private static final String NOME_RECURSO_JETBRAINS_TOOL = "jetbrains-tool.jar.base64";
    private static final String NOME_RECURSO_CHAVES = "idea.tar.gz.base64";
    private static final String NOME_RECURSO_FERRAMENTA_2026 = "enc-sniarbtej-2026.01.00.jar.base64";
    private static final String NOME_ARQUIVO_JETBRAINS_TOOL = "jetbrains-tool.jar";
    private static final String NOME_ARQUIVO_FERRAMENTA_2026 = "enc-sniarbtej-2026.01.00.jar";

    private static final List<String> NOMES_CHAVES_A_GERAR = List.of(
            "pycharm.key",
            "rubymine.key",
            "webstorm.key",
            "datagrip.key"
    );

    /**
     * Orquestra a criação dos arquivos de ferramenta e a instalação das chaves.
     *
     * @param caminhoRaiz O diretório base da aplicação.
     */
    public void configurar(String caminhoRaiz) {
        log.info("Iniciando a configuração das ferramentas JetBrains.");
        Path diretorioFerramentas = Paths.get(caminhoRaiz, "ferramentas", "ferramenta-atual");

        try {
            Files.createDirectories(diretorioFerramentas);
            criarArquivoDecodificado(diretorioFerramentas, NOME_RECURSO_JETBRAINS_TOOL, NOME_ARQUIVO_JETBRAINS_TOOL);
            criarArquivoDecodificado(diretorioFerramentas, NOME_RECURSO_FERRAMENTA_2026, NOME_ARQUIVO_FERRAMENTA_2026);
            instalarChaves(diretorioFerramentas);
            log.info("Configuração das ferramentas JetBrains finalizada com sucesso.");
        } catch (IOException e) {
            log.error("Falha crítica durante a configuração das ferramentas.", e);
        }
    }

    /**
     * Lê um recurso Base64 do classpath, decodifica seu conteúdo e grava o arquivo correspondente no diretório de destino.
     * Este método centraliza a leitura das ferramentas empacotadas em {@code src/main/resources},
     * garantindo que a origem do binário não fique mais embutida diretamente no código Java.
     *
     * @param diretorioDestino O diretório onde o arquivo decodificado será salvo.
     * @param nomeRecursoBase64 O nome do recurso Base64 presente no classpath.
     * @param nomeArquivoDestino O nome final do arquivo gerado no disco.
     * @throws IOException Se ocorrer um erro ao ler o recurso, decodificar o Base64 ou gravar o arquivo.
     */
    private void criarArquivoDecodificado(Path diretorioDestino, String nomeRecursoBase64, String nomeArquivoDestino) throws IOException {
        // Cada arquivo é carregado do classpath para permitir manutenção simples dos binários codificados em Base64.
        byte[] conteudoDecodificado = lerBytesDoRecursoBase64(nomeRecursoBase64);

        // O arquivo é sempre regravado para refletir exatamente o conteúdo empacotado na versão atual da aplicação.
        Path caminhoArquivo = diretorioDestino.resolve(nomeArquivoDestino);
        Files.write(caminhoArquivo, conteudoDecodificado);
        log.info("Arquivo '{}' criado em: {}", nomeArquivoDestino, caminhoArquivo);
    }

    /**
     * Cria o diretório de chaves, descompacta o .tar.gz e replica os arquivos .key.
     *
     * @param diretorioPai O diretório 'ferramenta-atual'.
     * @throws IOException Se ocorrer um erro de I/O.
     */
    private void instalarChaves(Path diretorioPai) throws IOException {
        Path diretorioChaves = diretorioPai.resolve("chaves");
        Files.createDirectories(diretorioChaves);
        log.debug("Diretório de chaves garantido em: {}", diretorioChaves);

        byte[] tarGzBytes = lerBytesDoRecursoBase64(NOME_RECURSO_CHAVES);
        try (InputStream inputStream = new ByteArrayInputStream(tarGzBytes)) {
            descompactarTarGz(inputStream, diretorioChaves);
        }
        log.info("Arquivo 'idea.tar.gz' descompactado com sucesso.");

        Path ideaKeyPath = diretorioChaves.resolve("idea.key");
        if (Files.exists(ideaKeyPath)) {
            replicarChavePrincipal(ideaKeyPath, diretorioChaves);
        } else {
            log.error("Arquivo 'idea.key' não foi encontrado no 'idea.tar.gz' após a descompressão.");
        }
    }

    /**
     * Lê um recurso Base64 do classpath e devolve o conteúdo já decodificado.
     * A normalização remove quebras de linha e outros espaços em branco para permitir
     * que os arquivos em {@code resources} sejam formatados sem impactar a decodificação.
     *
     * @param nomeRecursoBase64 O nome do recurso Base64 presente no classpath.
     * @return Conteúdo binário decodificado do recurso informado.
     * @throws IOException Se o recurso não existir, estiver vazio ou contiver Base64 inválido.
     */
    private byte[] lerBytesDoRecursoBase64(String nomeRecursoBase64) throws IOException {
        // O recurso precisa ser lido pelo classloader para funcionar tanto no ambiente de desenvolvimento quanto no JAR empacotado.
        try (InputStream inputStream = GerenciadorDeFerramentas.class.getClassLoader().getResourceAsStream(nomeRecursoBase64)) {
            if (inputStream == null) {
                throw new IOException("Recurso Base64 não encontrado no classpath: " + nomeRecursoBase64);
            }

            // A limpeza dos espaços evita falha de decodificação quando o arquivo possui quebras de linha.
            String conteudoBase64 = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s", "");
            if (conteudoBase64.isBlank()) {
                throw new IOException("Recurso Base64 vazio: " + nomeRecursoBase64);
            }

            try {
                return Base64.getDecoder().decode(conteudoBase64);
            } catch (IllegalArgumentException e) {
                throw new IOException("Conteúdo Base64 inválido no recurso: " + nomeRecursoBase64, e);
            }
        }
    }

    /**
     * Extrai o conteúdo de um stream .tar.gz para um diretório de destino.
     *
     * @param inputStream O stream de dados do arquivo.
     * @param diretorioDestino O local para extrair os arquivos.
     * @throws IOException Se ocorrer um erro durante a descompressão.
     */
    private void descompactarTarGz(InputStream inputStream, Path diretorioDestino) throws IOException {
        log.debug("Iniciando descompressão do arquivo .tar.gz...");
        try (GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(inputStream);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                Path destinoArquivo = diretorioDestino.resolve(entry.getName()).normalize();

                // Medida de segurança para evitar ataques "Zip Slip"
                if (!destinoArquivo.startsWith(diretorioDestino)) {
                    throw new IOException("Entrada de TAR maliciosa detectada: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(destinoArquivo);
                } else {
                    Files.createDirectories(destinoArquivo.getParent());
                    try (OutputStream out = Files.newOutputStream(destinoArquivo)) {
                        tarIn.transferTo(out);
                    }
                }
                log.info("Extraído: {}", destinoArquivo);
            }
        }
    }

    /**
     * Lê o arquivo idea.key e cria cópias com os nomes especificados.
     *
     * @param chavePrincipal O caminho para o arquivo idea.key.
     * @param diretorioChaves O diretório onde as novas chaves serão criadas.
     * @throws IOException Se ocorrer um erro de leitura ou escrita.
     */
    private void replicarChavePrincipal(Path chavePrincipal, Path diretorioChaves) throws IOException {
        log.debug("Lendo conteúdo da chave principal: {}", chavePrincipal);
        byte[] conteudoChave = Files.readAllBytes(chavePrincipal);

        for (String nomeChave : NOMES_CHAVES_A_GERAR) {
            Path novaChavePath = diretorioChaves.resolve(nomeChave);
            Files.write(novaChavePath, conteudoChave);
            log.info("Chave gerada: {}", novaChavePath);
        }
    }
}
