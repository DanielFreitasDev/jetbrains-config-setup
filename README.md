# JetBrains Config Setup

## 📖 Visão Geral

O **JetBrains Config Setup** é uma ferramenta de linha de comando em Java, projetada para automatizar a instalação e configuração de IDEs da JetBrains em ambientes baseados em Linux. A aplicação gerencia a estrutura de pastas, a instalação de ferramentas, a extração de instaladores e a configuração personalizada das IDEs, tornando o processo de setup de um ambiente de desenvolvimento rápido e padronizado.

## ✨ Funcionalidades

  - **Criação de Estrutura de Diretórios**: Cria uma estrutura de pastas organizada para `binarios`, `configuracoes`, `instaladores`, `atalhos` e `ferramentas`.
  - **Instalação Automatizada de IDEs**: Descompacta arquivos `.tar.gz` de IDEs localizados na pasta `instaladores`.
  - **Menu de Instalação Interativo**: Permite ao usuário selecionar quais IDEs deseja instalar, incluindo uma opção para instalar todas de uma vez.
  - **Configuração Portátil**: Altera os arquivos de propriedades (`idea.properties`) das IDEs para que as configurações, plugins e logs sejam salvos dentro da estrutura de pastas do projeto, e não no diretório do usuário.
  - **Configuração de VMOptions**: Adiciona automaticamente o `javaagent` para as ferramentas de ativação ao arquivo `.vmoptions` de cada IDE.
  - **Geração de Atalhos**: Cria arquivos `.desktop` (atalhos) para as IDEs instaladas, que podem ser salvos localmente ou no diretório de aplicações do sistema.
  - **Ativação Automatizada**: Copia os arquivos de chave (`.key`) necessários para o diretório de configuração de cada IDE.
  - **Interface de Console Colorida**: Utiliza a biblioteca Jansi para uma melhor experiência do usuário no terminal.

## 🚀 Como Funciona

A aplicação segue um fluxo de execução orquestrado pela classe `Main`:

1.  **Inicialização**: O `AnsiConsole` é instalado para suportar cores no terminal. O caminho raiz da aplicação é determinado (pode ser passado como argumento ou será o diretório atual).
2.  **Criação da Estrutura de Pastas (`GerenciadorDePastas`)**:
      * Verifica e cria, se não existirem, os diretórios `atalhos`, `binarios`, `configuracoes`, `ferramentas`, e `instaladores` no caminho raiz.
3.  **Configuração das Ferramentas (`GerenciadorDeFerramentas`)**:
      * Decodifica uma string Base64 interna para criar o arquivo `jetbrains-tool.jar` na pasta `ferramentas/ferramenta-atual`.
      * Decodifica outra string Base64 para descompactar as chaves de ativação (`.key`) na pasta `ferramentas/ferramenta-atual/chaves`.
4.  **Instalação das IDEs (`GerenciadorDeInstalacao`)**:
      * Procura por arquivos `.tar.gz` na pasta `instaladores`.
      * Exibe um menu interativo para o usuário selecionar quais IDEs instalar.
      * Para cada IDE selecionada:
          * Extrai o nome e a versão do arquivo.
          * Descompacta o conteúdo da IDE no diretório `binarios/<nome-da-ide>/<versao>`.
          * Chama o `GerenciadorDeConfiguracao`.
5.  **Configuração Específica da IDE (`GerenciadorDeConfiguracao`)**:
      * **`idea.properties`**: Modifica este arquivo para apontar os caminhos de configuração e de sistema para o diretório `configuracoes/<nome-da-ide>/<versao>`, tornando a instalação portátil.
      * **`*.vmoptions`**: Adiciona as flags `-javaagent` com o caminho para o `jetbrains-tool.jar`.
      * **Cópia da Chave**: Copia o arquivo `.key` correspondente da pasta `ferramentas` para a pasta de configuração da IDE.
      * **Geração de Atalho**: Se o usuário optou, cria um arquivo `.desktop` com os caminhos corretos para o executável e o ícone da IDE.

## 🛠️ Como Usar

### Pré-requisitos

  * Java 21 ou superior.
  * Maven (para compilar o projeto).

### Passos

1.  **Compile o Projeto**:
    Clone o repositório e compile o projeto usando Maven. Isso irá gerar um arquivo `jetbrains-config-setup-1.0.jar` (ou similar) na pasta `target`.

    ```bash
    mvn clean package
    ```

2.  **Prepare a Estrutura**:

      * Crie um diretório base para sua configuração.
      * Dentro dele, crie uma pasta chamada `instaladores`.
      * Coloque os arquivos `.tar.gz` das IDEs da JetBrains que você deseja instalar dentro da pasta `instaladores`.

3.  **Execute a Aplicação**:
    Execute o arquivo `.jar` a partir do seu diretório base. Você pode especificar o caminho raiz como um argumento ou executar o JAR diretamente no diretório.

    *Opção 1: Executando no diretório base*

    ```bash
    java -jar /caminho/para/jetbrains-config-setup-1.0.jar
    ```

    *Opção 2: Especificando o caminho base*

    ```bash
    java -jar /caminho/para/jetbrains-config-setup-1.0.jar /meu/diretorio/base
    ```

    *Opção 3: Executando com logs detalhados (modo verbose)*

    ```bash
    java -jar /caminho/para/jetbrains-config-setup-1.0.jar --verbose
    ```

4.  **Siga o Menu Interativo**:

      * O programa listará todos os instaladores encontrados.
      * Digite os números correspondentes às IDEs que deseja instalar, separados por vírgula (ex: `1,3`).
      * Você também pode digitar o número correspondente a "Instalar Todos" ou `0` para sair.
      * Responda se deseja criar atalhos e onde deseja salvá-los.

A aplicação cuidará do resto\!

## 🏗️ Estrutura do Projeto

```
jetbrains-config-setup/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/io/nexus/jetbrainsconfigsetup/
    │   │   ├── Main.java                   # Ponto de entrada da aplicação
    │   │   ├── GerenciadorDePastas.java      # Cria a estrutura de diretórios
    │   │   ├── GerenciadorDeFerramentas.java # Gerencia o 'jetbrains-tool.jar' e as chaves
    │   │   ├── GerenciadorDeInstalacao.java  # Lida com a instalação e o menu
    │   │   ├── GerenciadorDeConfiguracao.java# Aplica as configurações personalizadas
    │   │   ├── IdeInfo.java                  # DTO para informações da IDE
    │   │   ├── ProductInfo.java              # DTO para o 'product-info.json'
    │   │   └── AtalhoInfo.java               # DTO para informações de atalhos
    │   └── resources/
    │       └── log4j2.xml                    # Configuração de logs
    └── test/
```

## 📦 Dependências

  * **Lombok**: Para reduzir código boilerplate (e.g., getters, setters, loggers).
  * **SLF4J & Log4j2**: Para um sistema de logging robusto.
  * **Apache Commons Compress**: Para descompactar arquivos `.tar.gz`.
  * **Gson**: Para fazer o parse do arquivo `product-info.json` e extrair a versão da IDE.
  * **Jansi**: Para adicionar cores e formatar a saída no console.
  * **Maven Shade Plugin**: Para empacotar o projeto e suas dependências em um único JAR executável.

## 📄 Licença

Este projeto não possui uma licença definida. Verifique os arquivos para mais detalhes.
