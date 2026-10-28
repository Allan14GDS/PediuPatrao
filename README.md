# 🍕 Pediu Patrão

Sistema **SaaS web responsivo** para a gestão inteligente de pizzarias e operações de delivery. Centraliza pedidos, clientes, cardápio, equipa e rastreio operacional num único painel, com controlo de acesso por perfil e registo de auditoria das operações críticas.

![Java](https://img.shields.io/badge/Java-17%2B-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-3-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)

---

## ✨ Funcionalidades Principais

### 🔐 Controle de Acesso Baseado em Perfis (RBAC)

A autenticação usa formulário de login do Spring Security, com passwords armazenadas em **BCrypt**. Cada sessão recebe um dos papéis definidos em `Role`: **Atendente**, **Gerente** e **Admin**. As rotas MVC e a API são autorizadas em `SecurityConfig`.

| Capacidade | Atendente | Gerente | Admin |
|---|:---:|:---:|:---:|
| Consultar pedidos | ✅ | ✅ | ✅ |
| Criar pedidos | ✅ | ✅ | — |
| Avançar o status do pedido | ✅ | ✅ | ✅ |
| Cancelar pedido e aplicar desconto | — | ✅ | ✅ |
| Consultar e manter clientes | ✅ | consulta | — |
| Consultar a auditoria | — | ✅ | ✅ |
| Gerir utilizadores | — | — | ✅ |
| Área administrativa (`/admin`) | — | ✅ | ✅ |

Após o login, o utilizador entra em `/pedidos`. O logout redireciona para `/login?logout`.

### 🔄 Máquina de Estados de Pedidos

Todo pedido nasce em `RECEBIDO`. O ciclo operacional, modelado em `StatusPedido`, acompanha a pizza do balcão até a entrega:

`RECEBIDO` → `EM_PREPARACAO` → `PRONTO` → `SAIU_PARA_ENTREGA` ou `RETIRADO` → `FINALIZADO`

Estados terminais e regras de negócio em `PedidoService`:

- **Cancelamento** só é permitido enquanto o pedido está em `RECEBIDO`, `EM_PREPARACAO` ou `PRONTO`. Depois da saída para entrega, a operação é recusada e exige justificativa.
- **Desconto** limitado a **20%**, aplicável apenas antes da saída do pedido. O valor total é recalculado a partir dos itens.
- Cada inclusão, mudança de status, cancelamento e desconto gera um registo de auditoria.

### 📒 Auditoria Imutável

`AuditoriaService` apenas **regista e consulta**. Não há atualização nem exclusão de trilhas. Cada evento guarda utilizador, perfil, data e hora, operação, entidade, campo alterado, valor anterior, valor novo e justificativa.

Operações rastreadas:

- Inclusão, alteração de status, cancelamento e desconto de **pedidos**
- Inclusão e alteração de **clientes** (campo a campo)

A consulta em `/auditoria` está restrita a **Admin** e **Gerente**, ordenada da mais recente para a mais antiga.

### 🎨 UI/UX Moderna

- **Landing page** em `/` (`index.html`): dark mode, tipografia Space Grotesk e DM Sans, paleta quente e layout responsivo com Tailwind CSS.
- **Login em split layout** (`login.html`): painel escuro de marca à esquerda e formulário claro à direita, empilhado em ecrãs pequenos.
- **Painel operacional** em Thymeleaf (`layout.html`) para pedidos, clientes, produtos, utilizadores e auditoria.

---

## 🛠️ Principais Alterações Realizadas

Para atingir o estado atual de "Release Candidate", as seguintes evoluções foram aplicadas ao projeto base:
- **Refatoração de Segurança:** Implementação de um modelo RBAC rigoroso no `SecurityConfig`, com proteção de rotas da API e ocultação dinâmica de elementos no Thymeleaf baseada no perfil ativo.
- **Motor de Auditoria:** Criação do `AuditoriaService` para o registo imutável de transações críticas (alterações de estado, aplicação de descontos, cancelamentos e modificação de dados de clientes), garantindo a precisão das formatações financeiras.
- **Regras de Negócio Avançadas:** Introdução de validações no `PedidoService` para limitar a aplicação de descontos (máximo de 20%) e restringir cancelamentos consoante o estado atual do pedido, delegando esta autoridade exclusivamente ao Gerente.
- **Renovação UI/UX:** Desenvolvimento de uma nova Landing Page pública em *Dark Mode* e reestruturação do ecrã de autenticação (*Split Layout*) utilizando Tailwind CSS, elevando a apresentação visual a um padrão de produto SaaS comercial.

---

## 🚀 Como Executar

### Pré-requisitos

- **Java 17+** (o projeto compila com Java 21, declarado no `pom.xml`)
- **MongoDB** acessível (local ou Atlas)
- Maven Wrapper já incluído (`./mvnw`); não é preciso instalar o Maven

### 1. Configurar a base de dados

Na raiz do projeto, crie um ficheiro `.env`. O `spring-dotenv` injeta estas variáveis em `application.properties`:

```properties
MONGODB_URI=mongodb://localhost:27017
MONGODB_DATABASE=pizzaria
```

Substitua a URI pela connection string do seu cluster, se usar MongoDB Atlas.

### 2. Subir a aplicação

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

### 3. Abrir no browser

| Ecrã | URL |
|---|---|
| Landing page | http://localhost:8080/ |
| Login | http://localhost:8080/login |
| Pedidos (após autenticação) | http://localhost:8080/pedidos |

Na primeira execução, o `DataInitializer` cria ou atualiza os três utilizadores de teste. As passwords são gravadas já cifradas com BCrypt.

---

## 🔑 Credenciais de Teste

Utilizadores semeados por `DataInitializer` (username e password em texto simples, antes da cifra):

| Perfil | Utilizador | Password | Papel |
|---|---|---|---|
| 👑 Admin | `admin` | `admin` | `ADMIN` |
| 🏪 Gerente | `gerente` | `gerente` | `GERENTE` |
| 🎧 Atendente | `atendente` | `atendente` | `ATENDENTE` |

> Estas contas existem apenas para desenvolvimento e demonstração. Não as reutilize em produção.

---

## 🧱 Visão da Arquitetura

```text
Browser (Thymeleaf + Tailwind)
        │
        ▼
Controllers  ──  Spring Security (RBAC)
        │
        ▼
Services (pedidos, clientes, auditoria, utilizadores)
        │
        ▼
MongoDB  (pedidos, clientes, produtos, usuarios, auditorias)
```

Pacote base: `com.umc.pediupatrao`. Classe de arranque: `PizzariaPedidosApplication`.
# 🍕 Pediu Patrão

Sistema **SaaS web responsivo** para a gestão inteligente de pizzarias e operações de delivery. Centraliza pedidos, clientes, cardápio, equipa e rastreio operacional num único painel, com controlo de acesso por perfil e registo de auditoria das operações críticas.

![Java](https://img.shields.io/badge/Java-17%2B-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-3-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)

---

## ✨ Funcionalidades Principais

### 🔐 Controle de Acesso Baseado em Perfis (RBAC)

A autenticação usa formulário de login do Spring Security, com passwords armazenadas em **BCrypt**. Cada sessão recebe um dos papéis definidos em `Role`: **Atendente**, **Gerente** e **Admin**. As rotas MVC e a API são autorizadas em `SecurityConfig`.

| Capacidade | Atendente | Gerente | Admin |
|---|:---:|:---:|:---:|
| Consultar pedidos | ✅ | ✅ | ✅ |
| Criar pedidos | ✅ | ✅ | — |
| Avançar o status do pedido | ✅ | ✅ | ✅ |
| Cancelar pedido e aplicar desconto | — | ✅ | ✅ |
| Consultar e manter clientes | ✅ | consulta | — |
| Consultar a auditoria | — | ✅ | ✅ |
| Gerir utilizadores | — | — | ✅ |
| Área administrativa (`/admin`) | — | ✅ | ✅ |

Após o login, o utilizador entra em `/pedidos`. O logout redireciona para `/login?logout`.

### 🔄 Máquina de Estados de Pedidos

Todo pedido nasce em `RECEBIDO`. O ciclo operacional, modelado em `StatusPedido`, acompanha a pizza do balcão até a entrega:

`RECEBIDO` → `EM_PREPARACAO` → `PRONTO` → `SAIU_PARA_ENTREGA` ou `RETIRADO` → `FINALIZADO`

Estados terminais e regras de negócio em `PedidoService`:

- **Cancelamento** só é permitido enquanto o pedido está em `RECEBIDO`, `EM_PREPARACAO` ou `PRONTO`. Depois da saída para entrega, a operação é recusada e exige justificativa.
- **Desconto** limitado a **20%**, aplicável apenas antes da saída do pedido. O valor total é recalculado a partir dos itens.
- Cada inclusão, mudança de status, cancelamento e desconto gera um registo de auditoria.

### 📒 Auditoria Imutável

`AuditoriaService` apenas **regista e consulta**. Não há atualização nem exclusão de trilhas. Cada evento guarda utilizador, perfil, data e hora, operação, entidade, campo alterado, valor anterior, valor novo e justificativa.

Operações rastreadas:

- Inclusão, alteração de status, cancelamento e desconto de **pedidos**
- Inclusão e alteração de **clientes** (campo a campo)

A consulta em `/auditoria` está restrita a **Admin** e **Gerente**, ordenada da mais recente para a mais antiga.

### 🎨 UI/UX Moderna

- **Landing page** em `/` (`index.html`): dark mode, tipografia Space Grotesk e DM Sans, paleta quente e layout responsivo com Tailwind CSS.
- **Login em split layout** (`login.html`): painel escuro de marca à esquerda e formulário claro à direita, empilhado em ecrãs pequenos.
- **Painel operacional** em Thymeleaf (`layout.html`) para pedidos, clientes, produtos, utilizadores e auditoria.

---

## 🛠️ Principais Alterações Realizadas

Para atingir o estado atual de "Release Candidate", as seguintes evoluções foram aplicadas ao projeto base:
- **Refatoração de Segurança:** Implementação de um modelo RBAC rigoroso no `SecurityConfig`, com proteção de rotas da API e ocultação dinâmica de elementos no Thymeleaf baseada no perfil ativo.
- **Motor de Auditoria:** Criação do `AuditoriaService` para o registo imutável de transações críticas (alterações de estado, aplicação de descontos, cancelamentos e modificação de dados de clientes), garantindo a precisão das formatações financeiras.
- **Regras de Negócio Avançadas:** Introdução de validações no `PedidoService` para limitar a aplicação de descontos (máximo de 20%) e restringir cancelamentos consoante o estado atual do pedido, delegando esta autoridade exclusivamente ao Gerente.
- **Renovação UI/UX:** Desenvolvimento de uma nova Landing Page pública em *Dark Mode* e reestruturação do ecrã de autenticação (*Split Layout*) utilizando Tailwind CSS, elevando a apresentação visual a um padrão de produto SaaS comercial.

---

## 🚀 Como Executar

### Pré-requisitos

- **Java 17+** (o projeto compila com Java 21, declarado no `pom.xml`)
- **MongoDB** acessível (local ou Atlas)
- Maven Wrapper já incluído (`./mvnw`); não é preciso instalar o Maven

### 1. Configurar a base de dados

Na raiz do projeto, crie um ficheiro `.env`. O `spring-dotenv` injeta estas variáveis em `application.properties`:

```properties
MONGODB_URI=mongodb://localhost:27017
MONGODB_DATABASE=pizzaria
```

Substitua a URI pela connection string do seu cluster, se usar MongoDB Atlas.

### 2. Subir a aplicação

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

### 3. Abrir no browser

| Ecrã | URL |
|---|---|
| Landing page | http://localhost:8080/ |
| Login | http://localhost:8080/login |
| Pedidos (após autenticação) | http://localhost:8080/pedidos |

Na primeira execução, o `DataInitializer` cria ou atualiza os três utilizadores de teste. As passwords são gravadas já cifradas com BCrypt.

---

## 🔑 Credenciais de Teste

Utilizadores semeados por `DataInitializer` (username e password em texto simples, antes da cifra):

| Perfil | Utilizador | Password | Papel |
|---|---|---|---|
| 👑 Admin | `admin` | `admin` | `ADMIN` |
| 🏪 Gerente | `gerente` | `gerente` | `GERENTE` |
| 🎧 Atendente | `atendente` | `atendente` | `ATENDENTE` |

> Estas contas existem apenas para desenvolvimento e demonstração. Não as reutilize em produção.

---

## 🧱 Visão da Arquitetura

```text
Browser (Thymeleaf + Tailwind)
        │
        ▼
Controllers  ──  Spring Security (RBAC)
        │
        ▼
Services (pedidos, clientes, auditoria, utilizadores)
        │
        ▼
MongoDB  (pedidos, clientes, produtos, usuarios, auditorias)
```

Pacote base: `com.umc.pediupatrao`. Classe de arranque: `PizzariaPedidosApplication`.
