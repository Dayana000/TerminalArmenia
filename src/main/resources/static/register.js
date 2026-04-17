async function register() {
  const name     = document.getElementById('name').value.trim();
  const email    = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value;
  const role     = document.getElementById('role').value;

  if (!name || !email || !password || !role) {
    showToast('Por favor completa todos los campos', 'error'); return;
  }
  if (password.length < 6) {
    showToast('La contraseña debe tener al menos 6 caracteres', 'error'); return;
  }

  const btn     = document.getElementById('registerBtn');
  const btnText = document.getElementById('btnText');
  const spinner = document.getElementById('btnSpinner');
  btn.disabled  = true;
  btnText.textContent = 'Registrando...';
  spinner.style.display = 'inline-block';

  try {
    const res  = await fetch(`${API}/users/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, password, role })
    });
    const text = await res.text();

    let user;
    try { user = JSON.parse(text); } catch { user = null; }

    if (user && user.id) {
      showToast('¡Cuenta creada correctamente!', 'success');
      setTimeout(() => window.location.href = 'login.html', 1000);
    } else {
      showToast(text || 'Error al crear la cuenta', 'error');
    }
  } catch {
    showToast('Error al conectar con el servidor', 'error');
  } finally {
    btn.disabled = false;
    btnText.textContent = 'Crear Cuenta';
    spinner.style.display = 'none';
  }
}

document.addEventListener('keydown', e => { if (e.key === 'Enter') register(); });
