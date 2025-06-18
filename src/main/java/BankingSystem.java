import java.sql.SQLException;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;

public class BankingSystem {
    private static final Logger LOGGER = Logger.getLogger(BankingSystem.class.getName());
    private static final Scanner scanner = new Scanner(System.in);
    private static final ContaDAO contaDAO = new ContaDAO();

    public static void main(String[] args) {
        try {
            while (true) {
                System.out.println("\n=== Sistema Bancário ===");
                System.out.println("1. Criar Conta");
                System.out.println("2. Depositar");
                System.out.println("3. Sacar");
                System.out.println("4. Consultar Saldo");
                System.out.println("5. Transferir");
                System.out.println("6. Sair");
                System.out.print(getMessage("menu.option"));

                try {
                    int escolha = Integer.parseInt(scanner.nextLine().trim());
                    switch (escolha) {
                        case 1:
                            criarConta();
                            break;
                        case 2:
                            realizarDeposito();
                            break;
                        case 3:
                            realizarSaque();
                            break;
                        case 4:
                            consultarSaldo();
                            break;
                        case 5:
                            realizarTransferencia();
                            break;
                        case 6:
                            System.out.println(getMessage("system.exit"));
                            return;
                        default:
                            System.out.println(getMessage("invalid.option"));
                    }
                } catch (NumberFormatException e) {
                    System.out.println(getMessage("invalid.input.number"));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro no sistema: ", e);
            System.out.println(getMessage("system.error"));
        } finally {
            contaDAO.closeConnection();
            scanner.close();
        }
    }

    private static void criarConta() {
        System.out.print(getMessage("prompt.name"));
        String nome = scanner.nextLine().trim();
        System.out.print(getMessage("prompt.password"));
        String senha = scanner.nextLine().trim();

        if (nome.isEmpty() || senha.isEmpty()) {
            System.out.println(getMessage("empty.name.or.password"));
            return;
        }

        try {
            int idConta = contaDAO.criarConta(nome, senha);
            System.out.printf(getMessage("account.created"), idConta);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erro ao criar conta: ", e);
            System.out.println(getMessage("account.create.error"));
        }
    }

    private static void realizarDeposito() {
        Conta conta = autenticarUsuario();
        if (conta == null) {
            System.out.println(getMessage("auth.failed"));
            return;
        }

        System.out.print(getMessage("prompt.deposit.amount"));
        try {
            double valor = Double.parseDouble(scanner.nextLine().trim());
            if (valor <= 0) {
                System.out.println(getMessage("invalid.deposit.amount"));
                return;
            }
            contaDAO.depositar(conta.getIdConta(), valor);
            System.out.println(getMessage("deposit.success"));
        } catch (NumberFormatException e) {
            System.out.println(getMessage("invalid.input.number"));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erro ao realizar depósito: ", e);
            System.out.println(getMessage("deposit.error"));
        }
    }

    private static void realizarSaque() {
        Conta conta = autenticarUsuario();
        if (conta == null) {
            System.out.println(getMessage("auth.failed"));
            return;
        }

        System.out.print(getMessage("prompt.withdraw.amount"));
        try {
            double valor = Double.parseDouble(scanner.nextLine().trim());
            if (valor <= 0) {
                System.out.println(getMessage("invalid.withdraw.amount"));
                return;
            }
            contaDAO.sacar(conta.getIdConta(), valor);
            System.out.println(getMessage("withdraw.success"));
        } catch (NumberFormatException e) {
            System.out.println(getMessage("invalid.input.number"));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erro ao realizar saque: ", e);
            System.out.println(getMessage("withdraw.error"));
        }
    }

    private static void consultarSaldo() {
        Conta conta = autenticarUsuario();
        if (conta == null) {
            System.out.println(getMessage("auth.failed"));
            return;
        }

        try {
            System.out.printf(getMessage("balance.info"), conta.getNomeTitular(), conta.getSaldo());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erro ao consultar saldo: ", e);
            System.out.println(getMessage("balance.error"));
        }
    }

    private static void realizarTransferencia() {
        Conta contaOrigem = autenticarUsuario();
        if (contaOrigem == null) {
            System.out.println(getMessage("auth.failed"));
            return;
        }

        System.out.print(getMessage("prompt.transfer.account"));
        try {
            int idContaDestino = Integer.parseInt(scanner.nextLine().trim());
            if (idContaDestino == contaOrigem.getIdConta()) {
                System.out.println(getMessage("invalid.transfer.self"));
                return;
            }
            System.out.print(getMessage("prompt.transfer.amount"));
            double valor = Double.parseDouble(scanner.nextLine().trim());
            if (valor <= 0) {
                System.out.println(getMessage("invalid.transfer.amount"));
                return;
            }
            contaDAO.transferir(contaOrigem.getIdConta(), idContaDestino, valor);
            System.out.println(getMessage("transfer.success"));
        } catch (NumberFormatException e) {
            System.out.println(getMessage("invalid.input.number"));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erro ao realizar transferência: ", e);
            System.out.println(getMessage("transfer.error"));
        }
    }

    private static Conta autenticarUsuario() {
        System.out.print(getMessage("prompt.account.id"));
        try {
            int idConta = Integer.parseInt(scanner.nextLine().trim());
            System.out.print(getMessage("prompt.password"));
            String senha = scanner.nextLine().trim();
            return contaDAO.autenticarConta(idConta, senha);
        } catch (NumberFormatException e) {
            System.out.println(getMessage("invalid.input.number"));
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getMessage(String key) {
        switch (key) {
            case "menu.option": return "Escolha uma opção: ";
            case "system.exit": return "Saindo do sistema...";
            case "invalid.option": return "Opção inválida. Tente novamente.";
            case "invalid.input.number": return "Entrada inválida. Digite um número válido.";
            case "system.error": return "Ocorreu um erro inesperado. Tente novamente mais tarde.";
            case "prompt.name": return "Digite o nome do titular: ";
            case "prompt.password": return "Digite a senha: ";
            case "empty.name.or.password": return "Nome e senha não podem ser vazios.";
            case "account.created": return "Conta criada com sucesso! ID da conta: %d%n";
            case "account.create.error": return "Erro ao criar conta. Tente novamente.";
            case "auth.failed": return "ID ou senha inválidos.";
            case "prompt.deposit.amount": return "Digite o valor do depósito: ";
            case "invalid.deposit.amount": return "Valor inválido para depósito.";
            case "deposit.success": return "Depósito realizado com sucesso!";
            case "deposit.error": return "Erro ao realizar depósito. Tente novamente.";
            case "prompt.withdraw.amount": return "Digite o valor do saque: ";
            case "invalid.withdraw.amount": return "Valor inválido para saque.";
            case "withdraw.success": return "Saque realizado com sucesso!";
            case "withdraw.error": return "Erro ao realizar saque. Tente novamente.";
            case "prompt.account.id": return "Digite o ID da conta: ";
            case "balance.info": return "Titular: %s, Saldo: R$ %.2f%n";
            case "balance.error": return "Erro ao consultar saldo. Tente novamente.";
            case "prompt.transfer.account": return "Digite o ID da conta de destino: ";
            case "invalid.transfer.self": return "Não é possível transferir para a mesma conta.";
            case "prompt.transfer.amount": return "Digite o valor da transferência: ";
            case "invalid.transfer.amount": return "Valor inválido para transferência.";
            case "transfer.success": return "Transferência realizada com sucesso!";
            case "transfer.error": return "Erro ao realizar transferência. Tente novamente.";
            default: return key;
        }
    }
}