var pendingEmail = '';

// PASO 1: Registrar
async function register() {
    var name            = document.getElementById('name').value.trim();
    var email           = document.getElementById('email').value.trim();
    var password        = document.getElementById('password').value;
    var passwordConfirm = document.getElementById('passwordConfirm').value;

    if (!name || !email || !password || !passwordConfirm) {
        showToast('Por favor completa todos los campos', 'error'); return;
    }
    if (password !== passwordConfirm) {
        showToast('Las contrasenas no coinciden', 'error'); return;
    }
    if (password.length < 8) {
        showToast('La contrasena debe tener al menos 8 caracteres', 'error'); return;
    }
    if (!/[a-zA-Z]/.test(password)) {
        showToast('La contrasena debe contener al menos una letra', 'error'); return;
    }
    if (!/[0-9]/.test(password)) {
        showToast('La contrasena debe contener al menos un numero', 'error'); return;
    }

    var btn     = document.getElementById('registerBtn');
    var btnText = document.getElementById('btnText');
    var spinner = document.getElementById('btnSpinner');
    btn.disabled = true;
    btnText.textContent = 'Enviando...';
    spinner.style.display = 'inline-block';

    try {
        var res  = await fetch(API + '/users/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password, role: 'USER' })
        });
        var text = await res.text();
        var data;
        try { data = JSON.parse(text); } catch(e) { data = null; }

        if (res.ok && data && data.requiresVerification) {
            pendingEmail = email;
            document.getElementById('emailDisplay').textContent = email;
            document.getElementById('stepRegister').style.display = 'none';
            document.getElementById('stepVerify').style.display = '';
            showToast('Codigo enviado a tu correo', 'success');
        } else {
            showToast(text || 'Error al crear la cuenta', 'error');
        }
    } catch(e) {
        showToast('Error al conectar con el servidor', 'error');
    } finally {
        btn.disabled = false;
        btnText.textContent = 'Crear Cuenta';
        spinner.style.display = 'none';
    }
}

// PASO 2: Verificar codigo
async function verifyCode() {
    var code = document.getElementById('verifyCode').value.trim();
    if (!code || code.length !== 6) {
        showToast('Ingresa el codigo de 6 digitos', 'error'); return;
    }

    var btn     = document.getElementById('verifyBtn');
    var btnText = document.getElementById('verifyBtnText');
    var spinner = document.getElementById('verifySpinner');
    btn.disabled = true;
    btnText.textContent = 'Verificando...';
    spinner.style.display = 'inline-block';

    try {
        var res  = await fetch(API + '/users/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: pendingEmail, code: code })
        });
        var text = await res.text();

        if (res.ok) {
            showToast('Correo verificado. Redirigiendo...', 'success');
            setTimeout(function() { window.location.href = 'login.html'; }, 1500);
        } else {
            showToast(text || 'Codigo incorrecto', 'error');
        }
    } catch(e) {
        showToast('Error de conexion', 'error');
    } finally {
        btn.disabled = false;
        btnText.textContent = 'Verificar Codigo';
        spinner.style.display = 'none';
    }
}

// Reenviar codigo
async function resendCode() {
    if (!pendingEmail) return;
    showToast('Reenviando codigo...', 'info');
    try {
        await fetch(API + '/users/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: pendingEmail, name: '', password: 'temporal1', role: 'PASAJERO' })
        });
        showToast('Codigo reenviado a ' + pendingEmail, 'success');
    } catch(e) {
        showToast('Error al reenviar', 'error');
    }
}

document.addEventListener('keydown', function(e) {
    if (e.key === 'Enter') {
        if (document.getElementById('stepVerify').style.display !== 'none') {
            verifyCode();
        } else {
            register();
        }
    }
});