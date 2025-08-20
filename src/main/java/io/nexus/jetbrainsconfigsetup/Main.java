package io.nexus.jetbrainsconfigsetup;

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
            caminhoRaiz = System.getProperty("user.dir");
            log.warn("Nenhum argumento válido fornecido. Usando o diretório de execução atual como caminho raiz: '{}'", caminhoRaiz);
        }

        GerenciadorDePastas gerenciador = new GerenciadorDePastas();
        // O metodo agora reflete melhor o fluxo completo.
        gerenciador.criarEstruturaEConfigurarFerramentas(caminhoRaiz);

        log.info("Aplicação finalizada com sucesso.");
    }
}