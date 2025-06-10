package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um usuário, com saldo, histórico de transações e limite de cartão.
 */
public class Usuario {
    private String nome;
    private String email;
    private String senha;
    private String cpf;
    private String dataNascimento;
    private String token;
    private double saldo;
    private double limiteCartao;
    private List<Transacao> transacoes;

    // Construtor vazio para desserialização
    public Usuario() {
        this.saldo        = 0.0;
        this.limiteCartao = 1000.0;
        this.transacoes   = new ArrayList<>();
    }

    public Usuario(String nome, String email, String senha, String cpf, String dataNascimento) {
        this.nome           = nome;
        this.email          = email;
        this.senha          = senha;
        this.cpf            = cpf;
        this.dataNascimento = dataNascimento;
        this.token          = null;
        this.saldo          = 0.0;
        this.limiteCartao   = 1000.0;
        this.transacoes     = new ArrayList<>();
    }

    public String getNome()            { return nome; }
    public void setNome(String nome)   { this.nome = nome; }

    public String getEmail()           { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha()           { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getCpf()             { return cpf; }
    public void setCpf(String cpf)     { this.cpf = cpf; }

    public String getDataNascimento()  { return dataNascimento; }
    public void setDataNascimento(String dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getToken()           { return token; }
    public void setToken(String token) { this.token = token; }

    public double getSaldo()           { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    public double getLimiteCartao()                 { return limiteCartao; }
    public void setLimiteCartao(double limiteCartao){ this.limiteCartao = limiteCartao; }

    public List<Transacao> getTransacoes()          { return transacoes; }
    public void setTransacoes(List<Transacao> tx)   { this.transacoes = tx; }

    /**
     * Adiciona uma transação ao histórico do usuário.
     */
    public void adicionarTransacao(Transacao t) {
        this.transacoes.add(t);
    }
}
