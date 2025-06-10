package app;

import static spark.Spark.*;
import com.google.gson.Gson;
import model.Usuario;
import model.GerenciadorUsuarios;
import model.CartaoException;

import java.util.*;

public class App {

    static class UsuarioDTO {
        String nome, email, senha, cpf, dataNascimento;
    }

    static class Resposta {
        String status, mensagem;
        public Resposta() {}
        public Resposta(String status, String mensagem) {
            this.status = status;
            this.mensagem = mensagem;
        }
    }

    public static void main(String[] args) {
        port(4567);
        GerenciadorUsuarios ger = new GerenciadorUsuarios();
        Gson gson = new Gson();

        // CORS (libera todas as origens e métodos)
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            res.header("Access-Control-Allow-Headers", "*");
        });
        options("/*", (req, res) -> "");

        // Health check
        get("/ping", (req, res) -> "pong");

        // --- LOGIN (SEM autenticação prévia) ---
        post("/api/login", (req, res) -> {
            res.type("application/json");
            UsuarioDTO d = gson.fromJson(req.body(), UsuarioDTO.class);
            Usuario u = ger.fazerLogin(d.email, d.senha);
            if (u != null) {
                String token = UUID.randomUUID().toString();
                ger.salvarToken(u.getEmail(), token);
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("status", "ok");
                out.put("mensagem", "Login bem-sucedido!");
                out.put("token", token);
                out.put("nome", u.getNome());
                return gson.toJson(out);
            } else {
                res.status(401);
                return gson.toJson(new Resposta("erro", "Email ou senha inválidos."));
            }
        });

        // --- CADASTRO (SEM autenticação prévia) ---
        post("/api/cadastro", (req, res) -> {
            res.type("application/json");
            UsuarioDTO d = gson.fromJson(req.body(), UsuarioDTO.class);
            if (ger.usuarioExiste(d.email)) {
                res.status(400);
                return gson.toJson(new Resposta("erro", "Email já cadastrado."));
            }
            boolean ok = ger.cadastrarUsuario(d.nome, d.email, d.senha, d.cpf, d.dataNascimento);
            if (ok) {
                return gson.toJson(new Resposta("ok", "Usuário cadastrado com sucesso!"));
            } else {
                res.status(500);
                return gson.toJson(new Resposta("erro", "Erro ao cadastrar usuário."));
            }
        });

        // --- DEPÓSITO (requisição autenticada) ---
        post("/api/depositar", (req, res) -> executa(req, res, ger, body -> {
            double valor = ((Number) body.get("valor")).doubleValue();
            String email = body.get("email").toString();
            ger.depositar(email, valor);
        }, gson));

        // --- TRANSFERÊNCIA (requisição autenticada) ---
        post("/api/transferir", (req, res) -> executa(req, res, ger, body -> {
            String origemEmail = body.get("email").toString();
            String destinatario = body.get("destinatario").toString();
            double valor = ((Number) body.get("valor")).doubleValue();
            boolean ok = ger.transferir(origemEmail, destinatario, valor);
            if (!ok) halt(400, gson.toJson(new Resposta("erro", "Saldo insuficiente ou destinatário inválido")));
        }, gson));

        // --- PAGAR COM SALDO DA CONTA (requisição autenticada) ---
        post("/api/pagar", (req, res) -> executa(req, res, ger, body -> {
            String email = body.get("email").toString();
            String desc = body.get("descricao").toString();
            double valor = ((Number) body.get("valor")).doubleValue();
            boolean ok = ger.pagarConta(email, desc, valor);
            if (!ok) halt(400, gson.toJson(new Resposta("erro", "Saldo insuficiente")));
        }, gson));

        // --- AJUSTAR LIMITE DO CARTÃO (requisição autenticada) ---
        post("/api/limite", (req, res) -> executa(req, res, ger, body -> {
            String email = body.get("email").toString();
            double limite = ((Number) body.get("limite")).doubleValue();
            ger.ajustarLimiteCartao(email, limite);
        }, gson));

        // --- PAGAR VIA CARTÃO (requisição autenticada) ---
        post("/api/pagarCartao", (req, res) -> {
            res.type("application/json");
            // Autenticação manual (sem usar executa helper)
            String auth = req.headers("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                res.status(401);
                return gson.toJson(new Resposta("erro", "Unauthorized"));
            }
            String token = auth.substring(7);
            Usuario u = ger.buscarUsuarioPorToken(token);
            if (u == null) {
                res.status(401);
                return gson.toJson(new Resposta("erro", "Unauthorized"));
            }

            // Desserializa JSON recebido
            Map<String, Object> body = gson.fromJson(req.body(), Map.class);
            String desc = body.get("descricao").toString();
            double valor = ((Number) body.get("valor")).doubleValue();

            try {
                // Chama o método que debita do limite do cartão
                ger.pagarCartao(token, desc, valor);
                return gson.toJson(new Resposta("ok", "Sucesso"));
            } catch (CartaoException e) {
                res.status(400);
                return gson.toJson(new Resposta("erro", e.getMessage()));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(new Resposta("erro", "Erro interno no servidor."));
            }
        });

        // --- OBTER DADOS DO USUÁRIO (requisição autenticada) ---
        get("/api/user", (req, res) -> executaGet(req, res, ger, u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nome", u.getNome());
            m.put("tipoConta", "Conta Corrente");
            m.put("status", "Ativa");
            m.put("limiteCartao", u.getLimiteCartao());
            return m;
        }, gson));

        // --- OBTER DASHBOARD (saldo + transações, requisição autenticada) ---
        get("/api/dashboard", (req, res) -> executaGet(req, res, ger, u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("saldo", u.getSaldo());
            m.put("transacoes", u.getTransacoes());
            return m;
        }, gson));

        System.out.println("Servidor Spark rodando na porta 4567...");
    }

    /**
     * Helper para rotas POST autenticadas que recebem um JSON no corpo e apenas
     * adicionam "email" ao mapa antes de chamar action.accept(body).
     */
    private static Object executa(spark.Request req, spark.Response res,
                                  GerenciadorUsuarios ger,
                                  java.util.function.Consumer<Map<String, Object>> action,
                                  Gson gson) {
        res.type("application/json");
        String auth = req.headers("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            halt(401, gson.toJson(new Resposta("erro", "Unauthorized")));
        }
        String token = auth.substring(7);
        Usuario u = ger.buscarUsuarioPorToken(token);
        if (u == null) {
            halt(401, gson.toJson(new Resposta("erro", "Unauthorized")));
        }
        Map<String, Object> body = gson.fromJson(req.body(), Map.class);
        body.put("email", u.getEmail());
        action.accept(body);
        return gson.toJson(new Resposta("ok", "Sucesso"));
    }

    /**
     * Helper para rotas GET autenticadas que devolvem um objeto baseado no usuário.
     */
    private static Object executaGet(spark.Request req, spark.Response res,
                                     GerenciadorUsuarios ger,
                                     java.util.function.Function<Usuario, Object> fn,
                                     Gson gson) {
        res.type("application/json");
        String auth = req.headers("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            halt(401, gson.toJson(new Resposta("erro", "Unauthorized")));
        }
        String token = auth.substring(7);
        Usuario u = ger.buscarUsuarioPorToken(token);
        if (u == null) {
            halt(401, gson.toJson(new Resposta("erro", "Unauthorized")));
        }
        return gson.toJson(fn.apply(u));
    }
}
