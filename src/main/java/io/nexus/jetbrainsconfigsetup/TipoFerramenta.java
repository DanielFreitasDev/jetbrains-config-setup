package io.nexus.jetbrainsconfigsetup;

/**
 * Representa as variantes de ferramenta suportadas pela configuração das IDEs JetBrains.
 * Cada variante define qual biblioteca deve ser usada como {@code javaagent}
 * e se a cópia do arquivo {@code .key} para a configuração da IDE ainda é necessária.
 */
public enum TipoFerramenta {

    LEGADA("Legada", "jetbrains-tool.jar", true),
    VERSAO_2026("2026", "enc-sniarbtej-2026.01.00.jar", false);

    private final String nomeExibicao;
    private final String nomeArquivoJavaAgent;
    private final boolean deveCopiarChavePadrao;

    TipoFerramenta(String nomeExibicao, String nomeArquivoJavaAgent, boolean deveCopiarChavePadrao) {
        this.nomeExibicao = nomeExibicao;
        this.nomeArquivoJavaAgent = nomeArquivoJavaAgent;
        this.deveCopiarChavePadrao = deveCopiarChavePadrao;
    }

    /**
     * Retorna o nome amigável exibido para o usuário no fluxo interativo.
     *
     * @return Nome amigável da ferramenta.
     */
    public String getNomeExibicao() {
        return nomeExibicao;
    }

    /**
     * Retorna o nome do arquivo que deve ser referenciado no parâmetro {@code -javaagent}.
     *
     * @return Nome do arquivo da biblioteca utilizada como javaagent.
     */
    public String getNomeArquivoJavaAgent() {
        return nomeArquivoJavaAgent;
    }

    /**
     * Indica se a variante selecionada ainda depende da cópia da chave padrão do produto.
     *
     * @return {@code true} quando a chave deve ser copiada; caso contrário, {@code false}.
     */
    public boolean deveCopiarChavePadrao() {
        return deveCopiarChavePadrao;
    }
}
