package model;

/**
 * Representa uma transação financeira.
 */
public class Transacao {
    private String descricao;
    private double valor;

    // Construtor vazio para desserialização
    public Transacao() {}

    public Transacao(String descricao, double valor) {
        this.descricao = descricao;
        this.valor = valor;
    }

    public String getDescricao() { return descricao; }
    public double getValor()      { return valor;    }
}
