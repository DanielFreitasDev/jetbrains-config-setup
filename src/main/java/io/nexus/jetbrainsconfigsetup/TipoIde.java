package io.nexus.jetbrainsconfigsetup;

import lombok.Getter;

/**
 * Enumeração que define as IDEs da JetBrains suportadas pela aplicação,
 * incluindo o nome amigável e o código do produto usado para download.
 */
@Getter
public enum TipoIde {
    INTELLIJ_IDEA("IntelliJ IDEA Ultimate", "IU"),
    RUBYMINE("RubyMine", "RM"),
    PYCHARM("PyCharm Professional", "PY"),
    DATAGRIP("DataGrip", "DG"),
    WEBSTORM("WebStorm", "WS");

    private final String nomeAmigavel;
    private final String codigoProduto;

    /**
     * Construtor para o enum TipoIde.
     *
     * @param nomeAmigavel  O nome de exibição da IDE.
     * @param codigoProduto O código oficial do produto (usado na URL de download).
     */
    TipoIde(String nomeAmigavel, String codigoProduto) {
        this.nomeAmigavel = nomeAmigavel;
        this.codigoProduto = codigoProduto;
    }
}