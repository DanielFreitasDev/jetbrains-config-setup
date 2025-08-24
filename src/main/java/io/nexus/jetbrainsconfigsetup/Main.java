package io.nexus.jetbrainsconfigsetup;

import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.AnsiConsole;

@Slf4j
public class Main {
    public static void main(String[] args) {
        AnsiConsole.systemInstall();
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