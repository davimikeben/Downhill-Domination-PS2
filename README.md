# Downhill Domination — Mobile (Android) 2.0.0

Jogo de PS2 no celular, com o **jogo completo publicado aqui no GitHub**, teclado virtual
completo e ajustes para GPU fraca (Mali-G52).

![Pad do jogo](docs/pad-preview.png)

## Baixar

| Arquivo | Tamanho | Para que serve |
|---|---|---|
| [DownhillDominationMobile-2.0.0.apk](https://github.com/davimikeben/Downhill-Domination-PS2/releases/latest) | 46 KB | o app (Android 7+) |
| `DownhillDomination.cso.000` | 1,25 GB | 1ª metade do jogo |
| `DownhillDomination.cso.001` | 1,25 GB | 2ª metade do jogo |

O jogo é um CSO (imagem comprimida de PS2) de **2.500.420.488 bytes**. O GitHub aceita no
máximo 2 GB por arquivo, então ele é publicado em **duas partes**; o app junta as duas em um
clique (menu *Juntar as 2 partes baixadas*) e confere o resultado.

> Por que não vai dentro do APK, e por que não fica "em MB": o disco tem 2,77 GB de dados
> **já comprimidos** (áudio e vídeo do jogo). Medi bloco a bloco: 93–96% dos setores não
> comprimem, e o CSO só ganha 10% (2,77 → 2,50 GB). Um APK com o jogo dentro passaria de
> 2,5 GB, acima do limite de 2 GB por arquivo de release do GitHub e do que o Android
> instala de forma confiável. Por isso: APK leve + jogo em 2 partes + junção automática.

## Como jogar (3 passos)

1. Instale o **APK** e um núcleo de PS2 no celular — o app detecta e leva na loja:
   **ARMSX2** (mais rápido em ARM), **AetherSX2**, **NetherSX2** ou **RetroArch + LRPS2**.
2. Baixe as **2 partes** do jogo (botão *Baixar o jogo do GitHub* já abre esta página) e
   toque em **Juntar as 2 partes baixadas** — pode marcar as duas em qualquer ordem.
   O app grava `downhill.cso` (2,5 GB), valida o cabeçalho CISO e mostra o serial
   **SLES-52202 (PAL)**, provando que o jogo está íntegro.
3. Toque em **▶ JOGAR**: a imagem vai para o núcleo por `content://`, sem copiar de novo.

Também funciona com a sua imagem original: **Importar ROM (.zip / .iso / .cso)** —
o zip de 2,42 GB é descompactado em streaming (com CRC e checagem de espaço).

## O que mudou na 2.0.0

**Pad redesenhado** (o PNG antigo era feio, e tinha um bug de verdade: um `Paint` herdava
alpha 18 e todas as teclas saíam lavadas):

- analógicos agora ficam **embaixo**, onde o dedão descansa (antes estavam no topo);
- **D-pad em cruz única**, com centro escavado e setas vetoriais (nada de emoji colado);
- botões de ação com os símbolos reais do PlayStation desenhados em vetor;
- ombros L1/L2/R1/R2 em pill, teclas extras só com ícone vetorial (raio, disquete, pasta,
  avançar, ajustes) — antes os rótulos vazavam da tecla;
- rótulos micro em caixa alta, rim de 1 dp e gradiente de vidro escuro;
- os emoji continuam no app onde eles ajudam a identificar: **legenda** e **leitura do
  Pad Lab** (mostra `🔺 Triângulo — Salto e manobras aéreas` ao apertar).

**Jogo completo no GitHub**: CSO dividido em 2 partes + junção dentro do app + link direto
para a release.

**Suporte a CSO**: o app lê o cabeçalho CISO, acha o `SYSTEM.CNF` descomprimindo só os
setores necessários e guarda a imagem comprimida (10% menos espaço no celular).

## Teclado virtual

21 teclas do DualShock 2 (direcional, △ ○ ✕ □, L1/L2/R1/R2, L3/R3, Select, Start) + 5 extras
(⚡ Turbo, 💾 Salvar, 📂 Carregar, ⏩ Avançar, ⚙ Ajustes), dois analógicos com zona morta e
ponteiro preso (o dedo não troca de tecla ao deslizar), vibração, escala e transparência.

- **Legenda com emoji**: cada tecla com o emoji do pad e a função no jogo.
- **Pad Lab**: tela cheia para testar, com contador de FPS real.
- **Exportar pad**: gera `downhill-pad.cfg` + `downhill-pad.png` + `LEIA-ME.txt` para o
  RetroArch (`Settings › On-Screen Display › On-Screen Overlay`). Exemplo pronto em
  [`docs/downhill-pad.cfg`](docs/downhill-pad.cfg) (23 descritores: 21 teclas + 2 analógicos).

## Otimização para Mali-G52

| Onde | Ajuste | Valor |
|---|---|---|
| Graphics | Renderer | OpenGL |
| Graphics | Internal Resolution | **1x (nativo)** |
| Graphics | Texture Filtering / FXAA / Shade Boost | Desligado |
| Emulation | **MT-VU** | Ligado |
| Emulation | EE Cycle Rate / Skip | 100% / 1 |
| System | Sustained Performance Mode | Ligado |

O app mostra a lista completa e copia para a área de transferência. O pad é desenhado em um
único `Canvas` (sem inflar layout por frame) e o Pad Lab não roda loop de animação quando a
tela está em segundo plano.

## Ferramentas

```bash
scripts/setup-toolchain.sh                       # JDK 17 + SDK 34 + Gradle 8.7
python3 tools/ciso.py encode jogo.iso jogo.cso   # ISO -> CSO (alinhamento p/ >2 GB)
python3 tools/ciso.py test jogo.cso              # confere o cabeçalho CISO
scripts/fetch-rom.sh                             # baixa o arquivo original do TransferNow
split -n 2 -d -a 3 jogo.cso DownhillDomination.cso.   # as partes da release
```

## Estrutura

```
app/src/main/java/com/downhill/mobile/
├── MainActivity.java            # importar, juntar partes, jogar, exportar, ajustes
├── PadLabActivity.java          # teste do pad em tela cheia + FPS
├── legend/LegendActivity.java   # legenda com emoji de cada tecla
├── pad/                         # modelo das teclas, geometria, desenho, toque
├── rom/                         # ISO 9660, CISO (CSO), importador, junção, provider
├── emu/                         # detecção dos núcleos + tabela de otimização
├── export/                      # overlay do RetroArch (.cfg) + PNG
└── ui/                          # estilo dos cartões e botões
```

## Testes (54)

`gradle :app:testDebugUnitTest`:

- geometria do pad em 8 telas (nenhuma tecla fora da tela, sem sobreposição, hit-test);
- 21 teclas com emoji, dica e códigos de gamepad únicos;
- ISO 9660 sintético (PAL/NTSC-U, lixo, rótulo de volume);
- **CISO**: ida e volta de setores, blocos comprimidos e crus, `align=2` (>2 GB), lixo;
- **junção das partes**: ordem `.000/.001/.010`, byte a byte, cancelamento;
- extração de zip/iso (truncado, cancelado, sem imagem dentro);
- overlay do RetroArch (formato, faixas 0..1, mapa DualShock→RetroPad);
- tabela de otimização e escolha do núcleo;
- **Robolectric**: abre as três telas, rasteriza o pad com Skia (pixels conferidos), gera os
  arquivos do export, roda import zip→iso→serial.

Evidência da verificação com os arquivos reais em [`docs/verification.txt`](docs/verification.txt).

## Signature

APK assinado com `signing/downhill-release.jks` (senha `downhill2026`, alias `downhill`).
