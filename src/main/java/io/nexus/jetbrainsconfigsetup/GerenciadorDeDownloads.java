package io.nexus.jetbrainsconfigsetup;

import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Classe responsável por baixar as versões mais recentes das IDEs da JetBrains.
 */
@Slf4j
public class GerenciadorDeDownloads {

    // URL base para download dos produtos JetBrains
    private static final String BASE_URL_DOWNLOAD = "https://download.jetbrains.com/product?code=%s&latest&distribution=%s";
    private static final String DIRETORIO_INSTALADORES = "instaladores";
    private static final String[] VARIAVEIS_PROXY_HTTPS = {"https_proxy", "HTTPS_PROXY", "http_proxy", "HTTP_PROXY"};
    private static final String[] VARIAVEIS_PROXY_HTTP = {"http_proxy", "HTTP_PROXY"};

    private final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    /**
     * Estrutura interna com os dados necessários para abrir conexão via proxy.
     *
     * @param proxy           Objeto Proxy pronto para ser usado na conexão.
     * @param nomeVariavel    Nome da variável de ambiente que originou a configuração.
     * @param host            Host do proxy.
     * @param porta           Porta do proxy.
     * @param usuario         Usuário do proxy (opcional).
     * @param senha           Senha do proxy (opcional).
     */
    private record ConfiguracaoProxy(Proxy proxy, String nomeVariavel, String host, int porta, String usuario, String senha) {
    }

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
        HttpURLConnection conn = abrirConexaoComSuporteAProxy(uri, nomeIde);
        conn.setInstanceFollowRedirects(true); // Seguir redirecionamentos
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            log.error("Falha no download de {}. Código de resposta: {}", nomeIde, responseCode);
            conn.disconnect();
            return null;
        }

        // --- Lógica de Nome de Arquivo ---
        // Pega o nome do arquivo diretamente da URL final (redirecionada)
        String urlFinal = conn.getURL().getPath();
        log.debug("URL final detectada: {}", urlFinal);
        String nomeArquivo = urlFinal.substring(urlFinal.lastIndexOf('/') + 1);

        if (nomeArquivo.isEmpty()) {
            log.error("Falha total ao determinar o nome do arquivo para {}. URL final: {}", nomeIde, urlFinal);
            conn.disconnect();
            return null;
        }
        log.info("Nome do arquivo determinado pela URL final: {}", nomeArquivo);
        // --- Fim da Lógica de Nome de Arquivo ---

        Path arquivoDestino = diretorioDestino.resolve(nomeArquivo);
        long tamanhoTotal = conn.getContentLengthLong();

        System.out.println("  Salvando em: " + arquivoDestino);
        // Exibe 0 MB se o tamanho total for desconhecido (tamanhoTotal = -1)
        System.out.println("  Tamanho: " + (tamanhoTotal > 0 ? (tamanhoTotal / 1024 / 1024) + " MB" : "Desconhecido"));

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
                } else {
                    // Se o tamanho for desconhecido, exibe o total baixado
                    System.out.print(ansi().cursorUp(1).eraseLine()
                            .a("  Baixando: " + (totalLido / 1024 / 1024) + " MB baixados")
                            .newline());
                }
            }
            out.flush(); // Garante que tudo foi escrito

            // Limpa a linha de progresso final
            if (tamanhoTotal <= 0) {
                System.out.print(ansi().cursorUp(1).eraseLine()
                        .a("  Baixando: " + (totalLido / 1024 / 1024) + " MB baixados (Concluído)")
                        .newline());
            }

        } finally {
            conn.disconnect();
        }

        return arquivoDestino;
    }

    /**
     * Abre a conexão HTTP/HTTPS aplicando proxy quando houver configuração válida no ambiente.
     *
     * <p>Este método mantém o comportamento atual quando não há proxy configurado, e só ativa o proxy
     * quando encontra uma configuração válida nas variáveis conhecidas (`http_proxy`, `https_proxy`,
     * e variações em maiúsculas). Também aplica autenticação básica no proxy quando usuário/senha
     * estiverem presentes na URL da variável de ambiente.</p>
     *
     * @param uri     URI de destino do download.
     * @param nomeIde Nome amigável da IDE (apenas para logs).
     * @return Uma conexão pronta para uso.
     * @throws IOException Se ocorrer erro ao abrir a conexão.
     */
    private HttpURLConnection abrirConexaoComSuporteAProxy(URI uri, String nomeIde) throws IOException {
        // Resolve a configuração de proxy considerando esquema (http/https) e regras de no_proxy.
        ConfiguracaoProxy configuracaoProxy = resolverConfiguracaoProxyParaDestino(uri);

        if (configuracaoProxy == null) {
            return (HttpURLConnection) uri.toURL().openConnection();
        }

        // Quando existe proxy configurado, abre a conexão explicitamente passando o Proxy.
        HttpURLConnection conexao = (HttpURLConnection) uri.toURL().openConnection(configuracaoProxy.proxy());

        // Se usuário/senha vierem na variável (ex: http://user:pass@proxy:8080), envia autenticação básica.
        if (configuracaoProxy.usuario() != null && !configuracaoProxy.usuario().isBlank()) {
            String senha = configuracaoProxy.senha() == null ? "" : configuracaoProxy.senha();
            String credenciaisBrutas = configuracaoProxy.usuario() + ":" + senha;
            String credenciaisBase64 = Base64.getEncoder().encodeToString(credenciaisBrutas.getBytes(StandardCharsets.UTF_8));
            conexao.setRequestProperty("Proxy-Authorization", "Basic " + credenciaisBase64);
        }

        log.info("Usando proxy {}:{} (origem: {}) para baixar {}.",
                configuracaoProxy.host(), configuracaoProxy.porta(), configuracaoProxy.nomeVariavel(), nomeIde);
        return conexao;
    }

    /**
     * Resolve qual proxy deve ser usado para uma URI específica.
     *
     * <p>Regras aplicadas:</p>
     * <ul>
     *     <li>Respeita `no_proxy`/`NO_PROXY` antes de qualquer outra decisão.</li>
     *     <li>Para HTTPS, tenta primeiro `https_proxy`/`HTTPS_PROXY` e depois fallback para `http_proxy`/`HTTP_PROXY`.</li>
     *     <li>Para HTTP, usa `http_proxy`/`HTTP_PROXY`.</li>
     *     <li>Se encontrar valor inválido, ignora esse valor e tenta os próximos.</li>
     * </ul>
     *
     * @param uri URI de destino.
     * @return Configuração de proxy válida, ou null quando não deve usar proxy.
     */
    private ConfiguracaoProxy resolverConfiguracaoProxyParaDestino(URI uri) {
        String hostDestino = uri.getHost();
        int portaDestino = uri.getPort() > 0 ? uri.getPort() : resolverPortaPadraoPorEsquema(uri.getScheme());

        // Primeiro verifica se o destino está explicitamente na lista de bypass do no_proxy.
        if (deveIgnorarProxyPorNoProxy(hostDestino, portaDestino)) {
            log.debug("Destino {}:{} está em no_proxy/NO_PROXY. Download será feito sem proxy.", hostDestino, portaDestino);
            return null;
        }

        String esquema = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
        String[] variaveisDeProxy = "http".equals(esquema) ? VARIAVEIS_PROXY_HTTP : VARIAVEIS_PROXY_HTTPS;

        for (String nomeVariavel : variaveisDeProxy) {
            // Tenta cada variável conhecida na ordem de prioridade definida acima.
            String valorVariavel = System.getenv(nomeVariavel);
            if (valorVariavel == null || valorVariavel.isBlank()) {
                continue;
            }

            ConfiguracaoProxy configuracaoProxy = criarConfiguracaoProxyAPartirDaVariavel(nomeVariavel, valorVariavel);
            if (configuracaoProxy != null) {
                return configuracaoProxy;
            }
        }

        return null;
    }

    /**
     * Converte o conteúdo de uma variável de ambiente de proxy em uma configuração utilizável.
     *
     * <p>Suporta formatos com e sem esquema, por exemplo:</p>
     * <ul>
     *     <li>`http://proxy.empresa:8080`</li>
     *     <li>`https://proxy.empresa:8443`</li>
     *     <li>`proxy.empresa:8080`</li>
     *     <li>`http://usuario:senha@proxy.empresa:8080`</li>
     * </ul>
     *
     * @param nomeVariavel Nome da variável de ambiente.
     * @param valorBruto   Valor bruto da variável.
     * @return Configuração de proxy válida, ou null quando o valor for inválido.
     */
    private ConfiguracaoProxy criarConfiguracaoProxyAPartirDaVariavel(String nomeVariavel, String valorBruto) {
        String valorNormalizado = valorBruto.trim();

        // Se vier sem esquema (proxy:8080), prefixamos com http:// para permitir parse por URI.
        if (!valorNormalizado.contains("://")) {
            valorNormalizado = "http://" + valorNormalizado;
        }

        try {
            URI uriProxy = URI.create(valorNormalizado);
            String hostProxy = uriProxy.getHost();

            if (hostProxy == null || hostProxy.isBlank()) {
                log.warn("Ignorando proxy inválido na variável {}: host ausente.", nomeVariavel);
                return null;
            }

            int portaProxy = uriProxy.getPort();
            if (portaProxy <= 0) {
                // Quando a porta não é informada, escolhe um padrão baseado no esquema.
                portaProxy = resolverPortaPadraoPorEsquema(uriProxy.getScheme());
            }

            String usuario = null;
            String senha = null;
            String userInfo = uriProxy.getUserInfo();

            if (userInfo != null && !userInfo.isBlank()) {
                // Separa usuário e senha (quando houver), preservando ':' na senha quando existir.
                String[] partes = userInfo.split(":", 2);
                usuario = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
                senha = partes.length > 1 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "";
            }

            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostProxy, portaProxy));
            return new ConfiguracaoProxy(proxy, nomeVariavel, hostProxy, portaProxy, usuario, senha);

        } catch (IllegalArgumentException e) {
            log.warn("Ignorando proxy inválido na variável {}: '{}'.", nomeVariavel, valorBruto);
            return null;
        }
    }

    /**
     * Verifica se o host/porta de destino deve ignorar o proxy via no_proxy/NO_PROXY.
     *
     * <p>Regras suportadas:</p>
     * <ul>
     *     <li>`*` (ignora proxy para qualquer host)</li>
     *     <li>host exato (`intranet.local`)</li>
     *     <li>sufixo de domínio (`.empresa.com` ou `empresa.com`)</li>
     *     <li>host com porta (`repo.empresa.com:8443`)</li>
     * </ul>
     *
     * @param hostDestino  Host de destino.
     * @param portaDestino Porta de destino.
     * @return true quando o proxy deve ser ignorado.
     */
    private boolean deveIgnorarProxyPorNoProxy(String hostDestino, int portaDestino) {
        if (hostDestino == null || hostDestino.isBlank()) {
            return false;
        }

        String noProxy = System.getenv("no_proxy");
        if (noProxy == null || noProxy.isBlank()) {
            noProxy = System.getenv("NO_PROXY");
        }

        if (noProxy == null || noProxy.isBlank()) {
            return false;
        }

        String hostNormalizado = hostDestino.toLowerCase(Locale.ROOT);

        for (String regraBruta : noProxy.split(",")) {
            String regra = regraBruta.trim().toLowerCase(Locale.ROOT);
            if (regra.isBlank()) {
                continue;
            }

            if ("*".equals(regra)) {
                return true;
            }

            String hostRegra = regra;
            Integer portaRegra = null;

            // Quando a regra traz porta (ex: dominio.local:8443), aplica o bypass apenas nessa porta.
            int indiceDoisPontos = regra.lastIndexOf(':');
            int indiceFechamentoIpv6 = regra.lastIndexOf(']');
            long quantidadeDeDoisPontos = regra.chars().filter(caractere -> caractere == ':').count();
            boolean ipv6SemColchetes = quantidadeDeDoisPontos > 1 && indiceFechamentoIpv6 < 0;
            boolean possuiPortaExplicita = !ipv6SemColchetes && indiceDoisPontos > 0 && indiceDoisPontos > indiceFechamentoIpv6;

            if (possuiPortaExplicita) {
                String possivelPorta = regra.substring(indiceDoisPontos + 1);
                if (possivelPorta.chars().allMatch(Character::isDigit)) {
                    hostRegra = regra.substring(0, indiceDoisPontos);
                    portaRegra = Integer.parseInt(possivelPorta);
                }
            }

            if (portaRegra != null && portaDestino != portaRegra) {
                continue;
            }

            if (hostCombinaComRegraNoProxy(hostNormalizado, hostRegra)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Compara host de destino com uma regra de no_proxy já normalizada.
     *
     * @param hostDestinoNormalizado Host de destino em minúsculo.
     * @param regraHostNormalizada   Regra de host em minúsculo.
     * @return true quando a regra cobre o host informado.
     */
    private boolean hostCombinaComRegraNoProxy(String hostDestinoNormalizado, String regraHostNormalizada) {
        if (regraHostNormalizada == null || regraHostNormalizada.isBlank()) {
            return false;
        }

        String regra = regraHostNormalizada;
        if (regra.startsWith("[") && regra.endsWith("]") && regra.length() > 2) {
            // Remove colchetes de regra IPv6 para comparar apenas o host.
            regra = regra.substring(1, regra.length() - 1);
        }

        if (hostDestinoNormalizado.equals(regra)) {
            return true;
        }

        if (regra.startsWith(".") && regra.length() > 1) {
            String dominio = regra.substring(1);
            return hostDestinoNormalizado.equals(dominio) || hostDestinoNormalizado.endsWith("." + dominio);
        }

        return hostDestinoNormalizado.endsWith("." + regra);
    }

    /**
     * Resolve a porta padrão com base no esquema da URI.
     *
     * @param esquema Esquema (`http`, `https` ou outro).
     * @return Porta padrão para o esquema.
     */
    private int resolverPortaPadraoPorEsquema(String esquema) {
        return "https".equalsIgnoreCase(esquema) ? 443 : 80;
    }
}
