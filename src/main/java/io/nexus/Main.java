package io.nexus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(String[] args) {
        log.info("Aplicação iniciada.");

        String caminhoRaiz;

        // Verifica se um argumento de linha de comando foi fornecido.
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            // Usa o primeiro argumento como o caminho raiz.
            caminhoRaiz = args[0];
            log.info("Caminho raiz fornecido via argumento: '{}'", caminhoRaiz);
        } else {
            // Se nenhum argumento for fornecido, usa o diretório de execução atual.
            // System.getProperty("user.dir") retorna o diretório onde o comando java foi executado.
            caminhoRaiz = System.getProperty("user.dir");
            log.warn("Nenhum argumento válido fornecido. Usando o diretório de execução atual como caminho raiz: '{}'", caminhoRaiz);
        }

        GerenciadorDePastas gerenciador = new GerenciadorDePastas();
        gerenciador.criarEstrutura(caminhoRaiz);

        log.info("Aplicação finalizada com sucesso.");
    }
}