$ErrorActionPreference = 'Stop'

$arquivo = Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE 'Downloads') `
    -Filter '*firebase-adminsdk*.json' -File -ErrorAction SilentlyContinue
if (@($arquivo).Count -eq 1) {
    $caminho = $arquivo.FullName
    Write-Host "Chave do Firebase encontrada em Downloads."
} else {
    $caminho = (Read-Host 'Caminho do JSON da conta de serviço do Firebase').Trim().Trim('"')
}
if (!$caminho -or !(Test-Path -LiteralPath $caminho -PathType Leaf)) {
    throw 'Arquivo de credenciais do Firebase não encontrado.'
}
$caminho = (Resolve-Path -LiteralPath $caminho).Path
try {
    $credencial = Get-Content -LiteralPath $caminho -Raw | ConvertFrom-Json
    $projeto = $credencial.project_id
    if ($credencial.type -ne 'service_account' -or !$projeto) {
        throw 'Use o JSON da conta de serviço do Firebase.'
    }
} catch {
    throw 'Use um JSON válido da conta de serviço do Firebase Admin SDK.'
} finally {
    Remove-Variable credencial -ErrorAction SilentlyContinue
}

$uid = (Read-Host "UID do usuário que recebe as ordens no Firebase ($projeto)").Trim()
$email = (Read-Host 'Endereço completo da caixa Gmail').Trim()
$pasta = (Read-Host 'Nome do marcador Gmail dedicado aos e-mails de teste').Trim()
if (!$uid -or $email -notmatch '^[^\s@]+@[^\s@]+\.[^\s@]+$' -or !$pasta -or $pasta -ieq 'INBOX') {
    throw 'Informe o UID, um e-mail válido e um marcador dedicado diferente da Caixa de entrada.'
}

do {
    $senhaOculta = Read-Host 'Senha de app do Google (16 caracteres)' -AsSecureString
    $senha = ([System.Net.NetworkCredential]::new('', $senhaOculta)).Password -replace '\s', ''
    $senhaOculta.Dispose()
    if ($senha.Length -ne 16) { Write-Host 'Cole somente o código de 16 caracteres.' }
} until ($senha.Length -eq 16)

$configuracao = @{
    GOOGLE_APPLICATION_CREDENTIALS = $caminho
    FIREBASE_PROJECT_ID = $projeto
    FIREBASE_USUARIO_UID = $uid
    API_TOKEN = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
    API_ENDERECO = '127.0.0.1'
    API_PORTA = '8080'
    IMAP_HOST = 'imap.gmail.com'
    IMAP_PORTA = '993'
    IMAP_USUARIO = $email
    IMAP_SENHA = $senha
    IMAP_PASTA = $pasta
    IMAP_PROXY_HOST = $null
    IMPORTACAO_CRON = '0 * * * * *'
}
Remove-Variable senha
$anterior = @{}
try {
    foreach ($nome in $configuracao.Keys) {
        $anterior[$nome] = [Environment]::GetEnvironmentVariable($nome, 'Process')
        [Environment]::SetEnvironmentVariable($nome, $configuracao[$nome], 'Process')
    }
    Write-Host "A API consulta o marcador '$pasta' a cada minuto. Pressione Ctrl+C para parar."
    & (Join-Path $PSScriptRoot 'gradlew.bat') -p (Join-Path $PSScriptRoot 'api-email') bootRun
    if ($LASTEXITCODE -ne 0) { throw 'A API encerrou com erro. Confira a mensagem acima.' }
} finally {
    foreach ($nome in $anterior.Keys) {
        [Environment]::SetEnvironmentVariable($nome, $anterior[$nome], 'Process')
    }
}
