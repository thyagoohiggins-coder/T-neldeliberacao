# Hora a Hora — Túnel de liberação FAI

App Android para registrar e organizar os veículos que passam pelo **Túnel de
liberação FAI**, agrupando por intervalo de hora e gerando as mensagens prontas
para enviar no **WhatsApp**. Versão inicial: **0.1**.

> Splash de abertura: "Criado por Hikins" (2 segundos).

## O que o app faz

- **Registrar veículo:** VIN, Status (Liberado / Negado), Motivo (só quando Negado:
  *Demérito em aberto* ou *Reparo Inadequado*) e Intervalo (06:00 … 15:00).
- **Organização automática:** os veículos são agrupados por intervalo em **ordem
  decrescente** (15:00 no topo) e, dentro de cada intervalo, na ordem de adição.
- **Formatar intervalo → WhatsApp:** cada intervalo tem um botão que abre o
  WhatsApp com a lista numerada pronta (sem emojis). Ex.:
  ```
  1. VIN: 95PBEK31DVB105453 Status: Negado Motivo: Demérito em aberto Intervalo: 13:00
  2. VIN: 95PBSK31DVB105345 Status: Negado Motivo: Reparo Inadequado Intervalo: 13:00
  3. VIN: 95PBSK31DVB105345 Status: Liberado Intervalo: 13:00
  ```
- **Estatística por intervalo (opcional na mensagem):** você digita **Prod** e
  **Aprov**, o app calcula **Dif = Produção − Meta** (negativo quando não bateu a
  meta, positivo quando passou):
  ```
  Intervalo: 13:00 Meta: 10 Prod: 4 Aprov: 2 Dif: -6
  ```
  As **metas** são fixas: 06:00=8, 07:00=10, 08:00=8, 09:00=10, 10:00=10,
  11:00=6, 12:00=2, 13:00=10, 14:00=8, 15:00=8 (total **80**).
- **Resumo total:** `TTL: 80 Prod: <nº de VINs> Aprov: <nº de Liberados> Dif: <saldo>`,
  onde **Dif** é o **saldo acumulado** = soma dos `Prod − Meta` de todos os
  intervalos presentes (termina positivo ou negativo).
- **Dados salvos no aparelho** (reaparecem ao reabrir). Botão **Limpar tudo** no
  menu para começar um novo turno.
- **Atualização automática:** ao abrir e pelo menu *Verificar atualização*, o app
  consulta as *releases* deste repositório e, se houver versão nova, baixa e
  instala o APK.

## Como gerar o APK (release automática)

O APK é compilado e publicado **automaticamente** pelo GitHub Actions
(`.github/workflows/release.yml`) em dois casos:

- **A cada push na branch de desenvolvimento** (`claude/**`): a release é
  (re)criada com a versão do arquivo **`VERSION`** (hoje `0.1`). Prático para já
  ter o APK sem precisar de tag.
- **Ao criar uma tag `v*`** (ex.: `v0.2`): publica aquela versão específica.

Para lançar uma nova versão numerada, edite o arquivo `VERSION` (ou crie uma tag
`vX.Y`). O nome da versão dentro do app segue esse valor.

### 1. Cadastrar os Secrets de assinatura (uma única vez)

Para que as atualizações instalem por cima da versão anterior, o APK precisa ser
sempre assinado com a **mesma chave**. Em **Settings → Secrets and variables →
Actions → New repository secret**, crie os 4 secrets:

| Secret | Valor |
|---|---|
| `KEYSTORE_BASE64` | (conteúdo do arquivo base64 da keystore — enviado à parte) |
| `KEYSTORE_PASSWORD` | senha da keystore |
| `KEY_ALIAS` | `tunelfai` |
| `KEY_PASSWORD` | senha da chave |

> Sem esses secrets o workflow ainda compila, mas o APK sai assinado em *debug* e
> as atualizações automáticas **não** instalam por cima da versão anterior.

### 2. Publicar uma versão

```bash
git tag v0.1
git push origin v0.1
```

O Actions compila o APK, cria a Release **v0.1** e anexa o arquivo
`HoraAHora-TunelFAI-v0.1.apk`. Para lançar a próxima versão, use `v0.2`, `v0.3`, etc.
Você também pode rodar o workflow manualmente em **Actions → Build e Release APK →
Run workflow** (gera o APK como *artifact* do run).

### 3. Instalar no celular

Baixe o `.apk` da Release, abra no Android e permita "instalar de fontes
desconhecidas". As próximas versões o próprio app oferece para atualizar.

## Rodar/compilar localmente (opcional)

Requer Android SDK. Debug: `./gradlew assembleDebug`. Release assinado: coloque a
`release.keystore` na raiz e defina as variáveis `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`, depois `./gradlew assembleRelease`.

## Estrutura

- `app/src/main/java/com/hikins/tunelfai/` — código Kotlin (UI em Jetpack Compose).
  - `model/` — dados e metas fixas · `data/` — persistência local ·
    `ui/` — telas e formatação · `update/` — atualização via GitHub.
- `.github/workflows/release.yml` — build + assinatura + publicação do APK.
