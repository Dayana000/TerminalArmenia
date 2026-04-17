// Guard
if (!requireAuth(['ADMIN', 'EMPRESA'])) { /* redirige automáticamente */ }

// Render header
document.getElementById('headerMount').innerHTML = renderHeader('Panel de Administración');

const role = getRole();
let allRoutes = [];
let allReservations = [];
let editingRouteId = null;

// ── Tab switching ────────────────────────────────────────────
function showTab(tab) {
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  document.getElementById('secRutas').style.display    = tab === 'rutas'    ? '' : 'none';
  document.getElementById('secReservas').style.display = tab === 'reservas' ? '' : 'none';
  event.target.classList.add('active');
  if (tab === 'reservas') loadReservations();
}

// Ocultar tab reservas para EMPRESA
if (role !== 'ADMIN') document.getElementById('tabReservas').style.display = 'none';

// ── RUTAS ────────────────────────────────────────────────────
async function loadRoutes() {
  try {
    const res    = await fetch(`${API}/routes`);
    allRoutes    = await res.json();
    renderRoutes(allRoutes);
    updateRouteStats(allRoutes);
  } catch { showToast('Error al cargar rutas', 'error'); }
}

function updateRouteStats(routes) {
  document.getElementById('statTotal').textContent  = routes.length;
  const totalCupos = routes.reduce((s, r) => s + (r.availableSeats || 0), 0);
  document.getElementById('statCupos').textContent  = totalCupos;
  const avg = routes.length ? routes.reduce((s, r) => s + (r.price || 0), 0) / routes.length : 0;
  document.getElementById('statPrecio').textContent = avg ? `$${Math.round(avg/1000)}K` : '—';
}

function renderRoutes(routes) {
  const tbody = document.getElementById('routesTableBody');
  if (!routes.length) {
    tbody.innerHTML = `<tr><td colspan="7"><div class="empty-state"><div class="emoji">🗺️</div><p>No hay rutas registradas aún</p></div></td></tr>`;
    return;
  }
  tbody.innerHTML = routes.map(r => {
    const pct   = r.capacity ? Math.round((r.availableSeats / r.capacity) * 100) : 0;
    const color = pct < 20 ? 'var(--danger)' : pct < 50 ? 'var(--warning)' : 'var(--success)';
    return `
    <tr>
      <td><strong>${r.origin}</strong></td>
      <td>${r.destination}</td>
      <td style="color:var(--muted);font-size:13px">${r.schedule || '—'}</td>
      <td><span style="color:var(--accent);font-weight:700">${formatPrice(r.price)}</span></td>
      <td>${r.capacity}</td>
      <td>
        <div style="display:flex;align-items:center;gap:8px">
          <div class="seats-bar" style="width:60px;margin:0">
            <div class="seats-fill ${pct<20?'critical':pct<50?'low':''}" style="width:${pct}%"></div>
          </div>
          <span style="color:${color};font-weight:600">${r.availableSeats}</span>
        </div>
      </td>
      <td>
        <div class="td-actions">
          <button class="btn btn-secondary btn-sm" onclick='editRoute(${JSON.stringify(r)})'>✏️ Editar</button>
          <button class="btn btn-danger btn-sm" onclick="deleteRoute(${r.id})">🗑️</button>
        </div>
      </td>
    </tr>`;
  }).join('');
}

function filterRoutes() {
  const q = document.getElementById('searchRuta').value.toLowerCase();
  renderRoutes(allRoutes.filter(r =>
    r.origin?.toLowerCase().includes(q) || r.destination?.toLowerCase().includes(q)
  ));
}

async function saveRoute() {
  const origin      = document.getElementById('origin').value.trim();
  const destination = document.getElementById('destination').value.trim();
  const sDate       = document.getElementById('scheduleDate').value;
  const sTime       = document.getElementById('scheduleTime').value;
  const price       = Number(document.getElementById('price').value);
  const capacity    = Number(document.getElementById('capacity').value);

  if (!origin || !destination) { showToast('Origen y destino son obligatorios', 'error'); return; }
  if (!sDate || !sTime)        { showToast('Selecciona fecha y hora de salida', 'error'); return; }
  if (!price || price <= 0)    { showToast('El precio debe ser mayor que cero', 'error'); return; }
  if (!capacity || capacity<=0){ showToast('La capacidad debe ser mayor que cero', 'error'); return; }

  const body = { origin, destination, schedule: `${sDate} ${sTime}`, price, capacity };

  const saveBtnText = document.getElementById('saveBtnText');
  const saveSpinner = document.getElementById('saveSpinner');
  saveBtnText.textContent = 'Guardando...';
  saveSpinner.style.display = 'inline-block';

  try {
    const url    = editingRouteId ? `${API}/routes/${editingRouteId}` : `${API}/routes`;
    const method = editingRouteId ? 'PUT' : 'POST';
    const res    = await fetch(url, { method, headers: {'Content-Type':'application/json'}, body: JSON.stringify(body) });

    if (res.ok) {
      showToast(editingRouteId ? 'Ruta actualizada ✓' : 'Ruta creada ✓', 'success');
      closeModal('modalRuta');
      clearRutaForm();
      loadRoutes();
    } else {
      const t = await res.text();
      showToast(t || 'Error al guardar', 'error');
    }
  } catch { showToast('Error de conexión', 'error'); }
  finally {
    saveBtnText.textContent = 'Guardar Ruta';
    saveSpinner.style.display = 'none';
  }
}

function editRoute(r) {
  editingRouteId = r.id;
  const parts = (r.schedule || '').split(' ');
  document.getElementById('origin').value        = r.origin;
  document.getElementById('destination').value   = r.destination;
  document.getElementById('scheduleDate').value  = parts[0] || '';
  document.getElementById('scheduleTime').value  = parts[1] || '';
  document.getElementById('price').value         = r.price;
  document.getElementById('capacity').value      = r.capacity;
  document.getElementById('modalRutaTitle').textContent = 'Editar Ruta';
  openModal('modalRuta');
}

async function deleteRoute(id) {
  if (!confirm('¿Eliminar esta ruta? Esta acción no se puede deshacer.')) return;
  try {
    const res = await fetch(`${API}/routes/${id}`, { method: 'DELETE' });
    if (res.ok) { showToast('Ruta eliminada', 'info'); loadRoutes(); }
    else showToast('Error al eliminar', 'error');
  } catch { showToast('Error de conexión', 'error'); }
}

function clearRutaForm() {
  ['origin','destination','scheduleDate','scheduleTime','price','capacity'].forEach(id => {
    document.getElementById(id).value = '';
  });
  editingRouteId = null;
  document.getElementById('modalRutaTitle').textContent = 'Nueva Ruta';
}

// Reset form al cerrar modal
document.getElementById('modalRuta').addEventListener('click', function(e) {
  if (e.target === this) { closeModal('modalRuta'); clearRutaForm(); }
});

// ── RESERVAS (solo ADMIN) ─────────────────────────────────────
async function loadReservations() {
  try {
    const res       = await fetch(`${API}/reservations`);
    allReservations = await res.json();
    renderReservations(allReservations);
    updateResStats(allReservations);
  } catch { showToast('Error al cargar reservas', 'error'); }
}

function updateResStats(list) {
  document.getElementById('rStatTotal').textContent      = list.length;
  document.getElementById('rStatReservadas').textContent  = list.filter(r => r.status === 'RESERVADA').length;
  document.getElementById('rStatConfirmadas').textContent = list.filter(r => r.status === 'CONFIRMADA').length;
  document.getElementById('rStatCanceladas').textContent  = list.filter(r => r.status === 'CANCELADA').length;
}

function renderReservations(list) {
  const tbody = document.getElementById('reservationsTableBody');
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="8"><div class="empty-state"><div class="emoji">📋</div><p>No hay reservas registradas</p></div></td></tr>`;
    return;
  }
  tbody.innerHTML = list.map(r => {
    const statusBadge = `<span class="badge badge-${r.status?.toLowerCase()}">${r.status}</span>`;
    const actions = r.status !== 'CANCELADA' ? `
      ${r.status !== 'CONFIRMADA' ? `<button class="btn btn-success btn-sm" onclick="confirmRes(${r.id})">✅ Confirmar</button>` : ''}
      <button class="btn btn-danger btn-sm" onclick="cancelRes(${r.id})">❌ Cancelar</button>
    ` : '<span style="color:var(--muted);font-size:12px">—</span>';

    return `
    <tr>
      <td><span style="font-family:var(--font-head);color:var(--accent);font-size:12px">${r.reservationNumber || '—'}</span></td>
      <td>${r.passengerName || '—'}</td>
      <td><strong>${r.origin}</strong> → ${r.destination}</td>
      <td style="color:var(--muted);font-size:12px">${r.schedule || '—'}</td>
      <td>${r.seat}</td>
      <td style="color:var(--accent);font-weight:700">${formatPrice(r.price)}</td>
      <td>${statusBadge}</td>
      <td><div class="td-actions">${actions}</div></td>
    </tr>`;
  }).join('');
}

function filterReservations() {
  const q = document.getElementById('searchRes').value.toLowerCase();
  renderReservations(allReservations.filter(r =>
    r.reservationNumber?.toLowerCase().includes(q) ||
    r.passengerName?.toLowerCase().includes(q) ||
    r.origin?.toLowerCase().includes(q) ||
    r.destination?.toLowerCase().includes(q)
  ));
}

async function confirmRes(id) {
  try {
    const res = await fetch(`${API}/reservations/${id}/confirm`, { method: 'PUT' });
    if (res.ok) { showToast('Reserva confirmada ✓', 'success'); loadReservations(); }
    else showToast('Error al confirmar', 'error');
  } catch { showToast('Error de conexión', 'error'); }
}

async function cancelRes(id) {
  if (!confirm('¿Cancelar esta reserva? Se devolverá el cupo a la ruta.')) return;
  try {
    const res = await fetch(`${API}/reservations/${id}/cancel`, { method: 'PUT' });
    if (res.ok) { showToast('Reserva cancelada', 'info'); loadReservations(); }
    else showToast('Error al cancelar', 'error');
  } catch { showToast('Error de conexión', 'error'); }
}

// ── INIT ──────────────────────────────────────────────────────
loadRoutes();
