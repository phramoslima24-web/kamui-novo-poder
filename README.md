# Kamui: Prisão — addon para Narutoto 0.4.1 (Forge 1.20.1)

Adiciona o jutsu **Kamui: Prisão** à própria tela **Dojutsu** do Narutoto (botão abaixo do painel, junto dos
Kamui originais), usando o mesmo estado de jutsus (`PlayerVariables`), os mesmos slots e as mesmas teclas.

## Como usar
1. Abra a tela Dojutsu e clique em **Kamui: Prisão** (aprende, gastando JP 8 / DS 150 / Mangekyo de Obito — igual ao
   Kamui Transferência; em criativo equipa direto). Ele é equipado no slot de jutsu selecionado.
2. Alterne o modo com as **mesmas setas ← / →** do Kamui original:
   - **Total (Prender):** clique esquerdo em qualquer mob ou jogador → espiral do Kamui → alvo é preso (custa 100 chakra).
   - **Parcial (Sair):** use o jutsu (tecla de usar, Z por padrão) → abre a lista de prisioneiros; clicar em um deles
     o liberta **onde você está agora**, em qualquer dimensão.

## Persistência
- Todos os prisioneiros ficam em `data/kamuiprison_prisoners.dat` (do overworld): único por mundo, independe da dimensão.
- **Mobs:** NBT completo é gravado e a entidade é removida; ao libertar é recriada igual (vida, equipamento, nome, dono...).
  Copias antigas de um mob preso são bloqueadas ao carregar chunks (sem duplicação).
- **Jogadores:** ficam na dimensão `kamuiprison:prison` (cela fechada, modo aventura). Se saírem/morrerem/relogarem,
  são trazidos de volta. Se forem libertados offline, saem ao logar. O modo de jogo original é restaurado.
- Admin: `/kamuiprison list` e `/kamuiprison free <nome|uuid>` (nível 2).
- Config: `config/kamuiprison-common.toml` (custos, delay, `sharedPrison`).

## Compilar pelo GitHub
1. Envie esta pasta inteira para um repositório (inclui `.github/workflows/build.yml`).
2. Aba **Actions** → rode o workflow (ou faça push). Baixe o artefato **kamuiprison-addon-jar**.
3. Coloque o JAR em `mods/` junto do `narutoto-0_4_1-forge.jar` (cliente e servidor).

O workflow usa JDK 17 e Gradle 8.1.1 (ForgeGradle 6 / Forge 47.3.0). O Narutoto **não** é necessário para compilar:
a integração é feita por reflexão em `compat/NarutoBridge.java`.
