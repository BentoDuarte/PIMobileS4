# PIMobileS4 — base Android de ordens de serviço

O [serviço de importação por e-mail](api-email/README.md) lê solicitações via IMAP e cria ordens no mesmo Firestore do aplicativo.

Aplicativo nativo Android em **Java + layouts XML**, construído sobre o projeto enviado. Não usa páginas HTML ou JSF.

## O que está incluído

| Fluxo pedido | Implementação |
|---|---|
| Login | MainActivity + activity_main.xml; Firebase Authentication por e-mail e senha |
| Lista de ordens | ListaOrdensActivity + activity_lista.xml; busca por número, título e cliente, filtro de status e estado vazio |
| Detalhes | DetalhesOrdemActivity + activity_detalhes.xml |
| Criar ordem | FormularioOrdemActivity + activity_formulario.xml |
| Editar ordem | Mesmo formulário, carregando a ordem pelo ID |
| Alterar status | Diálogo com seleção e confirmação nos detalhes |
| Gerar PDF | Ação nos detalhes; exportação nativa paginada via OrdemPdf |
| Confirmar exclusão | Diálogo com Cancelar e Excluir |

## Abrir e experimentar

1. Extraia o ZIP e abra a pasta `PIMobileS4-main` no Android Studio.
2. Use o JDK recomendado pelo Android Studio para o Gradle/AGP já configurado no projeto. O Java 11 em `compileOptions` é o nível do código, não a versão obrigatória do JDK que executa o Gradle.
3. Sincronize o Gradle. A sincronização inicial precisa acessar os servidores de distribuição e dependências; o projeto mantém as versões recebidas no ZIP original.
4. Rode o módulo `app` em aparelho ou emulador Android 7.0/API 24 ou superior.
5. Entre com uma conta criada no Firebase Authentication. Crie uma ordem para preencher a lista.
6. Para PDF, abra a ordem e escolha Gerar PDF; o Android permite escolher nome e destino. Abra/compartilhe o arquivo pelo gerenciador de arquivos após salvar.

Não é necessário instalar o aplicativo desktop do ChatGPT para abrir este projeto. Android Studio e o SDK Android continuam necessários para compilar o aplicativo.

## Classes model

Todas ficam em `app/src/main/java/com/example/pimobiles4/model` e têm construtor vazio, atributos privados, getters e setters.

| Classe | Papel e principais campos |
|---|---|
| Usuario | id, nome, email, ativo; não transporta senha |
| Cliente | id, nome, telefone, email |
| OrdemServico | id, título, cliente, descrição, responsável, status, prioridade, criação e atualização |
| Produto | id, nome, código, unidade de medida, valor unitário BigDecimal |
| Estoque | produtoId e quantidadeDisponivel |
| ItemOrdemServico | id, ordemServicoId, produtoId, descrição, quantidade, valor unitário BigDecimal |
| MovimentacaoEstoque | id, produtoId, ordemServicoId opcional, tipo, quantidade, data |
| StatusOrdemServico | ABERTA, EM_ANDAMENTO, CONCLUIDA, CANCELADA |
| Prioridade | BAIXA, MEDIA, ALTA |
| TipoMovimentacao | ENTRADA, SAIDA |

Os models são objetos do aplicativo/DTOs, **sem anotações JPA**. Entidades do backend serão classes separadas. IDs locais são demonstrativos; IDs definitivos virão do servidor.

## Organização

- `model/`: dados e enums.
- `data/OrdemRepository.java`: contrato de acesso às ordens.
- `data/FirebaseOrdemRepository.java`: operações assíncronas no Cloud Firestore.
- `ui/`: Activities das telas, navegação e validação de formulário.
- `util/OrdemPdf.java`: PDF A4 com quebra de linhas e paginação.
- `res/layout/`: interfaces XML editáveis no Android Studio.
- `res/values/`: cores, nome e tema claro compartilhado.

## Decisões provisórias

O PDF é geral: cita Android, Java, MySQL, autenticação, CRUD, estoque e relatórios, mas não especifica campos de OS, permissões ou transições de status. A lista de telas enviada orientou o fluxo de ordens.

- Campos obrigatórios: título, nome do cliente e descrição. E-mail opcional é validado quando informado.
- Telefone, e-mail do cliente e responsável são opcionais.
- Toda ordem nova começa ABERTA, com prioridade MEDIA.
- Qualquer status pode ser escolhido nesta base. Reabertura, cancelamento e bloqueio de edição precisam de regras do grupo.
- Datas são armazenadas como epoch em milissegundos e exibidas no fuso do aparelho.
- Criar e editar compartilham a mesma tela para manter a consistência.
- Alteração de status, exclusão e geração de PDF partem dos detalhes, sem exigir Activities separadas.
- Os dados começam vazios e persistem entre execuções. Sair encerra a sessão do Firebase e preserva ordens.
- Os models de estoque são uma preparação; **não há telas, movimentação ou baixa automática de estoque** nesta entrega. Produtos e itens ainda não são ligados ao CRUD de ordens.

## Configuração do Firebase

1. Registre o app Android com o pacote `com.example.pimobiles4` e coloque o arquivo `google-services.json` em `app/`.
2. No Firebase Authentication, habilite **E-mail/senha** e crie uma conta em **Usuários**.
3. Crie o banco padrão do Cloud Firestore em modo de produção. Na aba **Regras**, publique as regras de `firestore.rules` para este app.
4. Compile o projeto e entre com a conta criada.

Cada conta tem suas próprias ordens em `usuarios/{uid}/ordens`. O número da OS é gerado por conta. Os registros locais do protótipo continuam no armazenamento original e não são importados automaticamente. O PDF é gerado no aparelho a partir dos dados da ordem.

## Validação automática de commits e PRs

Para executar os testes unitários antes de cada commit, rode uma vez por clone:

```sh
git config --local core.hooksPath .githooks
```

O gancho usa o Gradle e bloqueia o commit se os testes falharem. Ele testa os arquivos como estão no disco, incluindo mudanças ainda fora do commit.

Nos PRs para `main`, o GitHub Actions executa `lintDebug`, `testDebugUnitTest` e `assembleDebug` sobre o código enviado. Para exigir essa validação antes do merge, configure o check **Validar Android** como obrigatório na proteção da `main`.

## Versão automática na main

O GitHub Actions valida os PRs e roda novamente a cada atualização da `main`, inclusive após merges. Depois de passar na validação e gerar o APK, o workflow publica uma tag Git no commit validado com o mesmo nome da versão do APK. O APK de depuração aparece em **Actions → Validacao Android → Artifacts**.

A execução de número `N` usa a tag `1.1.N`, `versionName = 1.1.N` e `versionCode = N + 1`. Os números podem pular porque os PRs também usam o contador do workflow. Reexecutar o mesmo build mantém sua versão.

Para que o APK do GitHub Actions acesse o Firebase, adicione o conteúdo completo do arquivo `app/google-services.json` ao secret `GOOGLE_SERVICES_JSON` em **Settings → Secrets and variables → Actions**. O workflow usa o secret apenas nas execuções da `main`.

## Menu inferior

- **Início** resume a quantidade total de ordens e a contagem por status.
- **Ordens** permite buscar, filtrar, expandir, criar, editar e excluir ordens.
- **Registros** mostra criação, edição, alteração de status e exclusão, com data e tipo de ação. A lista mostra o status atual das ordens existentes e preserva o status registrado quando uma ordem é excluída.
- **Relatórios** gera um PDF com o total e a distribuição por status ou somente a contagem do status escolhido.
- **Perfil** mostra a conta conectada e permite sair.

Publique o conteúdo atualizado de `firestore.rules` antes de usar esta versão. As alterações das ordens e seus registros são salvos juntos no Firestore e precisam de conexão. O histórico começa com as ações feitas a partir desta versão.

## Validação e roteiro de conferência

Ao abrir no Android Studio, confira:

1. Login inválido mostra erro; uma conta válida abre a lista de ordens.
2. Salvar sem os campos obrigatórios mostra erro; e-mail informado inválido também.
3. Criar uma OS, buscar por cliente e filtrar por status.
4. Editar sem alterar ID/data de criação; conferir atualização nos detalhes.
5. Cancelar a troca de status preserva o valor; confirmar atualiza lista e detalhes.
6. Cancelar exclusão preserva a OS; confirmar remove a ordem.
7. Fechar e reabrir o aplicativo preserva os registros; sair volta ao login.
8. Gerar PDF com descrição longa: conferir quebras de linha, páginas, acentos e destino.
9. Girar aparelho, abrir teclado e testar campos/botões em tela pequena.

## Rodando com Docker (Backend / API da Equipe)

Para garantir que a API do projeto rode de forma idêntica em qualquer ambiente de desenvolvimento ou produção, utilizamos o Docker com um servidor de produção (`gunicorn`)[cite: 17, 22].

### 1. Construir a imagem Docker
Na raiz do projeto (onde o `Dockerfile` está localizado), execute o comando de *build*[cite: 18]:

docker build -t opstrack-api:0.1.0 .

2. Executar o containerSuba o container em segundo plano, mapeando a porta 8000 do seu ambiente para a porta 8000 do container:   Bashdocker run -d --name opstrack -p 8000:8000 opstrack-api:0.1.0
3. Verificar se a API está respondendoTeste o endpoint de status da aplicação:   Bashcurl localhost:8000/status

(A resposta esperada deve ser {"status":"ok"})   4. Como ver os logs e parar o containerVisualizar logs em tempo real:Bashdocker logs -f opstrack
Parar e remover o container:Bashdocker rm -f opstrack
