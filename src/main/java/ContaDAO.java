import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ContaDAO {
    private static final Logger LOGGER = Logger.getLogger(ContaDAO.class.getName());
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/bank_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "ifg";
    private Connection conn;

    public ContaDAO() {
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao conectar ao banco de dados: ", e);
            throw new RuntimeException("Falha na conexão com o banco de dados.");
        }
    }

    public int criarConta(String nome, String senha) throws SQLException {
        String hashedSenha = BCrypt.hashpw(senha, BCrypt.gensalt());
        String sql = "INSERT INTO contas (nome_titular, senha, saldo) VALUES (?, ?, 0.00) RETURNING id_conta";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nome);
            pstmt.setString(2, hashedSenha);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int idConta = rs.getInt("id_conta");
                registrarTransacao(idConta, "CRIACAO", 0.00, null);
                return idConta;
            }
            throw new SQLException("Falha ao recuperar ID da conta criada.");
        }
    }

    public Conta autenticarConta(int idConta, String senha) throws SQLException {
        String sql = "SELECT id_conta, nome_titular, senha, saldo FROM contas WHERE id_conta = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idConta);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && BCrypt.checkpw(senha, rs.getString("senha"))) {
                return new Conta(rs.getInt("id_conta"), rs.getString("nome_titular"), rs.getDouble("saldo"));
            }
            return null;
        }
    }

    public void depositar(int idConta, double valor) throws SQLException {
        String sql = "UPDATE contas SET saldo = saldo + ? WHERE id_conta = ? AND EXISTS (SELECT 1 FROM contas WHERE id_conta = ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, valor);
            pstmt.setInt(2, idConta);
            pstmt.setInt(3, idConta);
            int rows = pstmt.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("Conta não encontrada.");
            }
            registrarTransacao(idConta, "DEPOSITO", valor, null);
        }
    }

    public void sacar(int idConta, double valor) throws SQLException {
        String sql = "UPDATE contas SET saldo = saldo - ? WHERE id_conta = ? AND saldo >= ? AND EXISTS (SELECT 1 FROM contas WHERE id_conta = ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, valor);
            pstmt.setInt(2, idConta);
            pstmt.setDouble(3, valor);
            pstmt.setInt(4, idConta);
            int rows = pstmt.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("Conta não encontrada ou saldo insuficiente.");
            }
            registrarTransacao(idConta, "SAQUE", valor, null);
        }
    }

    public void transferir(int idContaOrigem, int idContaDestino, double valor) throws SQLException {
        conn.setAutoCommit(false);
        try {
            String sqlVerificaOrigem = "SELECT saldo FROM contas WHERE id_conta = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlVerificaOrigem)) {
                pstmt.setInt(1, idContaOrigem);
                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    throw new IllegalArgumentException("Conta de origem não encontrada.");
                }
                if (rs.getDouble("saldo") < valor) {
                    throw new IllegalArgumentException("Saldo insuficiente na conta de origem.");
                }
            }

            String sqlVerificaDestino = "SELECT 1 FROM contas WHERE id_conta = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlVerificaDestino)) {
                pstmt.setInt(1, idContaDestino);
                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    throw new IllegalArgumentException("Conta de destino não encontrada.");
                }
            }

            String sqlSaque = "UPDATE contas SET saldo = saldo - ? WHERE id_conta = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlSaque)) {
                pstmt.setDouble(1, valor);
                pstmt.setInt(2, idContaOrigem);
                pstmt.executeUpdate();
            }

            String sqlDeposito = "UPDATE contas SET saldo = saldo + ? WHERE id_conta = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDeposito)) {
                pstmt.setDouble(1, valor);
                pstmt.setInt(2, idContaDestino);
                pstmt.executeUpdate();
            }

            registrarTransacao(idContaOrigem, "TRANSFERENCIA_SAIDA", valor, idContaDestino);
            registrarTransacao(idContaDestino, "TRANSFERENCIA_ENTRADA", valor, idContaOrigem);

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            LOGGER.log(Level.WARNING, "Erro ao realizar transferência: ", e);
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private void registrarTransacao(int idConta, String tipo, double valor, Integer idContaDestino) throws SQLException {
        String sql = "INSERT INTO transacoes (id_conta, tipo, valor, id_conta_destino) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idConta);
            pstmt.setString(2, tipo);
            pstmt.setDouble(3, valor);
            if (idContaDestino != null) {
                pstmt.setInt(4, idContaDestino);
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            pstmt.executeUpdate();
        }
    }

    public void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erro ao fechar conexão: ", e);
        }
    }
}