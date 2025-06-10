window.addEventListener('DOMContentLoaded', () => {
  const overlay   = document.getElementById('modal-overlay');
  const modal     = document.getElementById('modal');
  const titleEl   = document.getElementById('modal-title');
  const form      = document.getElementById('modal-form');
  const bodyEl    = modal.querySelector('.modal-body');
  const btnClose  = document.getElementById('modal-close');
  const btnCancel = document.getElementById('modal-cancel');

  // Pega o modal de escolha já presente no HTML
  const escolhaModal = document.getElementById('modal-pagamento-escolha');
  escolhaModal.style.display = 'none'; // começa oculto

  overlay.style.display = 'none';
  modal.style.display = 'none';

  function openModal({ title, fields, onSubmit }) {
    titleEl.textContent = title;
    bodyEl.innerHTML = '';

    fields.forEach(f => {
      const label = document.createElement('label');
      label.textContent = f.label;

      const input = document.createElement('input');
      input.type = f.type || 'text';
      input.name = f.name;
      input.placeholder = f.placeholder || '';
      input.style.width = '100%';
      input.style.padding = '8px';
      input.style.marginTop = '4px';
      input.style.marginBottom = '12px';
      input.style.border = '1px solid #ccc';
      input.style.borderRadius = '4px';

      bodyEl.appendChild(label);
      bodyEl.appendChild(input);
    });

    form.onsubmit = async e => {
      e.preventDefault();
      const data = {};
      fields.forEach(f => {
        const v = form[f.name].value;
        data[f.name] = f.type === 'number' ? parseFloat(v) : v;
      });
      await onSubmit(data);
      closeModal();
      await loadDashboard();
    };

    overlay.style.display = 'flex';
    modal.style.display = 'flex';
  }

  function closeModal() {
    overlay.style.display = 'none';
    modal.style.display = 'none';
  }

  btnClose.addEventListener('click', closeModal);
  btnCancel.addEventListener('click', closeModal);
  overlay.addEventListener('click', closeModal);

  // 1) Depositar
  document.getElementById('btn-deposit').addEventListener('click', () => {
    openModal({
      title: 'Depositar Valor',
      fields: [{ name: 'valor', label: 'Valor (R$)', type: 'number', placeholder: 'Ex: 1000' }],
      onSubmit: data => fetch('http://localhost:4567/api/depositar', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({ valor: data.valor })
      })
    });
  });

  // 2) Transferir
  document.getElementById('btn-transfer').addEventListener('click', () => {
    openModal({
      title: 'Transferir',
      fields: [
        { name: 'destinatario', label: 'Email destinatário' },
        { name: 'valor', label: 'Valor (R$)', type: 'number' }
      ],
      onSubmit: async data => {
        const res = await fetch('http://localhost:4567/api/transferir', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
          body: JSON.stringify({
            destinatario: data.destinatario,
            valor: data.valor
          })
        });
        const json = await res.json();
        if (json.status !== 'sucesso') {
          alert(json.mensagem);
        }
      }
    });
  });

  // 3) Pagar Conta
  document.getElementById('btn-pay').addEventListener('click', () => {
    escolhaModal.style.display = 'flex';

    document.getElementById('cancelar-pagamento').onclick = () => {
      escolhaModal.style.display = 'none';
    };

    document.getElementById('escolher-cartao').onclick = () => {
      escolhaModal.style.display = 'none';
      openModal({
        title: 'Pagar no Cartão',
        fields: [
          { name: 'descricao', label: 'Descrição' },
          { name: 'valor', label: 'Valor (R$)', type: 'number' }
        ],
        onSubmit: async data => {
          const payload = {
            descricao: data.descricao,
            valor: data.valor,
            token: localStorage.getItem('token')
          };
          const res = await fetch('http://localhost:4567/api/pagarCartao', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: JSON.stringify(payload)
          });
          const json = await res.json();
          if (json.status !== 'ok') {
            alert(json.mensagem);
          }
        }
      });
    };

    document.getElementById('escolher-conta').onclick = () => {
      escolhaModal.style.display = 'none';
      openModal({
        title: 'Pagar com Saldo da Conta',
        fields: [
          { name: 'descricao', label: 'Descrição' },
          { name: 'valor', label: 'Valor (R$)', type: 'number' }
        ],
        onSubmit: async data => {
          const res = await fetch('http://localhost:4567/api/pagar', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: JSON.stringify({
              descricao: data.descricao,
              valor: data.valor
            })
          });
          const json = await res.json();
          if (json.status !== 'ok') {
            alert(json.mensagem);
          }
        }
      });
    };
  });

  // 4) Ajustar Limite do Cartão
  document.getElementById('btn-limit').addEventListener('click', () => {
    openModal({
      title: 'Ajustar Limite do Cartão',
      fields: [{ name: 'limite', label: 'Novo Limite (R$)', type: 'number', placeholder: 'Ex: 2000' }],
      onSubmit: async data => {
        const res = await fetch('http://localhost:4567/api/limite', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
          body: JSON.stringify({ limite: data.limite })
        });
        const json = await res.json();
        if (json.status !== 'ok') {
          alert(json.mensagem);
        }
      }
    });
  });

  // Dashboard
  async function loadDashboard() {
    const token = localStorage.getItem('token');
    if (!token) return window.location.href = 'index.html';

    const accountInfo = document.getElementById('account-info');
    const userName    = document.getElementById('user-name');
    const limitValue  = document.getElementById('limit-value');
    const balanceVal  = document.getElementById('balance-value');
    const txList      = document.getElementById('transaction-list');

    try {
      const res = await fetch('http://localhost:4567/api/user', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!res.ok) throw new Error();
      const u = await res.json();
      accountInfo.textContent = `${u.tipoConta} · ${u.status}`;
      userName.textContent    = u.nome;
      limitValue.textContent  = `R$ ${u.limiteCartao.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`;
    } catch {
      accountInfo.textContent = 'Erro ao carregar conta';
    }

    try {
      const res2 = await fetch('http://localhost:4567/api/dashboard', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const d = await res2.json();
      balanceVal.textContent = `R$ ${d.saldo.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`;
      txList.innerHTML = '';
      d.transacoes.slice().reverse().forEach(tx => {
        const li = document.createElement('li');
        li.className = 'transaction-item';
        const desc = document.createElement('span');
        desc.className = 'transaction-desc';
        desc.textContent = tx.descricao;
        const amt = document.createElement('span');
        amt.className = `transaction-amount ${tx.valor < 0 ? 'negative' : 'positive'}`;
        const sign = tx.valor < 0 ? '- ' : '+ ';
        amt.textContent = `${sign}R$ ${Math.abs(tx.valor).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`;
        li.append(desc, amt);
        txList.append(li);
      });
    } catch (e) {
      console.error('Erro ao carregar dashboard:', e);
    }
  }

  loadDashboard();
});
