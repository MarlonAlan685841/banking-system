# 🏦 Sistema Bancário em Java (PostgreSQL)

Este projeto implementa um sistema bancário em **Java** com integração ao banco de dados **PostgreSQL** via JDBC, permitindo criar contas, depositar, sacar, consultar saldos e transferir valores entre contas, com autenticação segura por senha.

## 🚀 Visão Geral

O programa é composto por:

- **Interface Principal**: `BankingSystem.java` (menu interativo no console)
- **Acesso ao Banco**: `ContaDAO.java` (gerencia operações no PostgreSQL)
- **Modelo de Dados**: `Conta.java` (representa uma conta bancária)
- **Script SQL**: `create_database.sql` (cria o banco de dados)

👉 **Baixe este README**: [README.md](https://raw.githubusercontent.com/seu_usuario/banking-system/main/README.md)  
👉 **Baixe o projeto completo**: [banking-system.zip](https://github.com/seu_usuario/banking-system/archive/refs/heads/main.zip)

---

## 📋 Regras do Sistema

- Contas são identificadas por um **ID único** e protegidas por **senha** (hasheada com jBCrypt).
- Operações (depósito, saque, consulta, transferência) exigem autenticação com ID e senha.
- O saldo não pode ser negativo, e todas as transações são registradas.
- Transferências verificam saldo suficiente e validade das contas.
- O sistema valida entradas e exibe mensagens claras para erros.

---

## ⚙️ Como Funciona o Código

### 🔐 Constantes e Configurações

```java
private static final String DB_URL = "jdbc:postgresql://localhost:5432/bank_db";
private static final String DB_USER = "postgres";
private static final String DB_PASSWORD = "sua_senha_aqui";
```

Define a conexão com o PostgreSQL (URL, usuário e senha).

---

### 🏦 Estrutura do Banco de Dados

```sql
CREATE TABLE IF NOT EXISTS contas (
                id_conta SERIAL PRIMARY KEY,
                nome_titular VARCHAR(100) NOT NULL,
                senha VARCHAR(255) NOT NULL,
                saldo DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                CONSTRAINT chk_saldo CHECK (saldo >= 0));
								
CREATE TABLE IF NOT EXISTS transacoes (
    id_transacao SERIAL PRIMARY KEY,
    id_conta INT REFERENCES contas(id_conta),
    tipo VARCHAR(255) NOT NULL,
    valor DECIMAL(10, 2) NOT NULL,
    data_transacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_conta_destino INT REFERENCES contas(id_conta) NULL);	
```

- `contas`: Armazena informações das contas (ID, nome, senha hasheada, saldo).
- `transacoes`: Registra histórico de operações, incluindo transferências com conta de destino.

---

### 🧑 Criar Conta

```java
public int criarConta(String nome, String senha) throws SQLException {
    String hashedSenha = BCrypt.hashpw(senha, BCrypt.gensalt());
    // Insere conta e retorna ID gerado
}
```

- Solicita nome e senha do titular.
- Gera hash da senha com jBCrypt.
- Retorna o ID da conta criada.

---

### 🔑 Autenticação

```java
private static Conta autenticarUsuario() {
    // Solicita ID e senha, verifica no banco
}
```

Valida o ID e a senha antes de permitir operações, retornando um objeto `Conta` ou `null`.

---

### 💸 Depósito, Saque e Transferência

```java
public void depositar(int idConta, double valor) throws SQLException {
    String sql = "UPDATE contas SET saldo = saldo + ? WHERE id_conta = ? AND EXISTS (SELECT 1 FROM contas WHERE id_conta = ?)";
    // Atualiza saldo e registra transação
}
public void sacar(int idConta, double valor) throws SQLException {
    String sql = "UPDATE contas SET saldo = saldo - ? WHERE id_conta = ? AND saldo >= ? AND EXISTS (SELECT 1 FROM contas WHERE id_conta = ?)";
    // Verifica saldo suficiente, atualiza e registra transação
}
public void transferir(int idContaOrigem, int idContaDestino, double valor) throws SQLException {
    // Realiza transferência atômica, atualiza saldos e registra transações
}
```

- **Depósito**: Adiciona valor ao saldo, validando a existência da conta.
- **Saque**: Subtrai valor, verificando saldo suficiente e existência da conta.
- **Transferência**: Transfere valor entre contas, com validação de saldo e contas, em uma transação atômica.

---

## 🛠️ Como Rodar

1. **Instale o PostgreSQL**:
   - Baixe e instale o PostgreSQL: [https://www.postgresql.org/download/](https://www.postgresql.org/download/).
   - Execute o script `src/main/resources/create_database.sql`:
     ```bash
     psql -U postgres -f src/main/resources/create_database.sql
     ```

2. **Atualize a Tabela de Transações (se necessário)**:
   - Se o banco já existe, adicione a coluna `id_conta_destino`:
     ```sql
     ALTER TABLE transacoes
     ADD COLUMN id_conta_destino INT REFERENCES contas(id_conta) NULL;
     ```

3. **Configure as Dependências**:
   - **Com Maven**:
     ```bash
     mvn clean install
     ```
   - **Sem Maven**:
     - Baixe:
       - PostgreSQL JDBC: [https://jdbc.postgresql.org/](https://jdbc.postgresql.org/)
       - jBCrypt: [https://repo1.maven.org/maven2/org/mindrot/jbcrypt/0.4/](https://repo1.maven.org/maven2/org/mindrot/jbcrypt/0.4/)
     - Adicione os JARs ao classpath.

4. **Configure o Banco**:
   - Em `ContaDAO.java`, atualize `DB_PASSWORD` com sua senha do PostgreSQL:
     ```java
     private static final String DB_PASSWORD = "sua_senha_aqui";
     ```

5. **Compile e Execute**:
   - **Com Maven**:
     ```bash
     cd banking-system
     mvn clean compile
     mvn exec:java -Dexec.mainClass="BankingSystem"
     ```
   - **Sem Maven**:
     ```bash
     javac -cp .:postgresql-42.7.3.jar:jbcrypt-0.4.jar src/main/java/*.java
     java -cp .:postgresql-42.7.3.jar:jbcrypt-0.4.jar:src/main/java BankingSystem
     ```
     *Nota*: No Windows, substitua `:` por `;`.

---

## 🎮 Como Utilizar

- **Iniciar**:
  - Execute o programa para ver o menu:
    ```
    === Sistema Bancário ===
    1. Criar Conta
    2. Depositar
    3. Sacar
    4. Consultar Saldo
    5. Transferir
    6. Sair
    Escolha uma opção:
    ```

- **Criar Conta**:
  - Escolha a opção 1.
  - Digite o nome do titular e uma senha.
  - Anote o ID retornado (ex.: "Conta criada com sucesso! ID da conta: 1").

- **Depositar/Sacar**:
  - Escolha a opção 2 ou 3.
  - Informe o ID da conta e a senha.
  - Digite o valor (ex.: `100.50`).
  - O sistema confirma ou exibe erros (ex.: "Saldo insuficiente").

- **Consultar Saldo**:
  - Escolha a opção 4.
  - Informe ID e senha.
  - Veja o nome do titular e o saldo (ex.: "Titular: João, Saldo: R$ 100.50").

- **Transferir**:
  - Escolha a opção 5.
  - Informe o ID da conta de origem e a senha.
  - Digite o ID da conta de destino e o valor.
  - O sistema confirma ou exibe erros (ex.: "Conta de destino não encontrada").

- **Sair**:
  - Escolha a opção 6 para encerrar.

---

## 📌 Observações

- O sistema usa **jBCrypt** para senhas seguras e **Logger** para erros.
- Validações evitam entradas inválidas (ex.: valores negativos).
- O histórico de transações é salvo, mas não há consulta direta nesta versão.
- Para expandir, considere adicionar uma interface gráfica ou consulta de transações.

---

## 📄 Licença

Este projeto é livre para fins educacionais. Se for usar ou modificar, sinta-se à vontade para dar os créditos. 😄
