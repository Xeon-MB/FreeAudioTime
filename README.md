# FreeAudioTime

**Acompanhe quanto tempo você passa ouvindo música e outros conteúdos de áudio.**

O FreeAudioTime é um aplicativo Android desenvolvido em Java que registra o tempo de reprodução de áudio de aplicativos compatíveis, permitindo acompanhar o tempo total de reprodução e o tempo dedicado a cada faixa.

## Sumário

- [Funcionalidades](#funcionalidades)
- [Instalação](#instalação)
  - [Como instalar pelo F-Droid](#como-instalar-pelo-f-droid)
- [Permissões](#permissões)
- [Tecnologias utilizadas](#tecnologias-utilizadas)
- [Distribuição](#distribuição)
- [Tutorial de Instalação e Configuração](#tutorial-de-instalação-e-configuração)
  - [Tutorial em vídeo](#tutorial-em-vídeo)
  - [Passo a passo (20 passos)](#passo-1-baixar-o-f-droid)
- [Solução de problemas](#solução-de-problemas)
  - [1. Acesso a notificações](#1-acesso-a-notificações-obrigatório)
  - [2. Economia de bateria](#2-economia-de-bateria)
  - [3. Início automático](#3-início-automático-xiaomi--redmi--poco)
  - [4. Travar o app nos recentes](#4-travar-o-app-nos-recentes)
  - [5. Aplicativo de música compatível](#5-aplicativo-de-música-compatível)
  - [6. Versão do aplicativo](#6-versão-do-aplicativo)
  - [Ainda não funcionou?](#ainda-não-funcionou)
- [Desenvolvedor](#desenvolvedor)

## Funcionalidades

- Registro do tempo total de reprodução.
- Contagem individual do tempo de cada música.
- Identificação do título e do artista, quando disponíveis.
- Acompanhamento da reprodução em segundo plano.
- Armazenamento local dos dados.


### Como instalar pelo F-Droid

1. Instale o [F-Droid](https://f-droid.org/).
2. Abra as configurações e acesse **Repositórios**.
3. Adicione o endereço do repositório do FreeAudioTime.
4. Pesquise por `FreeAudioTime`.
5. Instale o aplicativo.

## Permissões

O aplicativo utiliza o acesso às notificações para identificar sessões de mídia e obter informações sobre o conteúdo reproduzido.

Para funcionar corretamente, é necessário conceder a permissão de acesso às notificações nas configurações do Android.

## Tecnologias utilizadas

- Java
- Android Studio
- Android SDK
- MediaSessionManager
- NotificationListenerService
- Git e GitHub

## Distribuição

O aplicativo é distribuído por meio de um repositório F-Droid próprio, hospedado no GitHub Pages.

## Desenvolvedor

**Xeon-MB**

GitHub: https://github.com/Xeon-MB

---

*FreeAudioTime — Conheça melhor seu tempo de áudio.*

## Tutorial de Instalação e Configuração

## Tutorial em vídeo

Clique na imagem abaixo para assistir ao tutorial de instalação e configuração do FreeAudioTime.

<p align="center">
  <a href="https://youtu.be/QhhEblQsE8Y">
    <img src="images/tutorial-thumbnail.png"
         alt="Tutorial FreeAudioTime"
         width="500">
  </a>
</p>

---

### Passo 1: Baixar o F-Droid
Acesse o site oficial do F-Droid e toque no botão "BAIXAR O F-DROID".

![Baixar o F-Droid](images/01.png)

---

### Passo 2: Executar o instalador APK
Na pasta de transferências, selecione o arquivo `F-Droid.apk` baixado.

![Executar o instalador APK](images/02.png)

---

### Passo 3: Aguardar atualização inicial
Ao abrir o F-Droid pela primeira vez, aguarde o aplicativo buscar os repositórios padrão.

![Aguardar atualização inicial](images/03.png)

---

### Passo 4: Acessar Repositórios
Na tela principal do F-Droid, toque no ícone da caixa (gerenciador de repositórios) no menu superior.

![Acessar Repositórios](images/04..png)

---

### Passo 5: Adicionar novo repositório
Na tela de Repositórios, toque no botão com o ícone de soma `+` no canto inferior direito.

![Adicionar novo repositório](images/05..png)

---

### Passo 6: Escolher entrada de URL manual
Selecione a opção "Digitar URL do repositório manualmente".

![Escolher entrada de URL manual](images/06.png)

---

### Passo 7: Inserir a URL do repositório
Cole a URL do repositório do FreeAudioTime no campo indicado e toque em "Adicionar".

![Inserir a URL do repositório](images/07.png)

---

### Passo 8: Confirmar a inclusão
Confirme as informações do repositório exibido e toque no botão "Adicionar repositório".

![Confirmar a inclusão](images/08.png)

---

### Passo 9: Selecionar o app FreeAudioTime
Toque no card do aplicativo FreeAudioTime listado.

![Selecionar o app FreeAudioTime](images/09.png)

---

### Passo 10: Iniciar instalação
Na tela de detalhes do aplicativo, toque no botão "Instalar".

![Iniciar instalação](images/10.png)

---

### Passo 11: Permitir fontes desconhecidas
Caso o Android exiba a mensagem de segurança sobre a instalação de fontes desconhecidas, toque em "Configurações".

![Permitir fontes desconhecidas](images/11.png)

---

### Passo 12: Autorizar permissão no Android
Ative a opção "Autorizar permissão" para permitir instalações através do F-Droid.

![Autorizar permissão no Android](images/12.png)

---

### Passo 13: Confirmar instalação do app
Na caixa de diálogo que surgir, toque no botão "Instalar".

![Confirmar instalação do app](images/13.png)

---

### Passo 14: Abrir o aplicativo
Após o término da instalação, toque no botão "Abrir".

![Abrir o aplicativo](images/14.png)

---

### Passo 15: Configurar pasta de armazenamento
Na tela de boas-vindas do FreeAudioTime, toque em "Escolher pasta" para definir onde o histórico será salvo.

![Configurar pasta de armazenamento](images/15.png)

---

### Passo 16: Navegar até a pasta Documents
No gerenciador de arquivos do dispositivo, selecione a pasta "Documents".

![Navegar até a pasta Documents](images/16.png)

---

### Passo 17: Confirmar seleção da pasta
Dentro do diretório de documentos, toque no botão "USAR ESTA PASTA" na parte inferior.

![Confirmar seleção da pasta](images/17.png)

---

### Passo 18: Autorizar acesso do aplicativo
Toque em "PERMITIR" na mensagem de confirmação do sistema para permitir acesso à pasta selecionada.

![Autorizar acesso do aplicativo](images/18.png)

---

### Passo 19: Solicitação de permissão de notificações
No painel principal do app, observe o aviso de permissão necessária e toque no botão "Ativar permissão".

![Solicitação de permissão de notificações](images/19.png)

---

### Passo 20: Conceder acesso às notificações
Nas configurações do sistema Android ("Acessar notificações"), encontre o FreeAudioTime e ative a chave correspondente.

![Conceder acesso às notificações](images/20.png)

## Solução de problemas

Se o FreeAudioTime não estiver identificando a música ou contando o tempo, siga os passos abaixo, na ordem. Os nomes dos menus podem variar conforme o fabricante e a versão do sistema.

### 1. Acesso a notificações (obrigatório)
O app usa um serviço de escuta de notificações para acompanhar as sessões de mídia. Sem essa permissão, ele não detecta nenhuma reprodução.

1. Abra **Configurações → Notificações → Acesso a notificações**.
2. Ative o **FreeAudioTime**.
3. Se já estiver ativo e não funcionar, desative e ative novamente.

### 2. Economia de bateria
1. Abra **Configurações → Aplicativos → Gerenciar aplicativos → FreeAudioTime**.
2. Toque em **Economia de bateria** e selecione **Sem restrições**.
3. Se existir a opção **Pausar atividade do app se não usado**, desative-a.

### 3. Início automático (Xiaomi / Redmi / POCO)
1. Abra **Configurações → Aplicativos → Permissões → Início automático**.
2. Ative o **FreeAudioTime**.

Em algumas versões, a opção fica em **Gerenciar aplicativos → FreeAudioTime → Início automático**.

### 4. Travar o app nos recentes
1. Abra a tela de aplicativos recentes.
2. Segure o cartão do FreeAudioTime e toque no ícone de **cadeado**.

### 5. Aplicativo de música compatível
O app só contabiliza o tempo de aplicativos da lista de compatibilidade:

- Stash
- Spotify
- YouTube Music
- Deezer
- SoundCloud
- Amazon Music
- Apple Music
- Opentune
- Wlisten
- LarkPlayer

### 6. Versão do aplicativo
Use sempre a versão mais recente do APK.

### Ainda não funcionou?
Abra uma *issue* informando o **modelo do aparelho**, a **versão do Android** e, se for Xiaomi, a **versão do HyperOS/MIUI**.

## Instalação

O FreeAudioTime possui um repositório próprio no F-Droid.

**Endereço do repositório:**

https://xeon-mb.github.io/FreeAudioTime/repo
