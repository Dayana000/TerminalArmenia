// Guard
if (!requireAuth(['PASAJERO'])) { /* redirige automáticamente */ }

// Header
document.getElementById('headerMount').innerHTML = renderHeader('Portal del Pasajero');

let allRoutes = [];
let selectedRoute = null;

// ── Tabs ─────────────────────────────────────────────────────
function showTab(tab, el) {
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
  document.getElementById('secRutas').style.display         = tab === 'rutas'        ? '' : 'none';
  document.getElementById('secMisReservas').style.display   = tab === 'misreservas'  ? '' : 'none';
  if (tab === 'misreservas') loadMyReservations();
}

// ── CARGAR RUTAS ─────────────────────────────────────────────
async function loadRoutes() {
  try {
    const res  = await fetch(`${API}/routes`);
    allRoutes  = await res.json();
    renderRoutes(allRoutes);
  } catch {
    document.getElementById('routesGrid').innerHTML =
      `<div style="grid-column:1/-1"><div class="empty-state"><div class="emoji">⚠️</div><p>Error al cargar rutas</p></div></div>`;
  }
}

function filterRoutes() {
  const q = document.getElementById('searchRuta').value.toLowerCase();
  renderRoutes(allRoutes.filter(r =>
    r.origin?.toLowerCase().includes(q) || r.destination?.toLowerCase().includes(q)
  ));
}

function renderRoutes(routes) {
  const grid = document.getElementById('routesGrid');
  if (!routes.length) {
    grid.innerHTML = `<div style="grid-column:1/-1"><div class="empty-state"><div class="emoji">🔍</div><p>No se encontraron rutas</p></div></div>`;
    return;
  }

  grid.innerHTML = routes.map(r => {
    const pct      = r.capacity ? Math.round((r.availableSeats / r.capacity) * 100) : 0;
    const fillClass = pct < 20 ? 'critical' : pct < 50 ? 'low' : '';
    const noSeats   = !r.availableSeats || r.availableSeats <= 0;

    return `
    <div class="route-card ${noSeats ? 'no-seats' : ''}" onclick="${noSeats ? '' : `openReservationModal(${r.id})`}"
         style="${noSeats ? 'opacity:0.5;cursor:not-allowed' : ''}">

      <div class="route-arrow">
        <span class="route-city">${r.origin}</span>
        <div class="route-divider"></div>
        <span class="route-city">${r.destination}</span>
      </div>

      <div class="route-meta">
        <div class="route-meta-item">
          <span class="label">🕐 Horario</span>
          <span class="value">${r.schedule || '—'}</span>
        </div>
        <div class="route-meta-item">
          <span class="label">💰 Precio</span>
          <span class="value price-tag">${formatPrice(r.price)}</span>
        </div>
      </div>

      <div class="seats-bar">
        <div class="seats-fill ${fillClass}" style="width:${pct}%"></div>
      </div>
      <div style="display:flex;justify-content:space-between;align-items:center;margin-top:6px">
        <span style="font-size:12px;color:var(--muted)">
          ${noSeats
            ? '<span style="color:var(--danger)">🚫 Sin disponibilidad</span>'
            : `<span style="color:${pct<20?'var(--danger)':pct<50?'var(--warning)':'var(--success)'}">
                ${r.availableSeats} asiento${r.availableSeats !== 1 ? 's' : ''} disponible${r.availableSeats !== 1 ? 's' : ''}
               </span>`}
        </span>
        ${!noSeats ? '<span style="color:var(--accent);font-size:13px;font-weight:600">Reservar →</span>' : ''}
      </div>
    </div>`;
  }).join('');
}

// ── MODAL RESERVA ─────────────────────────────────────────────
function openReservationModal(routeId) {
  selectedRoute = allRoutes.find(r => r.id === routeId);
  if (!selectedRoute) return;

  document.getElementById('modalRouteSummary').innerHTML = `
    <div class="route-title">${selectedRoute.origin} → ${selectedRoute.destination}</div>
    <div class="route-details">
      <div>🕐 ${selectedRoute.schedule || '—'}</div>
      <div>💺 ${selectedRoute.availableSeats} disponibles</div>
      <div>💰 ${formatPrice(selectedRoute.price)}</div>
      <div>🚌 Cap. total: ${selectedRoute.capacity}</div>
    </div>
  `;
  document.getElementById('seatInput').value = '';
  openModal('modalReserva');
}

// ── CONFIRMAR RESERVA (Proceso 3) ─────────────────────────────
async function confirmReservation() {
  const seat = document.getElementById('seatInput').value.trim();
  if (!seat) { showToast('Ingresa el número de asiento', 'error'); return; }
  if (!selectedRoute) return;

  const userId = getUserId();
  if (!userId) { showToast('Sesión expirada, inicia sesión nuevamente', 'error'); return; }

  const resBtnText = document.getElementById('resBtnText');
  const resSpinner = document.getElementById('resSpinner');
  resBtnText.textContent = 'Procesando…';
  resSpinner.style.display = 'inline-block';

  try {
    const res = await fetch(`${API}/reservations`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId:  userId,
        routeId: selectedRoute.id,
        seat:    seat
      })
    });

    const text = await res.text();
    let data;
    try { data = JSON.parse(text); } catch { data = null; }

    if (res.ok && data && data.id) {
      closeModal('modalReserva');

      // Mostrar tiquete
      document.getElementById('ticketNumber').textContent = data.reservationNumber;
      document.getElementById('ticketDetails').innerHTML = `
        <div>🚌 <strong>${data.origin}</strong> → <strong>${data.destination}</strong></div>
        <div>🕐 ${data.schedule || '—'}</div>
        <div>💺 Asiento: <strong>${data.seat}</strong></div>
        <div>💰 ${formatPrice(data.price)}</div>
        <div style="margin-top:8px"><span class="badge badge-reservada">RESERVADA</span></div>
      `;
      openModal('modalTicket');
      loadRoutes(); // Actualizar disponibilidad
    } else {
      const msg = (data && typeof data === 'string') ? data : text;
      showToast(msg || 'Error al crear la reserva', 'error');
    }
  } catch {
    showToast('Error de conexión con el servidor', 'error');
  } finally {
    resBtnText.textContent = '🎫 Reservar Tiquete';
    resSpinner.style.display = 'none';
  }
}

// ── MIS RESERVAS ──────────────────────────────────────────────
async function loadMyReservations() {
  const userId = getUserId();
  if (!userId) return;

  const container = document.getElementById('misReservasList');
  container.innerHTML = `<div style="text-align:center;padding:40px;color:var(--muted)"><div class="spinner" style="margin:0 auto 12px"></div><p>Cargando…</p></div>`;

  try {
    const res  = await fetch(`${API}/reservations/user/${userId}`);
    const list = await res.json();

    if (!list.length) {
      container.innerHTML = `<div class="empty-state"><div class="emoji">🎫</div><p>Aún no tienes reservas. ¡Busca una ruta y reserva tu primer tiquete!</p></div>`;
      return;
    }

    // Ordenar: más recientes primero
    list.sort((a, b) => b.id - a.id);

    container.innerHTML = list.map(r => {
      const statusBadge = `<span class="badge badge-${r.status?.toLowerCase()}">${r.status}</span>`;
      const canCancel   = r.status !== 'CANCELADA';

      return `
      <div class="reservation-card fade-in">
        <div>
          <div class="res-number">${r.reservationNumber || '—'}</div>
          <div class="res-route">${r.origin} → ${r.destination}</div>
          <div class="res-meta">🕐 ${r.schedule || '—'} &nbsp;·&nbsp; 💺 Asiento ${r.seat} &nbsp;·&nbsp; 💰 ${formatPrice(r.price)}</div>
        </div>
        <div class="res-actions">
          ${statusBadge}
          ${canCancel
            ? `<button class="btn btn-danger btn-sm" onclick="cancelMyReservation(${r.id})">Cancelar</button>`
            : ''}
        </div>
      </div>`;
    }).join('');
  } catch {
    container.innerHTML = `<div class="empty-state"><div class="emoji">⚠️</div><p>Error al cargar tus reservas</p></div>`;
  }
}

async function cancelMyReservation(id) {
  if (!confirm('¿Deseas cancelar esta reserva? Se liberará el asiento.')) return;
  try {
    const res = await fetch(`${API}/reservations/${id}/cancel`, { method: 'PUT' });
    if (res.ok) {
      showToast('Reserva cancelada', 'info');
      loadMyReservations();
      loadRoutes();
    } else {
      const t = await res.text();
      showToast(t || 'Error al cancelar', 'error');
    }
  } catch { showToast('Error de conexión', 'error'); }
}

// Cerrar modal al click fuera
document.getElementById('modalReserva').addEventListener('click', function(e) {
  if (e.target === this) closeModal('modalReserva');
});
document.getElementById('modalTicket').addEventListener('click', function(e) {
  if (e.target === this) closeModal('modalTicket');
});

// ── INIT ──────────────────────────────────────────────────────
loadRoutes();
