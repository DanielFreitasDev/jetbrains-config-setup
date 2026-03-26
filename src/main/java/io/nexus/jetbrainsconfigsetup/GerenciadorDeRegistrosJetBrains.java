package io.nexus.jetbrainsconfigsetup;

import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Classe responsável por localizar e remover registros locais da JetBrains no Linux.
 */
@Slf4j
public class GerenciadorDeRegistrosJetBrains {

    private static final String NOME_DIRETORIO_JETBRAINS = "jetbrains";
    private static final List<String> DIRETORIOS_BASE_RELATIVOS = List.of(".cache", ".local/share");
    private final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");

    /**
     * Remove, no Linux, os diretórios JetBrains localizados diretamente em {@code ~/.cache} e {@code ~/.local/share}.
     * A comparação do nome do diretório é feita sem diferenciar maiúsculas de minúsculas para cobrir variações como
     * {@code jetbrains}, {@code Jetbrains} e {@code JetBrains}.
     */
    public void limparRegistros() {
        if (isWindows) {
            log.info("A limpeza de registros JetBrains ainda não está disponível no Windows.");
            System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Esta opção ainda não está disponível no Windows.").reset());
            return;
        }

        Path diretorioHome = Paths.get(System.getProperty("user.home"));
        List<Path> diretoriosRemovidos = new ArrayList<>();

        for (String diretorioBaseRelativo : DIRETORIOS_BASE_RELATIVOS) {
            // Cada diretório base é resolvido a partir do HOME para aceitar tanto a ideia de "~" quanto "$HOME".
            Path diretorioBase = diretorioHome.resolve(diretorioBaseRelativo).normalize();
            removerDiretoriosJetBrainsNoDiretorioBase(diretorioBase, diretoriosRemovidos);
        }

        if (diretoriosRemovidos.isEmpty()) {
            System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Nenhum diretório JetBrains foi encontrado em ~/.cache ou ~/.local/share.").reset());
            return;
        }

        System.out.println(ansi().fg(Ansi.Color.GREEN).a("Registros JetBrains removidos com sucesso.").reset());
        for (Path diretorioRemovido : diretoriosRemovidos) {
            System.out.println(" - " + diretorioRemovido);
        }
    }

    /**
     * Procura diretórios JetBrains diretamente dentro do diretório base informado e remove cada correspondência encontrada.
     * A busca é restrita ao primeiro nível porque os registros conhecidos da JetBrains nessas áreas ficam como filhos diretos
     * dos diretórios base monitorados.
     *
     * @param diretorioBase O diretório base onde a busca deve acontecer.
     * @param diretoriosRemovidos Coleção usada para acumular os diretórios efetivamente apagados.
     */
    private void removerDiretoriosJetBrainsNoDiretorioBase(Path diretorioBase, List<Path> diretoriosRemovidos) {
        if (Files.notExists(diretorioBase) || !Files.isDirectory(diretorioBase)) {
            log.debug("Diretório base não encontrado ou inválido para limpeza: {}", diretorioBase);
            return;
        }

        try (Stream<Path> filhos = Files.list(diretorioBase)) {
            // A filtragem considera apenas diretórios filhos imediatos para evitar apagar caminhos não relacionados.
            List<Path> diretoriosEncontrados = filhos
                    .filter(Files::isDirectory)
                    .filter(this::isDiretorioJetBrains)
                    .toList();

            for (Path diretorioEncontrado : diretoriosEncontrados) {
                try {
                    // A exclusão recursiva garante que todo o conteúdo local seja removido junto com o diretório principal.
                    apagarDiretorioRecursivamente(diretorioEncontrado);
                    diretoriosRemovidos.add(diretorioEncontrado);
                    log.info("Diretório JetBrains removido: {}", diretorioEncontrado);
                } catch (IOException e) {
                    log.error("Falha ao remover o diretório JetBrains {}", diretorioEncontrado, e);
                    System.out.println(ansi().fg(Ansi.Color.RED).a("Falha ao remover: " + diretorioEncontrado).reset());
                }
            }
        } catch (IOException e) {
            log.error("Falha ao listar o diretório base {}", diretorioBase, e);
            System.out.println(ansi().fg(Ansi.Color.RED).a("Falha ao acessar o diretório: " + diretorioBase).reset());
        }
    }

    /**
     * Verifica se o caminho informado representa um diretório chamado JetBrains, ignorando diferenças de caixa.
     *
     * @param diretorio Caminho candidato analisado.
     * @return {@code true} quando o nome do diretório equivale a JetBrains; caso contrário, {@code false}.
     */
    private boolean isDiretorioJetBrains(Path diretorio) {
        // O nome do diretório é comparado sem diferenciar caixa para aceitar todas as variações citadas.
        Path nomeArquivo = diretorio.getFileName();
        return nomeArquivo != null && NOME_DIRETORIO_JETBRAINS.equalsIgnoreCase(nomeArquivo.toString());
    }

    /**
     * Remove recursivamente um diretório e todo o seu conteúdo.
     * A ordenação reversa é necessária para apagar primeiro os filhos e só então o diretório pai,
     * evitando falhas por tentativa de remoção de pastas ainda preenchidas.
     *
     * @param diretorio Caminho do diretório que deve ser excluído.
     * @throws IOException Se ocorrer erro ao percorrer ou apagar algum item.
     */
    private void apagarDiretorioRecursivamente(Path diretorio) throws IOException {
        // A caminhada reversa garante a ordem correta de exclusão: arquivos e subpastas antes da pasta principal.
        try (Stream<Path> caminhos = Files.walk(diretorio)) {
            for (Path caminho : caminhos.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(caminho);
            }
        }
    }
}
