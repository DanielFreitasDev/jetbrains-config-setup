package io.nexus.jetbrainsconfigsetup;

import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Classe responsável por baixar as versões mais recentes das IDEs da JetBrains.
 */
@Slf4j
public class GerenciadorDeDownloads {

    // URL base para download dos produtos JetBrains
    private static final String BASE_URL_DOWNLOAD = "https://download.jetbrains.com/product?code=%s&latest&distribution=%s";
    // Padrão regex para extrair o nome do arquivo do header Content-Disposition
    private static final Pattern NOME_ARQUIVO_PATTERN = Pattern.compile("filename=\"?(.+?)\"?$");
    private static final String DIRETORIO_INSTALADORES = "instaladores";

    private final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    /**
     * Baixa uma lista de IDEs para o diretório de instaladores.
     *
     * @param idesParaBaixar A lista de {@link TipoIde} selecionadas para download.
     * @param caminhoRaiz    O diretório raiz da aplicação.
     * @return Uma lista de {@link Path} para os arquivos que foram baixados com sucesso.
     */
    public List<Path> baixarIdes(List<TipoIde> idesParaBaixar, String caminhoRaiz) {
        Path pastaInstaladores = Paths.get(caminhoRaiz, DIRETORIO_INSTALADORES);
        List<Path> arquivosBaixados = new ArrayList<>();

        if (idesParaBaixar.isEmpty()) {
            System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Nenhuma IDE selecionada para download.").reset());
            return arquivosBaixados;
        }

        System.out.println(ansi().fg(Ansi.Color.CYAN).a("\nIniciando processo de download...").reset());

        for (TipoIde ide : idesParaBaixar) {
            try {
                // Determina a distribuição (linux ou windowsZip)
                String distribuicao = isWindows ? "windowsZip" : "linux";
                String urlDownload = String.format(BASE_URL_DOWNLOAD, ide.getCodigoProduto(), distribuicao);

                System.out.println(ansi().fg(Ansi.Color.WHITE).a("Preparando download para: " + ide.getNomeAmigavel()).reset());

                // O método baixarArquivo trata a conexão, extração do nome e o download
                Path arquivoDestino = baixarArquivo(urlDownload, pastaInstaladores, ide.getNomeAmigavel());

                if (arquivoDestino != null) {
                    arquivosBaixados.add(arquivoDestino);
                    System.out.println(ansi().fg(Ansi.Color.GREEN).a("\n✓ Download de " + ide.getNomeAmigavel() + " concluído!").reset());
                } else {
                    System.out.println(ansi().fg(Ansi.Color.RED).a("\n✗ Falha no download de " + ide.getNomeAmigavel()).reset());
                }

            } catch (IOException e) {
                log.error("Erro ao tentar baixar a IDE: {}", ide.getNomeAmigavel(), e);
                System.out.println(ansi().fg(Ansi.Color.RED).a("Erro crítico ao baixar " + ide.getNomeAmigavel() + ". Verifique os logs.").reset());
            }
        }
        return arquivosBaixados;
    }

    /**
     * Executa o download de um arquivo a partir de uma URL, exibindo o progresso.
     *
     * @param urlString        A URL direta para o download.
     * @param diretorioDestino O diretório onde o arquivo será salvo.
     * @param nomeIde          O nome amigável da IDE (para logs).
     * @return O Path do arquivo baixado, ou null se falhar.
     * @throws IOException Se ocorrer um erro de conexão ou I/O.
     */
    private Path baixarArquivo(String urlString, Path diretorioDestino, String nomeIde) throws IOException {
        URI uri = URI.create(urlString);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setInstanceFollowRedirects(true); // Seguir redirecionamentos
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            log.error("Falha no download de {}. Código de resposta: {}", nomeIde, responseCode);
            return null;
        }

        // Tenta extrair o nome do arquivo do header
        String nomeArquivo = getNomeArquivo(conn);
        if (nomeArquivo == null) {
            log.error("Não foi possível determinar o nome do arquivo para {}", nomeIde);
            // Fallback: extrai da URL final (menos confiável)
            String urlFinal = conn.getURL().getPath();
            log.info("URL final: {}", urlFinal);
            nomeArquivo = urlFinal.substring(urlFinal.lastIndexOf('/') + 1);
            if (nomeArquivo.isEmpty()) {
                log.error("Falha total ao determinar nome do arquivo para {}.", nomeIde);
                return null;
            }
            log.warn("Usando nome do arquivo fallback da URL: {}", nomeArquivo);
        }

        Path arquivoDestino = diretorioDestino.resolve(nomeArquivo);
        long tamanhoTotal = conn.getContentLengthLong();

        System.out.println("  Salvando em: " + arquivoDestino);
        System.out.println("  Tamanho: " + (tamanhoTotal / 1024 / 1024) + " MB");

        // Bloco try-with-resources para garantir que os streams sejam fechados
        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(arquivoDestino)) {

            byte[] buffer = new byte[8192];
            int bytesLidos;
            long totalLido = 0;
            int progressoAnterior = -1;

            while ((bytesLidos = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesLidos);
                totalLido += bytesLidos;

                if (tamanhoTotal > 0) {
                    // Calcula o progresso
                    int progressoAtual = (int) ((totalLido * 100) / tamanhoTotal);
                    // Atualiza a linha do console apenas se o progresso mudar
                    if (progressoAtual != progressoAnterior) {
                        System.out.print(ansi().cursorUp(1).eraseLine()
                                .a("  Baixando: " + progressoAtual + "%")
                                .a(" [" + "=".repeat(progressoAtual / 2) + ">" + " ".repeat(50 - (progressoAtual / 2)) + "]")
                                .newline());
                        progressoAnterior = progressoAtual;
                    }
                }
            }
            out.flush(); // Garante que tudo foi escrito
        } finally {
            conn.disconnect();
        }

        return arquivoDestino;
    }

    /**
     * Extrai o nome do arquivo do header 'Content-Disposition' da conexão HTTP.
     *
     * @param conn A {@link HttpURLConnection} ativa.
     * @return O nome do arquivo, ou null se não for encontrado.
     */
    private String getNomeArquivo(HttpURLConnection conn) {
        String headerDisposition = conn.getHeaderField("Content-Disposition");
        if (headerDisposition != null && !headerDisposition.isEmpty()) {
            Matcher matcher = NOME_ARQUIVO_PATTERN.matcher(headerDisposition);
            if (matcher.find()) {
                String nome = matcher.group(1).replace("\"", "");
                log.debug("Nome do arquivo extraído do Content-Disposition: {}", nome);
                return nome;
            }
        }
        log.warn("Header Content-Disposition não encontrado ou inválido.");
        return null;
    }
}