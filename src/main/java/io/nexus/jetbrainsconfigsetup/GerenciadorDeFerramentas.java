package io.nexus.jetbrainsconfigsetup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private static final String JETBRAINS_TOOL_BASE64 = "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";
    private static final String KEYS_BASE64 = "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";

    private static final List<String> NOMES_CHAVES_A_GERAR = List.of(
            "pycharm.key",
            "rubymine.key",
            "webstorm.key",
            "datagrip.key"
    );

    /**
     * Orquestra a criação do .jar da ferramenta e a instalação das chaves.
     *
     * @param caminhoRaiz O diretório base da aplicação.
     */
    public void configurar(String caminhoRaiz) {
        log.info("Iniciando a configuração das ferramentas JetBrains.");
        Path diretorioFerramentas = Paths.get(caminhoRaiz, "ferramentas", "ferramenta-atual");

        try {
            Files.createDirectories(diretorioFerramentas);
            criarJarFerramenta(diretorioFerramentas);
            instalarChaves(diretorioFerramentas);
            log.info("Configuração das ferramentas JetBrains finalizada com sucesso.");
        } catch (IOException e) {
            log.error("Falha crítica durante a configuração das ferramentas.", e);
        }
    }

    /**
     * Decodifica a string Base64 e cria o arquivo jetbrains-tool.jar.
     *
     * @param diretorioDestino O diretório onde o .jar será salvo.
     * @throws IOException Se ocorrer um erro de I/O.
     */
    private void criarJarFerramenta(Path diretorioDestino) throws IOException {
        if (isBase64Invalido(JETBRAINS_TOOL_BASE64, "JETBRAINS_TOOL_BASE64")) {
            return;
        }

        log.debug("Decodificando e criando o arquivo jetbrains-tool.jar.");
        try {
            byte[] jarBytes = Base64.getDecoder().decode(JETBRAINS_TOOL_BASE64);
            Path jarPath = diretorioDestino.resolve("jetbrains-tool.jar");
            Files.write(jarPath, jarBytes);
            log.info("Arquivo 'jetbrains-tool.jar' criado em: {}", jarPath);
        } catch (IllegalArgumentException e) {
            log.error("A string JETBRAINS_TOOL_BASE64 fornecida não é um Base64 válido.", e);
        }
    }

    /**
     * Cria o diretório de chaves, descompacta o .tar.gz e replica os arquivos .key.
     *
     * @param diretorioPai O diretório 'ferramenta-atual'.
     * @throws IOException Se ocorrer um erro de I/O.
     */
    private void instalarChaves(Path diretorioPai) throws IOException {
        if (isBase64Invalido(KEYS_BASE64, "KEYS_BASE64")) {
            return;
        }

        Path diretorioChaves = diretorioPai.resolve("chaves");
        Files.createDirectories(diretorioChaves);
        log.debug("Diretório de chaves garantido em: {}", diretorioChaves);

        try {
            byte[] tarGzBytes = Base64.getDecoder().decode(KEYS_BASE64);
            descompactarTarGz(new ByteArrayInputStream(tarGzBytes), diretorioChaves);
            log.info("Arquivo 'idea.tar.gz' descompactado com sucesso.");

            Path ideaKeyPath = diretorioChaves.resolve("idea.key");
            if (Files.exists(ideaKeyPath)) {
                replicarChavePrincipal(ideaKeyPath, diretorioChaves);
            } else {
                log.error("Arquivo 'idea.key' não foi encontrado no 'idea.tar.gz' após a descompressão.");
            }
        } catch (IllegalArgumentException e) {
            log.error("A string KEYS_BASE64 fornecida não é um Base64 válido.", e);
        }
    }

    /**
     * Extrai o conteúdo de um stream .tar.gz para um diretório de destino.
     *
     * @param inputStream      O stream de dados do arquivo.
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
     * @param chavePrincipal  O caminho para o arquivo idea.key.
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

    /**
     * Verifica se a string Base64 é nula, vazia ou contém o valor padrão.
     *
     * @param base64       A string a ser verificada.
     * @param nomeVariavel O nome da variável para o log.
     * @return true se a string for inválida, false caso contrário.
     */
    private boolean isBase64Invalido(String base64, String nomeVariavel) {
        if (base64 == null || base64.isBlank() || "seu_base64_aqui".equals(base64)) {
            log.warn("A string Base64 para {} está vazia ou com o valor padrão. A operação será ignorada.", nomeVariavel);
            return true;
        }
        return false;
    }
}