package io.nexus.jetbrainsconfigsetup;

import lombok.Data;

import java.nio.file.Path;

@Data
public class IdeInfo {
    private final Path caminhoArquivo;
    private final String nome;
    private final String versao;
}