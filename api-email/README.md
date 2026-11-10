# Importação de ordens por e-mail

Serviço Spring Boot independente do Android. Lê uma pasta IMAP por TLS em modo somente leitura. Cada mensagem vira uma OS no Firestore com o assunto como título, o remetente como cliente e o corpo como descrição. HTML é convertido em texto, anexos são ignorados e a descrição é limitada a 20 mil caracteres. A ordem começa como `ABERTA`, com prioridade `MEDIA` e responsável vazio.

O serviço usa o mesmo projeto Firebase e o UID da conta que vai receber as ordens no app. Em uma transação do Firestore, reserva o próximo número em `usuarios/{uid}/config/sequencia`, grava a OS em `ordens`, grava a criação em `registros` e avança o cursor da pasta em `importacoesEmail`. Repetir a importação da mesma pasta e conta preserva as ordens já importadas. Uma execução processa até 50 mensagens em ordem de UID. Se houver falha, a próxima execução retoma a partir da última mensagem confirmada. Se o servidor alterar o `UIDVALIDITY` da pasta, a importação pausa para evitar recriar ordens.

## Configuração

Use um JDK 17 ou superior. Escolha uma pasta ou caixa dedicada a solicitações. Na primeira execução, as mensagens que já estiverem nessa pasta entram na fila, inclusive as lidas.

| Variável | Valor |
| --- | --- |
| `FIREBASE_PROJECT_ID` | ID do mesmo projeto Firebase usado pelo app |
| `FIREBASE_USUARIO_UID` | UID da conta em **Firebase Authentication → Usuários** |
| `GOOGLE_APPLICATION_CREDENTIALS` | Caminho local para as credenciais da conta de serviço quando o ambiente não fornece credenciais automaticamente |
| `IMAP_HOST` | Servidor IMAP do provedor |
| `IMAP_USUARIO` e `IMAP_SENHA` | Login IMAP e a senha ou senha de aplicativo aceita pelo provedor |
| `IMAP_PORTA` | Opcional, padrão `993` |
| `IMAP_PASTA` | Opcional, padrão `INBOX` |
| `IMAP_PROXY_HOST` | Opcional, IP ou nome do proxy para a conexão IMAP |
| `IMAP_PROXY_PORTA` | Opcional, padrão `1080` |
| `IMAP_PROXY_TIPO` | Opcional, `socks5` (padrão) ou `http` (HTTP CONNECT) |
| `API_TOKEN` | Segredo aleatório com pelo menos 32 caracteres entre letras, dígitos, `_` e `-` |
| `IMPORTACAO_CRON` | Opcional. Padrão `0 * * * * *` para consultar a caixa a cada minuto. Use `-` para desligar o agendamento e importar pelo endpoint |
| `API_ENDERECO` e `API_PORTA` | Opcionais. Fora do Docker, o endereço padrão é `127.0.0.1`. A imagem Docker usa `0.0.0.0`. A porta é `8080` por padrão e pode ser definida por `PORT` quando `API_PORTA` estiver ausente |

Use uma identidade de servidor com acesso de leitura e escrita ao Firestore, por exemplo com o papel `roles/datastore.user`. O SDK Admin usa permissões IAM. Mantenha as credenciais e o token fora do repositório. A conexão IMAP desta versão usa autenticação por senha. Se o provedor exigir OAuth, será necessário adaptar a conexão.

## Usar CCProxy

No servidor do CCProxy, habilite **SOCKS/MMS** em **Options** e permita o IP da máquina que roda a API no serviço SOCKS. O servidor do proxy precisa conseguir alcançar o servidor IMAP na porta `993`. Use o IP do servidor CCProxy em `IMAP_PROXY_HOST`, ou `127.0.0.1` se estiver rodando na mesma máquina. Se a porta SOCKS for diferente de `1080`, configure `IMAP_PROXY_PORTA`. O proxy configurado aqui vale para a conexão IMAP.

Para usar o serviço **Secure** do CCProxy, defina `IMAP_PROXY_TIPO=http` e informe a porta desse serviço em `IMAP_PROXY_PORTA`. O serviço precisa permitir túneis HTTP CONNECT para a porta IMAP de destino.

## Rodar e verificar

Defina as variáveis no terminal ou na configuração do ambiente. Na raiz do repositório, rode em PowerShell:

```powershell
.\gradlew.bat -p api-email test
.\gradlew.bat -p api-email bootRun
```

Para testar o endpoint sem que o agendamento importe a mensagem antes do POST, defina `$env:IMPORTACAO_CRON = "-"` antes de executar `bootRun`. Envie um e-mail de teste à pasta configurada e, em outro PowerShell com o mesmo token, dispare a importação:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/importacoes/email `
  -Headers @{ Authorization = "Bearer $env:API_TOKEN" }
```

Em caso de sucesso, a API retorna `200` com `ordensCriadas` e `emAndamento`. Confira a OS e o registro de criação no Android, conectado com a conta do UID configurado. Repita o POST e verifique que o mesmo e-mail gera zero novas ordens. A API retorna `401` para token inválido, `409` quando já há importação em execução na instância e `503` em caso de falha.

Para gerar o JAR executável, rode `.\gradlew.bat -p api-email bootJar`. Em um servidor, exponha o endpoint por HTTPS caso precise acioná-lo remotamente.

## Rodar com Docker

Na raiz do repositório, rode o build. O Docker executa os testes da API, gera o JAR e copia o resultado para uma imagem de execução com Java 17.

```powershell
docker build -t pimobiles4-api-email .
```

No mesmo PowerShell em que você definiu as variáveis da tabela, inicie o contêiner. O caminho em `GOOGLE_APPLICATION_CREDENTIALS` deve apontar para o JSON da conta de serviço no seu computador. O Docker monta o arquivo como somente leitura.

```powershell
docker run --rm --name pimobiles4-api-email -p 127.0.0.1:8080:8080 `
  --mount "type=bind,source=$env:GOOGLE_APPLICATION_CREDENTIALS,target=/run/secrets/firebase.json,readonly" `
  --env GOOGLE_APPLICATION_CREDENTIALS=/run/secrets/firebase.json `
  --env FIREBASE_PROJECT_ID --env FIREBASE_USUARIO_UID --env API_TOKEN `
  --env IMAP_HOST --env IMAP_USUARIO --env IMAP_SENHA --env IMAP_PASTA `
  --env IMAP_PORTA --env IMPORTACAO_CRON `
  --env IMAP_PROXY_HOST --env IMAP_PROXY_PORTA --env IMAP_PROXY_TIPO `
  pimobiles4-api-email
```

Em outro terminal, use `curl.exe --fail http://127.0.0.1:8080/status`. A resposta `{"status":"ok"}` confirma que o servidor iniciou. Para validar a importação, envie um e-mail de teste e confira a ordem no Android.

- [Credenciais do Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)
- [Permissões IAM do Cloud Firestore](https://firebase.google.com/docs/firestore/security/iam)
