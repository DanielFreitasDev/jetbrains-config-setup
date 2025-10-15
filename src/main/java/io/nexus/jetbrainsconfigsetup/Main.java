package io.nexus.jetbrainsconfigsetup;

import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.AnsiConsole;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@Slf4j
public class Main {
    public static void main(String[] args) {
        AnsiConsole.systemInstall();

        List<String> helpArgs = Arrays.asList("-h", "--help", "--h", "-help");
        if (args.length > 0 && helpArgs.contains(args[0].toLowerCase())) {
            exibirAjuda();
            AnsiConsole.systemUninstall();
            return;
        }

        log.info("Aplicação iniciada.");

        String caminhoRaiz = getRootPath(args);

        GerenciadorDePastas gerenciadorDePastas = new GerenciadorDePastas();
        gerenciadorDePastas.criarEstrutura(caminhoRaiz);

        GerenciadorDeFerramentas gerenciadorDeFerramentas = new GerenciadorDeFerramentas();
        gerenciadorDeFerramentas.configurar(caminhoRaiz);

        GerenciadorDeInstalacao gerenciadorDeInstalacao = new GerenciadorDeInstalacao();
        gerenciadorDeInstalacao.instalar(caminhoRaiz);

        log.info("Aplicação finalizada com sucesso.");
        AnsiConsole.systemUninstall();
    }

    private static void exibirAjuda() {
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("passo_a_passo_para_configurar_o_jetbrains_setup.md");
             Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
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