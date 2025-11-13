package io.nexus.jetbrainsconfigsetup;

import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.fusesource.jansi.Ansi.ansi;

@Slf4j
public class Main {

    private static Scanner scanner;

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        scanner = new Scanner(System.in);

        List<String> helpArgs = Arrays.asList("-h", "--help", "--h", "-help");
        if (args.length > 0 && helpArgs.contains(args[0].toLowerCase())) {
            exibirAjuda();
            AnsiConsole.systemUninstall();
            scanner.close();
            return;
        }

        log.info("Aplicação iniciada.");

        String caminhoRaiz = getRootPath(args);

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

    private static String getRootPath(String[] args) {
        String caminhoRaiz;

        // Verifica se um argumento de linha de comando foi fornecido.
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            // Usa o primeiro argumento como o caminho raiz.
            caminhoRaiz = args[0];
            log.info("Caminho raiz fornecido via argumento: '{}'", caminhoRaiz);
        } else {
            // Se nenhum argumento for fornecido, usa o diretório de execução atual.
            caminhoRaiz = System.getProperty("user.dir");
            log.warn("Nenhum argumento válido fornecido. Usando o diretório de execução atual como caminho raiz: '{}'", caminhoRaiz);
        }
        return caminhoRaiz;
    }
}