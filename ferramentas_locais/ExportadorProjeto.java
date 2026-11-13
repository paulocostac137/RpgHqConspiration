import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ExportadorProjeto {

    // ============================================================
    // CONFIGURAÇÕES
    // ============================================================

    private static final List<String> ARQUIVOS = List.of(

            "app/build.gradle.kts",

            "app/src/main/AndroidManifest.xml",

            "app/src/main/java/com/example/rpghqconspiration/MainActivity.java",

            "app/src/main/java/com/example/rpghqconspiration/ConexaoActivity.java",

            "app/src/main/java/com/example/rpghqconspiration/ServicoConexao.java",

            "app/src/main/res/layout/activity_main.xml",

            "app/src/main/res/layout/activity_conexao.xml",

            "app/src/main/java/com/example/rpghqconspiration/GifSincronizadoView.java"

    );

    private static final String NOME_ARQUIVO_SAIDA =
            "RpgHqConspiration_exportado.txt";

    private static final String SEPARADOR =
            "\n============================================================\n";

    // ============================================================
    // EXECUÇÃO
    // ============================================================

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println(" EXPORTADOR RPG HQ CONSPIRATION");
        System.out.println("========================================");

        try {

            // Raiz do projeto Android
            Path raizProjeto;

            if (args.length > 0) {
                raizProjeto = Paths.get(args[0])
                        .toAbsolutePath()
                        .normalize();
            } else {
                raizProjeto = Paths.get("")
                        .toAbsolutePath()
                        .normalize();
            }

            // Verifica se estamos na raiz correta
            if (!Files.exists(
                    raizProjeto.resolve("app/build.gradle.kts"))) {

                throw new IOException(
                        "Não foi possível localizar o projeto.\n" +
                                "Execute o exportador na raiz de " +
                                "RpgHqConspiration."
                );
            }

            // Arquivo TXT de saída
            Path arquivoSaida = raizProjeto
                    .resolve("ferramentas_locais")
                    .resolve(NOME_ARQUIVO_SAIDA);

            StringBuilder conteudo = new StringBuilder();

            conteudo.append(
                    "EXPORTAÇÃO DO PROJETO RPG HQ CONSPIRATION\n"
            );

            conteudo.append(
                    "Arquivos reunidos em um único documento.\n"
            );

            conteudo.append(
                    "Total de arquivos: " + ARQUIVOS.size() + "\n"
            );

            conteudo.append(SEPARADOR);

            // ====================================================
            // LEITURA DOS ARQUIVOS
            // ====================================================

            for (String caminhoRelativo : ARQUIVOS) {

                Path arquivo = raizProjeto.resolve(caminhoRelativo);

                System.out.println(
                        "Lendo: " + caminhoRelativo
                );

                // Não permite exportação incompleta
                if (!Files.isRegularFile(arquivo)) {

                    throw new IOException(
                            "Arquivo não encontrado: " +
                                    caminhoRelativo
                    );
                }

                // Lê o conteúdo integral em UTF-8
                String texto = Files.readString(
                        arquivo,
                        StandardCharsets.UTF_8
                );

                // Registra o caminho original
                conteudo.append("\n");
                conteudo.append("CAMINHO ORIGINAL:\n");
                conteudo.append(caminhoRelativo);
                conteudo.append("\n");

                conteudo.append(
                        "INÍCIO DO CONTEÚDO DO ARQUIVO\n"
                );

                conteudo.append(SEPARADOR);

                conteudo.append(texto);

                if (!texto.endsWith("\n")) {
                    conteudo.append("\n");
                }

                conteudo.append(SEPARADOR);

                conteudo.append(
                        "FIM DO ARQUIVO: "
                );

                conteudo.append(caminhoRelativo);
                conteudo.append("\n");

                conteudo.append(SEPARADOR);
            }

            // ====================================================
            // GRAVAÇÃO DO TXT
            // ====================================================

            Files.createDirectories(
                    arquivoSaida.getParent()
            );

            Files.writeString(
                    arquivoSaida,
                    conteudo.toString(),
                    StandardCharsets.UTF_8
            );

            System.out.println();
            System.out.println("EXPORTAÇÃO CONCLUÍDA!");
            System.out.println();

            System.out.println(
                    "Arquivos exportados: " + ARQUIVOS.size()
            );

            System.out.println(
                    "Arquivo gerado em:"
            );

            System.out.println(arquivoSaida);

        } catch (Exception erro) {

            System.err.println();
            System.err.println("ERRO NA EXPORTAÇÃO!");
            System.err.println(erro.getMessage());

            erro.printStackTrace();

        }
    }
}