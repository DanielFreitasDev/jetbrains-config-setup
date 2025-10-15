### 🧩 Passo a passo para configurar o JetBrains Setup

1. **Crie uma pasta** em qualquer lugar do seu computador.
   Você pode dar o nome que quiser, mas recomendamos usar **`jetbrains`** para facilitar a organização.

2. **Copie o arquivo** `jetbrains-config-setup-1.0.jar` para dentro dessa pasta que você acabou de criar.

3. **Abra o terminal** (ou prompt de comando) e digite o seguinte comando:

   ```bash
   java -jar jetbrains-config-setup-1.0.jar
   ```

   Esse comando vai **criar automaticamente uma estrutura de pastas** para organizar os arquivos.
   Não é preciso alterar nada depois disso.

   Depois de rodar o comando, sua pasta deve ficar assim:

   ```
   jetbrains
   ├── atalhos
   ├── binarios
   ├── configuracoes
   ├── ferramentas
   ├── instaladores
   └── jetbrains-config-setup-1.0.jar
   ```

4. Agora que a estrutura foi criada, **baixe o IntelliJ IDEA** ou **qualquer outra IDE da JetBrains** que você quiser usar.
   O arquivo baixado será um **`.tar.gz`**.

5. **Coloque o arquivo `.tar.gz`** dentro da pasta chamada **`instaladores`**.

6. Depois disso, **rode novamente o mesmo comando** no terminal:

   ```bash
   java -jar jetbrains-config-setup-1.0.jar
   ```

   O programa vai detectar os instaladores e perguntar **quais IDEs você quer instalar**.
   Você pode escolher uma, várias ou todas de uma vez.

---

💡 **Dica:**
Você só precisa criar a estrutura de pastas **uma vez**. Depois disso, sempre que quiser instalar outra IDE da JetBrains, basta colocar o novo arquivo `.tar.gz` na pasta `instaladores` e executar novamente o comando.

---

⚠️ **Observação importante sobre atualizações:**
Se você **atualizar uma IDE**, como o **IntelliJ IDEA**, ela vai **baixar a nova versão** e pedir para **reiniciar a IDE**.
Após fechar, aparecerá uma janela perguntando se você deseja **sobrescrever (replace)** ou **manter (keep)** os arquivos existentes.

➡️ **Tenha bastante cuidado nessa etapa!**
Escolha a opção **“Keep”** (manter) na coluna **“Solution”**.
Por padrão, a opção virá como **“Replace”**, então altere para **“Keep”** nos dois arquivos e, em seguida, clique no botão **“Proceed”**.

Isso garante que suas configurações e estrutura de instalação continuem funcionando corretamente.
