document.addEventListener("DOMContentLoaded", () => {
  const loginForm     = document.getElementById("loginForm");
  const cadastroForm  = document.getElementById("cadastroForm");
  const signUpButton  = document.getElementById("signUp");
  const signInButton  = document.getElementById("signIn");
  const container     = document.getElementById("container");
  const loginMsg      = document.getElementById("login-msg");
  const cadastroMsg   = document.getElementById("cadastro-msg");

  if (!loginForm && !cadastroForm) return;

  // Troca de painel
  signUpButton?.addEventListener("click", () => {
    container?.classList.add("right-panel-active");
    limparMensagens();
  });
  signInButton?.addEventListener("click", () => {
    container?.classList.remove("right-panel-active");
    limparMensagens();
  });

  // limpa avisos
  function limparMensagens() {
    loginMsg?.classList.remove("sucesso","erro","visivel");
    loginMsg && (loginMsg.textContent = "");
    cadastroMsg?.classList.remove("sucesso","erro","visivel");
    cadastroMsg && (cadastroMsg.textContent = "");
  }

  // mostra erro ou sucesso
  function mostrarMensagem(el, txt, tipo = "erro") {
    if (!el) return;
    el.textContent = txt;
    el.classList.remove("sucesso","erro","visivel");
    el.classList.add(tipo === "sucesso" ? "sucesso" : "erro");
    void el.offsetWidth;
    el.classList.add("visivel");
  }

  // desativa botão e troca texto
  function desativarBotao(btn, ativo = true, tmp = "") {
    if (!btn) return;
    btn.disabled = ativo;
    if (ativo) {
      btn.dataset.originalHtml = btn.dataset.originalHtml || btn.innerHTML;
      btn.innerHTML = tmp;
    } else {
      btn.innerHTML = btn.dataset.originalHtml;
      delete btn.dataset.originalHtml;
    }
  }

  // LOGIN
  loginForm?.addEventListener("submit", async e => {
    e.preventDefault();
    const btn = loginForm.querySelector("button");
    desativarBotao(btn, true, "Entrando...");
    try {
      const res = await fetch("http://localhost:4567/api/login", {
        method: "POST",
        headers: {"Content-Type":"application/json"},
        body: JSON.stringify({
          email: loginForm.email.value.trim(),
          senha: loginForm.senha.value.trim()
        })
      });
      const data = await res.json();
      if (data.status === "ok") {
        localStorage.setItem("token", data.token);
        localStorage.setItem("nomeUsuario", data.nome);
        document.body.classList.add("slide-out-left");
        setTimeout(() => window.location.href="dashboard.html", 500);
      } else {
        mostrarMensagem(loginMsg, data.mensagem);
      }
    } catch {
      mostrarMensagem(loginMsg, "Erro ao conectar com o servidor.");
    } finally {
      desativarBotao(btn, false);
    }
  });

  // CADASTRO
  cadastroForm?.addEventListener("submit", async e => {
    e.preventDefault();
    const btn = cadastroForm.querySelector("button");

    // coleta e valida
    const nome  = cadastroForm.nome.value.trim();
    const dataN = new Date(cadastroForm.dataNascimento.value);
    const hoje  = new Date();
    const idade = hoje.getFullYear() - dataN.getFullYear();
    const passou = hoje.getMonth() > dataN.getMonth() ||
                  (hoje.getMonth() === dataN.getMonth() && hoje.getDate() >= dataN.getDate());
    const idadeFinal = passou ? idade : idade-1;
    const email     = cadastroForm.email.value.trim();
    const cpf       = cadastroForm.cpf.value.replace(/\D/g,"");
    const senha     = cadastroForm.senha.value.trim();
    const confirm   = cadastroForm.confirmarSenha.value.trim();

    if (!/^[A-Za-zÀ-ú\s]+$/.test(nome))       return mostrarMensagem(cadastroMsg, "Nome inválido.");
    if (idadeFinal < 18)                      return mostrarMensagem(cadastroMsg, "É necessário ter 18 anos.");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return mostrarMensagem(cadastroMsg, "Email inválido.");
    if (cpf.length !== 11)                    return mostrarMensagem(cadastroMsg, "CPF inválido.");
    if (senha.length < 6)                     return mostrarMensagem(cadastroMsg, "Senha muito curta.");
    if (senha !== confirm)                    return mostrarMensagem(cadastroMsg, "Senhas não coincidem.");

    desativarBotao(btn, true, "Cadastrando...");
    try {
      const res = await fetch("http://localhost:4567/api/cadastro", {
        method: "POST",
        headers: {"Content-Type":"application/json"},
        body: JSON.stringify({ nome, dataNascimento:dataN, cpf, email, senha })
      });
      const data = await res.json();
      mostrarMensagem(cadastroMsg, data.mensagem, data.status==="ok"?"sucesso":"erro");
      if (data.status==="ok") {
        cadastroForm.reset();
        setTimeout(() => container.classList.remove("right-panel-active"), 1500);
      }
    } catch {
      mostrarMensagem(cadastroMsg, "Erro ao conectar com o servidor.");
    } finally {
      desativarBotao(btn, false);
    }
  });

});
