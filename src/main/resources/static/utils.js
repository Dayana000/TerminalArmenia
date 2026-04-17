// ─── CONFIGURACIÓN CENTRAL ────────────────────────────────────
const API = 'http://terminal-env.eba-mtft8yu7.us-east-2.elasticbeanstalk.com';

// ─── TOAST NOTIFICATIONS ─────────────────────────────────────
function showToast(message, type = 'info', duration = 3500) {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const icons = { success: '✅', error: '❌', info: '🔔' };
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<span>${icons[type]}</span><span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.animation = 'fadeIn 0.3s ease reverse';
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

// ─── SESIÓN ───────────────────────────────────────────────────
function getUser()  { return JSON.parse(sessionStorage.getItem('user') || 'null'); }
function getRole()  { return sessionStorage.getItem('role') || ''; }
function getUserId(){ const u = getUser(); return u ? u.id : null; }

function requireAuth(allowedRoles = []) {
  const role = getRole();
  if (!role) { window.location.href = 'login.html'; return false; }
  if (allowedRoles.length && !allowedRoles.includes(role)) {
    window.location.href = 'login.html';
    return false;
  }
  return true;
}

function logout() {
  sessionStorage.clear();
  window.location.href = 'login.html';
}

// ─── HEADER DINÁMICO ──────────────────────────────────────────
function renderHeader(subtitle = '') {
  const user = getUser();
  const role = getRole();
  const roleBadge = `<span class="badge badge-${role.toLowerCase()}">${role}</span>`;

  return `
    <header class="header">
      <div class="header-logo">🚌</div>
      <div class="header-text">
        <h1>Terminal Armenia</h1>
        <p>${subtitle || 'Sistema de Gestión de Tiquetes'}</p>
      </div>
      <div class="header-spacer"></div>
      <div class="header-user">
        ${roleBadge}
        <span>${user ? user.name : ''}</span>
        <button class="btn-logout" onclick="logout()">Cerrar sesión</button>
      </div>
    </header>
  `;
}

// ─── FORMAT ───────────────────────────────────────────────────
function formatPrice(n) {
  return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(n);
}

function formatDate(str) {
  if (!str) return '—';
  const d = new Date(str);
  return isNaN(d) ? str : d.toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
}

// ─── MODAL HELPERS ────────────────────────────────────────────
function openModal(id)  { document.getElementById(id).classList.add('open'); }
function closeModal(id) { document.getElementById(id).classList.remove('open'); }
