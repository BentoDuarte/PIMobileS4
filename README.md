# PIMobileS4 — base Android de ordens de serviço

Aplicativo nativo Android em **Java + layouts XML**, construído sobre o projeto enviado. Não usa páginas HTML ou JSF.

## O que está incluído

| Fluxo pedido | Implementação |
|---|---|
| Login | MainActivity + activity_main.xml; acesso demonstrativo |
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
5. Entre com **demo@os.com** e **demo123**. Crie uma ordem para preencher a lista.
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
- `data/LocalOrdemRepository.java`: implementação com JSON em SharedPreferences privadas do aplicativo.
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
- Os dados começam vazios e persistem entre execuções. Sair encerra a sessão demonstrativa e preserva ordens.
- Os models de estoque são uma preparação; **não há telas, movimentação ou baixa automática de estoque** nesta entrega. Produtos e itens ainda não são ligados ao CRUD de ordens.

## Limites desta primeira versão

- Login é demonstrativo, com credenciais públicas fixas; não autentica no servidor nem estabelece permissões reais.
- Não há API Java, conexão MySQL, sincronização, cadastro de usuários ou operação multiusuário.
- Ordens são guardadas apenas no aparelho. Limpar dados/desinstalar pode apagar o conteúdo; esta persistência é adequada ao protótipo, não a uma base operacional.
- SharedPreferences não é armazenamento criptografado; use dados fictícios nesta etapa.
- As telas usam dados de cliente embutidos na OS; um cadastro compartilhado de clientes fica para integração.
- Exporta PDF para salvar; compartilhamento direto dentro do aplicativo ainda não foi implementado.
- Tema claro inicial; identidade visual final deve ser alinhada com UX/UI.

## Integração Java + MySQL

O caminho previsto é **Android → API REST Java → MySQL**. Não coloque credenciais de MySQL no aplicativo nem conecte o Android diretamente ao banco.

Uma implementação remota de OrdemRepository poderá substituir LocalOrdemRepository. As requisições devem ser assíncronas, com estados de carregamento e tratamento de falhas. A interface atual é síncrona para o protótipo e precisa ser adaptada para chamadas de rede.

Contrato sugerido para discutir com o grupo (ainda não implementado):

| Método/rota | Ação |
|---|---|
| POST /auth/login | Validar credenciais e devolver sessão/token |
| GET /ordens | Listar com busca, status e paginação |
| GET /ordens/{id} | Consultar detalhes |
| POST /ordens | Criar ordem |
| PUT /ordens/{id} | Editar dados |
| PATCH /ordens/{id}/status | Alterar status com regra e histórico |
| DELETE /ordens/{id} | Excluir conforme permissões |
| GET /produtos | Consultar produtos/estoque |
| POST /movimentacoes-estoque | Registrar entrada/saída em transação |

O servidor deve validar os campos e as permissões, gerar IDs, controlar o saldo sem deixá-lo negativo, armazenar senhas com hash e definir como conflitos de edição serão tratados. BigDecimal representa dinheiro; enum deve ter um código estável no contrato. Definir também o significado de exclusão e a relação entre conclusão/cancelamento de OS e consumo de estoque.

## Validação e roteiro de conferência

Os XMLs, referências locais de recursos e declaração das Activities foram conferidos por script. A compilação `:app:assembleDebug` foi tentada, mas a rede do ambiente impediu baixar o Gradle (`Network is unreachable`). O ambiente também não contém SDK Android nem emulador; **não foi gerado APK e não houve execução das telas**.

Ao abrir no Android Studio, confira:

1. Login inválido mostra erro; credenciais demo abrem a lista vazia.
2. Salvar sem os campos obrigatórios mostra erro; e-mail informado inválido também.
3. Criar uma OS, buscar por cliente e filtrar por status.
4. Editar sem alterar ID/data de criação; conferir atualização nos detalhes.
5. Cancelar a troca de status preserva o valor; confirmar atualiza lista e detalhes.
6. Cancelar exclusão preserva a OS; confirmar remove a ordem.
7. Fechar e reabrir o aplicativo preserva os registros; sair volta ao login.
8. Gerar PDF com descrição longa: conferir quebras de linha, páginas, acentos e destino.
9. Girar aparelho, abrir teclado e testar campos/botões em tela pequena.
