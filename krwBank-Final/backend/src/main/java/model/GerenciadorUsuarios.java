package model;

import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Gerencia usuários, persistindo em usuarios.json no diretório de trabalho.
 * Inclui operações de login, cadastro, depósito, transferência, pagamento (saldo ou cartão) e ajuste de limite.
 */
public class GerenciadorUsuarios {
    private static final String ARQUIVO_USUARIOS = "usuarios.json";
    private final Gson gson = new Gson();
    private List<Usuario> usuarios;

    public GerenciadorUsuarios() {
        this.usuarios = carregarUsuarios();
        System.out.println("Usuários carregados: " + usuarios.size());
        usuarios.forEach(u -> System.out.println("- " + u.getEmail() + " / senha=" + u.getSenha()));
    }

    public boolean usuarioExiste(String email) {
        return buscarUsuarioPorEmail(email) != null;
    }

    public boolean cadastrarUsuario(String nome, String email, String senha, String cpf, String dataNascimento) {
        if (usuarioExiste(email)) return false;
        Usuario novo = new Usuario(nome, email, senha, cpf, dataNascimento);
        usuarios.add(novo);
        salvarUsuarios();
        return true;
    }

    /**
     * Tenta fazer login: retorna o objeto Usuario se e-mail e senha baterem, senão retorna null.
     */
    public Usuario fazerLogin(String email, String senha) {
        return usuarios.stream()
                .filter(u -> u.getEmail().equals(email) && u.getSenha().equals(senha))
                .findFirst().orElse(null);
    }

    /**
     * Armazena o token (por exemplo, JWT) no usuário identificado pelo e-mail.
     * Se não encontrar o usuário, não faz nada.
     */
    public void salvarToken(String email, String token) {
        Usuario u = buscarUsuarioPorEmail(email);
        if (u != null) {
            u.setToken(token);
            salvarUsuarios();
        }
    }

    /**
     * Retorna o usuário correspondente ao token passado (null se não achar).
     */
    public Usuario buscarUsuarioPorToken(String token) {
        return usuarios.stream()
                .filter(u -> token != null && token.equals(u.getToken()))
                .findFirst().orElse(null);
    }

    /**
     * Retorna o usuário correspondente ao e-mail (null se não achar).
     */
    public Usuario buscarUsuarioPorEmail(String email) {
        return usuarios.stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst().orElse(null);
    }

    /** Ajusta o limite do cartão do usuário identificado pelo e-mail. */
    public void ajustarLimiteCartao(String email, double novoLimite) {
        Usuario u = buscarUsuarioPorEmail(email);
        if (u == null) return;
        u.setLimiteCartao(novoLimite);
        salvarUsuarios();
    }

    public double getSaldo(Usuario u) {
        return u.getSaldo();
    }

    public List<Transacao> getTransacoesRecentes(Usuario u) {
        return Collections.unmodifiableList(u.getTransacoes());
    }

    /** Deposita no saldo da conta (não afeta limite do cartão). */
    public void depositar(String email, double valor) {
        Usuario u = buscarUsuarioPorEmail(email);
        if (u == null) return;
        u.setSaldo(u.getSaldo() + valor);
        u.getTransacoes().add(new Transacao("Depósito", valor));
        salvarUsuarios();
    }

    /**
     * Transfere valor de um usuário para outro (saldo da conta).
     * Retorna true se houver saldo suficiente e a transferência for feita; false caso contrário.
     */
    public boolean transferir(String fromEmail, String toEmail, double valor) {
        Usuario origem  = buscarUsuarioPorEmail(fromEmail);
        Usuario destino = buscarUsuarioPorEmail(toEmail);
        if (origem == null || destino == null || origem.getSaldo() < valor) return false;
        origem.setSaldo(origem.getSaldo() - valor);
        destino.setSaldo(destino.getSaldo() + valor);

        origem.getTransacoes().add(new Transacao("Transferência para " + toEmail, -valor));
        destino.getTransacoes().add(new Transacao("Transferência recebida de " + fromEmail, valor));
        salvarUsuarios();
        return true;
    }

    /**
     * Paga uma conta usando o saldo da conta (só debita de u.getSaldo()).
     * Retorna true se saldo suficiente; false caso contrário.
     */
    public boolean pagarConta(String email, String descricao, double valor) {
        Usuario u = buscarUsuarioPorEmail(email);
        if (u == null || u.getSaldo() < valor) return false;
        u.setSaldo(u.getSaldo() - valor);
        u.getTransacoes().add(new Transacao(descricao, -valor));
        salvarUsuarios();
        return true;
    }

    /**
     * *NOVO* — Paga uma conta usando o limite do cartão (não afeta saldo).
     * Localiza o usuário pelo token: se token inválido, lança CartaoException.
     * Se não houver limite suficiente, também lança CartaoException.
     * Em caso de sucesso, debita do limite e registra transação (valor negativo).
     */
    public void pagarCartao(String token, String descricao, double valor) throws CartaoException {
        // 1) Busca usuário pelo token
        Usuario u = buscarUsuarioPorToken(token);
        if (u == null) {
            throw new CartaoException("Usuário não autenticado (token inválido).");
        }

        // 2) Valida valor
        if (valor <= 0) {
            throw new CartaoException("Valor inválido para pagamento.");
        }

        // 3) Verifica limite
        double limiteAtual = u.getLimiteCartao();
        if (limiteAtual < valor) {
            throw new CartaoException("Limite do cartão insuficiente.");
        }

        // 4) Debita do limite
        u.setLimiteCartao(limiteAtual - valor);

        // 5) Registra no histórico de transações (valor negativo para indicar gasto no cartão)
        Transacao tx = new Transacao(descricao, -valor);
        u.adicionarTransacao(tx);

        // 6) Persiste tudo no JSON
        salvarUsuarios();
    }

    /* ========== Métodos auxiliares de IO (JSON) ========== */

    private List<Usuario> carregarUsuarios() {
        File file = new File(ARQUIVO_USUARIOS);
        try {
            if (!file.exists()) {
                file.createNewFile();
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("[]");
                }
            }
            try (FileReader reader = new FileReader(file)) {
                Type listType = new TypeToken<List<Usuario>>() {}.getType();
                List<Usuario> list = gson.fromJson(reader, listType);
                return list != null ? list : new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void salvarUsuarios() {
        try (FileWriter writer = new FileWriter(ARQUIVO_USUARIOS)) {
            gson.toJson(usuarios, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
