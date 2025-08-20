package io.nexus;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Classe responsável por criar uma estrutura de diretórios pré-definida.
 * Utiliza Log4j2 para logging robusto.
 */
@Log4j2
public class GerenciadorDePastas {

    private static final List<String> NOMES_PASTAS = List.of(
            "atalhos",
            "binarios",
            "configuracoes",
            "ferramentas",
            "instaladores"
    );

    /**
     * Cria a estrutura de pastas padrão dentro do caminho raiz fornecido.
     *
     * @param caminhoRaiz O caminho base onde as novas pastas serão criadas.
     */
    public void criarEstrutura(String caminhoRaiz) {
        log.info("Iniciando verificação e criação da estrutura de pastas em '{}'", caminhoRaiz);

        Path raiz = Paths.get(caminhoRaiz);

        for (String nomePasta : NOMES_PASTAS) {
            try {
                Path caminhoCompleto = raiz.resolve(nomePasta);
                // createDirectories é idempotente: só cria se não existir.
                Files.createDirectories(caminhoCompleto);
                log.debug("Diretório garantido: {}", caminhoCompleto.toAbsolutePath());
            } catch (IOException e) {
                // Log de erro profissional: inclui a mensagem e a stack trace da exceção.
                log.error("Falha ao criar o diretório '{}'", nomePasta, e);
            }
        }
        log.info("Processo de criação da estrutura de pastas finalizado.");
    }
}