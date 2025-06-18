public class Conta {
    private final int idConta;
    private final String nomeTitular;
    private final double saldo;

    public Conta(int idConta, String nomeTitular, double saldo) {
        this.idConta = idConta;
        this.nomeTitular = nomeTitular != null ? nomeTitular : "";
        this.saldo = saldo;
    }

    public int getIdConta() {
        return idConta;
    }

    public String getNomeTitular() {
        return nomeTitular;
    }

    public double getSaldo() {
        return saldo;
    }
}