async function login() {
  const email    = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value;

  if (!email || !password) {
    showToast('Por favor completa todos los campos', 'error');
    return;
  }

  const btn     = document.getElementById('loginBtn');
  const btnText = document.getElementById('btnText');
  const spinner = document.getElementById('btnSpinner');
  btn.disabled  = true;
  btnText.textContent = 'Verificando...';
  spinner.style.display = 'inline-block';

  try {
    const res  = await fetch(`${API}/users/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    const text = await res.text();

    let user;
    try { user = JSON.parse(text); } catch { user = null; }

    if (user && user.id) {
      const role = user.role.toUpperCase();
      sessionStorage.setItem('user', JSON.stringify(user));
      sessionStorage.setItem('role', role);
      showToast(`¡Bienvenido/a, ${user.name}!`, 'success');
      setTimeout(() => {
        if (role === 'PASAJERO') window.location.href = 'routes-view.html';
        else window.location.href = 'routes.html';
      }, 800);
    } else {
      showToast(text || 'Credenciales incorrectas', 'error');
    }
  } catch {
    showToast('Error al conectar con el servidor', 'error');
  } finally {
    btn.disabled  = false;
    btnText.textContent = 'Ingresar al sistema';
    spinner.style.display = 'none';
  }
}

// Enter key
document.addEventListener('keydown', e => { if (e.key === 'Enter') login(); });
