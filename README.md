# KRW Bank

KRW Bank é uma aplicação de banco digital full-stack, desenvolvida como projeto de portfólio. Ela oferece funcionalidades básicas de um sistema bancário, com cadastro e autenticação de usuários, operações financeiras (depósito, saque e transferência) e persistência de dados em arquivos JSON.

---

## 🔍 Visão Geral

- **Nome do projeto:** KRW Bank  
- **Linguagem principal:** Java  
- **Framework back-end:** Spark Java  
- **Bibliotecas:**  
  - [Gson](https://github.com/google/gson) para serialização JSON  
  - [Spark Java](https://sparkjava.com/) como micro-framework web  
- **Front-end:** HTML5, CSS3 e JavaScript puro  
- **Armazenamento:** Arquivos JSON (`usuarios.json`, `transacoes.json`)  

---

## 🚀 Funcionalidades

- **Cadastro de usuário** com validação de campos obrigatórios  
- **Login e geração de token** (Bearer Token)  
- **Operações financeiras**:  
  - Depósito  
  - Saque  
  - Transferência entre contas  
- **Histórico de transações** exibido no dashboard  
- **Persistência** de usuários e transações em arquivos JSON  
- **Interface web** responsiva e clean  

---

## ⚙️ Requisitos

- Java JDK 11 ou superior  
- Git  
- Navegador moderno (Chrome, Firefox, Edge)  

---

## 🛠️ Instalação e Execução

1. **Clone o repositório**  
   ```bash
   git clone https://github.com/caetanocarpes/krwBank-Final.git
   cd krwBank-Final
Compile e execute a aplicação Java (back-end)

Abra o projeto no IntelliJ e execute a classe App.java
ou

Pelo terminal:

bash
Copiar
Editar
javac -cp "lib/*" -d bin src/**/*.java
java -cp "bin:lib/*" app.App
Abra o front-end

Vá até a pasta frontend/

Abra o index.html no navegador

📁 Estrutura do Projeto
bash
Copiar
Editar
krwBank-Final/
├── src/                  # Código-fonte Java
│   └── app/              # Lógica de rotas e views
├── model/                # Classes de domínio (Usuário, Transação etc.)
├── frontend/             # HTML, CSS e JS da interface
├── usuarios.json         # Armazenamento dos usuários
├── transacoes.json       # Histórico de transações
├── README.md             # Este arquivo

📄 Licença
Este projeto está sob a licença MIT.
Feito com 💙 por Caetano Carpes
