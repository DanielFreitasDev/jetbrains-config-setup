package io.nexus.jetbrainsconfigsetup;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.fusesource.jansi.Ansi.ansi;

@Slf4j
public class Main {

    private static final List<String> ARGUMENTOS_AJUDA = Arrays.asList("-h", "--help", "--h", "-help");
    private static final List<String> ARGUMENTOS_VERBOSE = Arrays.asList("-v", "--verbose");

    private static Scanner scanner;

    /**
     * Representa a configuração consolidada da execução da aplicação.
     *
     * @param exibirAjuda           Indica se o usuário pediu apenas a tela de ajuda.
     * @param modoVerbose           Indica se os logs operacionais devem ser exibidos.
     * @param caminhoRaiz           Diretório raiz já resolvido para a execução atual.
     * @param caminhoRaizInformado  Informa se o caminho raiz veio explicitamente por argumento.
     * @param argumentosIgnorados   Lista de argumentos extras/invalidos que foram ignorados.
     */
    private record ConfiguracaoDeExecucao(boolean exibirAjuda,
                                          boolean modoVerbose,
                                          String caminhoRaiz,
                                          boolean caminhoRaizInformado,
                                          List<String> argumentosIgnorados) {
    }

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        scanner = new Scanner(System.in);

        ConfiguracaoDeExecucao configuracao = interpretarArgumentos(args);
        configurarNivelDeLog(configuracao.modoVerbose());

        if (!configuracao.argumentosIgnorados().isEmpty()) {
            log.warn("Argumentos ignorados: {}", String.join(", ", configuracao.argumentosIgnorados()));
        }

        if (configuracao.exibirAjuda()) {
            exibirAjuda();
            AnsiConsole.systemUninstall();
            scanner.close();
            return;
        }

        log.info("Aplicação iniciada.");

        String caminhoRaiz = configuracao.caminhoRaiz();
        if (configuracao.caminhoRaizInformado()) {
            log.info("Caminho raiz fornecido via argumento: '{}'", caminhoRaiz);
        } else {
            log.warn("Nenhum argumento de caminho válido fornecido. Usando o diretório de execução atual como caminho raiz: '{}'", caminhoRaiz);
        }

        // Inicializa os gerenciadores
        GerenciadorDePastas gerenciadorDePastas = new GerenciadorDePastas();
        gerenciadorDePastas.criarEstrutura(caminhoRaiz);

        GerenciadorDeFerramentas gerenciadorDeFerramentas = new GerenciadorDeFerramentas();
        gerenciadorDeFerramentas.configurar(caminhoRaiz);

        GerenciadorDeInstalacao gerenciadorDeInstalacao = new GerenciadorDeInstalacao();
        GerenciadorDeDownloads gerenciadorDeDownloads = new GerenciadorDeDownloads();

        // Loop do menu principal
        boolean executando = true;
        while (executando) {
            exibirMenuPrincipal();
            String escolha = scanner.nextLine().trim();

            switch (escolha) {
                case "1":
                    // Opção 1: Instalar IDEs (versão local)
                    log.info("Opção selecionada: 1 - Instalar IDEs (versão local)");
                    gerenciadorDeInstalacao.instalar(caminhoRaiz);
                    break;
                case "2":
                    // Opção 2: Baixar IDEs (versão mais recente)
                    log.info("Opção selecionada: 2 - Baixar IDEs (versão mais recente)");
                    iniciarProcessoDeDownload(gerenciadorDeDownloads, caminhoRaiz);
                    break;
                case "3":
                    // Opção 3: Baixar e Instalar IDEs (versão mais recente)
                    log.info("Opção selecionada: 3 - Baixar e Instalar IDEs (versão mais recente)");
                    iniciarProcessoDeDownloadEInstalacao(gerenciadorDeDownloads, gerenciadorDeInstalacao, caminhoRaiz);
                    break;
                case "0":
                    // Opção 0: Sair
                    executando = false;
                    break;
                default:
                    System.out.println(ansi().fg(Ansi.Color.RED).a("Opção inválida. Tente novamente.").reset());
                    break;
            }
        }

        log.info("Aplicação finalizada com sucesso.");
        AnsiConsole.systemUninstall();
        scanner.close();
    }

    /**
     * Exibe o menu principal de opções da aplicação.
     */
    private static void exibirMenuPrincipal() {
        System.out.println(ansi().fg(Ansi.Color.CYAN).a("\n--- JetBrains Config Setup ---").reset());
        System.out.println("Selecione uma opção:");
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("  [1] ").reset().a("Instalar IDEs (usar arquivos locais da pasta 'instaladores')"));
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("  [2] ").reset().a("Baixar IDEs (versão mais recente)"));
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("  [3] ").reset().a("Baixar e Instalar IDEs (versão mais recente)"));
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("  [0] ").reset().fg(Ansi.Color.RED).a("Sair").reset());
        System.out.print("\nEscolha: ");
    }

    /**
     * Inicia o fluxo de download de IDEs.
     *
     * @param gerenciador O GerenciadorDeDownloads.
     * @param caminhoRaiz O diretório raiz.
     */
    private static void iniciarProcessoDeDownload(GerenciadorDeDownloads gerenciador, String caminhoRaiz) {
        List<TipoIde> idesParaBaixar = exibirMenuSelecaoDownload();
        if (idesParaBaixar.isEmpty()) {
            return;
        }
        gerenciador.baixarIdes(idesParaBaixar, caminhoRaiz);
        System.out.println(ansi().fg(Ansi.Color.CYAN).a("\nProcesso de download finalizado.").reset());
    }

    /**
     * Inicia o fluxo de download e, em seguida, instalação das IDEs baixadas.
     *
     * @param downloader  O GerenciadorDeDownloads.
     * @param instalador  O GerenciadorDeInstalacao.
     * @param caminhoRaiz O diretório raiz.
     */
    private static void iniciarProcessoDeDownloadEInstalacao(GerenciadorDeDownloads downloader, GerenciadorDeInstalacao instalador, String caminhoRaiz) {
        List<TipoIde> idesParaBaixar = exibirMenuSelecaoDownload();
        if (idesParaBaixar.isEmpty()) {
            return;
        }

        // 1. Baixar
        List<java.nio.file.Path> arquivosBaixados = downloader.baixarIdes(idesParaBaixar, caminhoRaiz);

        if (arquivosBaixados.isEmpty()) {
            System.out.println(ansi().fg(Ansi.Color.RED).a("Nenhum arquivo foi baixado. Abortando instalação.").reset());
            return;
        }

        // 2. Instalar
        System.out.println(ansi().fg(Ansi.Color.CYAN).a("\nIniciando instalação dos arquivos baixados...").reset());
        instalador.instalarArquivosBaixados(arquivosBaixados, caminhoRaiz);
        System.out.println(ansi().fg(Ansi.Color.GREEN).a("\nProcesso de download e instalação finalizado!").reset());
    }

    /**
     * Exibe um menu para o usuário selecionar quais IDEs deseja baixar.
     *
     * @return Uma lista de {@link TipoIde} selecionadas.
     */
    private static List<TipoIde> exibirMenuSelecaoDownload() {
        System.out.println(ansi().fg(Ansi.Color.CYAN).a("\n--- Download de IDEs ---").reset());
        System.out.println("Selecione as IDEs para baixar (versão mais recente):");

        TipoIde[] todasAsIdes = TipoIde.values();
        for (int i = 0; i < todasAsIdes.length; i++) {
            System.out.println(ansi().fg(Ansi.Color.YELLOW).a(String.format("  [%2d] ", i + 1)).reset().a(todasAsIdes[i].getNomeAmigavel()));
        }
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a(String.format("  [%2d] ", todasAsIdes.length + 1)).reset().fg(Ansi.Color.GREEN).a("Baixar Todas").reset());
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a(String.format("  [%2d] ", 0)).reset().fg(Ansi.Color.RED).a("Cancelar").reset());
        System.out.print("\nDigite os números (separados por vírgula) ou uma das opções: ");

        String input = scanner.nextLine().trim();

        if (input.equals(String.valueOf(todasAsIdes.length + 1))) {
            return Arrays.asList(todasAsIdes); // Retorna todas
        }
        if (input.equals("0")) {
            return Collections.emptyList(); // Cancela
        }

        try {
            return Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .filter(i -> i > 0 && i <= todasAsIdes.length)
                    .mapToObj(i -> todasAsIdes[i - 1])
                    .distinct()
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            System.out.println(ansi().fg(Ansi.Color.RED).a("Entrada inválida. Por favor, insira apenas números.").reset());
            return Collections.emptyList();
        }
    }

    private static void exibirAjuda() {
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("passo_a_passo_para_configurar_o_jetbrains_setup.md")) {
            assert is != null;
            try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
                while (scanner.hasNextLine()) {
                    System.out.println(scanner.nextLine());
                }
            }
        } catch (Exception e) {
            log.error("Não foi possível carregar o arquivo de ajuda.", e);
            System.out.println("Erro: Não foi possível encontrar o arquivo de ajuda.");
        }
    }

    /**
     * Interpreta os argumentos de linha de comando, separando flags e caminho raiz.
     *
     * <p>Este método concentra toda a regra de parsing para manter o {@code main} simples e previsível.
     * Ele reconhece ajuda/verbose em qualquer posição, usa apenas o primeiro argumento não-flag como
     * caminho raiz e registra os demais itens como ignorados para diagnóstico em modo verboso.</p>
     *
     * @param args Argumentos recebidos no início da aplicação.
     * @return Uma {@link ConfiguracaoDeExecucao} pronta para ser aplicada.
     */
    private static ConfiguracaoDeExecucao interpretarArgumentos(String[] args) {
        boolean exibirAjuda = false;
        boolean modoVerbose = false;
        String caminhoRaizInformado = null;
        List<String> argumentosIgnorados = new ArrayList<>();

        for (String argumentoBruto : args) {
            if (argumentoBruto == null || argumentoBruto.isBlank()) {
                continue;
            }

            // Normaliza o argumento para garantir comparação consistente de flags.
            String argumentoNormalizado = argumentoBruto.trim();
            String argumentoMinusculo = argumentoNormalizado.toLowerCase(Locale.ROOT);

            // Flags conhecidas podem aparecer em qualquer posição e não devem ser tratadas como caminho.
            if (ARGUMENTOS_AJUDA.contains(argumentoMinusculo)) {
                exibirAjuda = true;
                continue;
            }
            if (ARGUMENTOS_VERBOSE.contains(argumentoMinusculo)) {
                modoVerbose = true;
                continue;
            }

            // Qualquer token iniciado por '-' que não seja uma flag conhecida é ignorado explicitamente.
            if (argumentoNormalizado.startsWith("-")) {
                argumentosIgnorados.add(argumentoNormalizado);
                continue;
            }

            // Apenas o primeiro token não-flag é considerado caminho raiz; os demais são descartados.
            if (caminhoRaizInformado == null) {
                caminhoRaizInformado = argumentoNormalizado;
            } else {
                argumentosIgnorados.add(argumentoNormalizado);
            }
        }

        boolean caminhoFoiInformado = caminhoRaizInformado != null && !caminhoRaizInformado.isBlank();
        String caminhoRaiz = caminhoFoiInformado ? caminhoRaizInformado : System.getProperty("user.dir");

        return new ConfiguracaoDeExecucao(
                exibirAjuda,
                modoVerbose,
                caminhoRaiz,
                caminhoFoiInformado,
                List.copyOf(argumentosIgnorados)
        );
    }

    /**
     * Define o nível global de log com base no modo de execução.
     *
     * <p>Por padrão a aplicação fica silenciosa para mensagens operacionais (`INFO`/`WARN`) e
     * mantém apenas erros (`ERROR`). Quando `--verbose` é informado, o nível sobe para `INFO`
     * para exibir o diagnóstico completo.</p>
     *
     * @param modoVerbose Indica se o usuário solicitou saída detalhada.
     */
    private static void configurarNivelDeLog(boolean modoVerbose) {
        // O ajuste explícito no início da execução evita variações por configuração externa.
        Level nivelDesejado = modoVerbose ? Level.INFO : Level.ERROR;
        Configurator.setRootLevel(nivelDesejado);
    }
}
