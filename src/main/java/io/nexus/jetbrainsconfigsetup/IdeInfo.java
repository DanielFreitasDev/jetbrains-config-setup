package io.nexus.jetbrainsconfigsetup;

import lombok.Data;

import java.nio.file.Path;

@Data
public class IdeInfo {
    private final Path caminhoArquivo;
    private final String nome;
    private final String versao;
    private final Path caminhoBinario;

    public IdeInfo(Path caminhoArquivo, String nome, String versao) {
        this.caminhoArquivo = caminhoArquivo;
        this.nome = nome;
        this.versao = versao;
        // O caminho raiz é extraído do pai do pai do arquivo de instalação (ex: .../instaladores/ -> ...)
        Path caminhoRaiz = caminhoArquivo.getParent().getParent();
        this.caminhoBinario = caminhoRaiz.resolve("binarios").resolve(nome).resolve(versao);
    }
}