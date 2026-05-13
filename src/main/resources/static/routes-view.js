if (!requireAuth(['PASAJERO', 'USER'])) { /* redirige automaticamente */ }

document.getElementById('headerMount').innerHTML = renderHeader('Portal del Pasajero');

let allRoutes    = [];
let selectedRoute = null;

function authHeaders(extra) {
    extra = extra || {};
    var token = sessionStorage.getItem('token');
    var headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = 'Bearer ' + token;
    return Object.assign(headers, extra);
}

// TABS
function showTab(tab, el) {
    document.querySelectorAll('.tab').forEach(function(t) { t.classList.remove('active'); });
    el.classList.add('active');
    document.getElementById('secRutas').style.display       = tab === 'rutas'       ? '' : 'none';
    document.getElementById('secMisReservas').style.display = tab === 'misreservas' ? '' : 'none';
    if (tab === 'misreservas') loadMyReservations();
}

// CARGAR RUTAS
async function loadRoutes() {
    try {
        var res = await fetch(API + '/routes', { headers: authHeaders() });
        allRoutes = await res.json();
        renderRoutes(allRoutes);
    } catch(e) {
        document.getElementById('routesGrid').innerHTML =
            '<div style="grid-column:1/-1"><div class="empty-state"><div class="emoji">⚠️</div><p>Error al cargar rutas</p></div></div>';
    }
}

function filterRoutes() {
    var q = document.getElementById('searchRuta').value.toLowerCase();
    renderRoutes(allRoutes.filter(function(r) {
        return (r.origin && r.origin.toLowerCase().includes(q)) ||
            (r.destination && r.destination.toLowerCase().includes(q));
    }));
}

function renderRoutes(routes) {
    var grid = document.getElementById('routesGrid');
    if (!routes.length) {
        grid.innerHTML = '<div style="grid-column:1/-1"><div class="empty-state"><div class="emoji">🔍</div><p>No se encontraron rutas</p></div></div>';
        return;
    }
    grid.innerHTML = routes.map(function(r) {
        var pct       = r.capacity ? Math.round((r.availableSeats / r.capacity) * 100) : 0;
        var fillClass = pct < 20 ? 'critical' : pct < 50 ? 'low' : '';
        var noSeats   = !r.availableSeats || r.availableSeats <= 0;
        var seatColor = pct < 20 ? 'var(--danger)' : pct < 50 ? 'var(--warning)' : 'var(--success)';
        var seatText  = noSeats
            ? '<span style="color:var(--danger)">🚫 Sin disponibilidad</span>'
            : '<span style="color:' + seatColor + '">' + r.availableSeats + ' asiento' + (r.availableSeats !== 1 ? 's' : '') + ' disponible' + (r.availableSeats !== 1 ? 's' : '') + '</span>';
        var onclick   = noSeats ? '' : 'openReservationModal(' + r.id + ')';
        var cardStyle = noSeats ? 'opacity:0.5;cursor:not-allowed' : '';
        return '<div class="route-card" onclick="' + onclick + '" style="' + cardStyle + '">' +
            '<div class="route-arrow">' +
            '<span class="route-city">' + r.origin + '</span>' +
            '<div class="route-divider"></div>' +
            '<span class="route-city">' + r.destination + '</span>' +
            '</div>' +
            '<div class="route-meta">' +
            '<div class="route-meta-item"><span class="label">🕐 Horario</span><span class="value">' + (r.schedule || '—') + '</span></div>' +
            '<div class="route-meta-item"><span class="label">💰 Precio</span><span class="value price-tag">' + formatPrice(r.price) + '</span></div>' +
            '</div>' +
            '<div class="seats-bar"><div class="seats-fill ' + fillClass + '" style="width:' + pct + '%"></div></div>' +
            '<div style="display:flex;justify-content:space-between;align-items:center;margin-top:6px">' +
            '<span style="font-size:12px;color:var(--muted)">' + seatText + '</span>' +
            (!noSeats ? '<span style="color:var(--accent);font-size:13px;font-weight:600">Reservar →</span>' : '') +
            '</div>' +
            '</div>';
    }).join('');
}

// MODAL RESERVA
async function openReservationModal(routeId) {
    selectedRoute = allRoutes.find(function(r) { return r.id === routeId; });
    if (!selectedRoute) return;

    document.getElementById('modalRouteSummary').innerHTML =
        '<div class="route-title">' + selectedRoute.origin + ' → ' + selectedRoute.destination + '</div>' +
        '<div class="route-details">' +
        '<div>🕐 ' + (selectedRoute.schedule || '—') + '</div>' +
        '<div>💺 ' + selectedRoute.availableSeats + ' disponibles</div>' +
        '<div>💰 ' + formatPrice(selectedRoute.price) + '</div>' +
        '</div>';

    document.getElementById('seatInput').value = '';
    document.getElementById('seatSelectionText').innerHTML = 'Haz clic en un asiento verde';
    
    var mapContainer = document.getElementById('seatMap');
    mapContainer.innerHTML = '<div style="text-align:center;padding:20px;grid-column:1/-1"><div class="spinner"></div><p>Cargando asientos...</p></div>';

    openModal('modalReserva');

    try {
        // Consultar asientos ocupados desde el backend
        var res = await fetch(API + '/reservations/route/' + routeId + '/seats', { headers: authHeaders() });
        var takenSeats = await res.json(); // Lista de strings ej: ["1", "5", "12"]

        var html = '';
        var capacity = selectedRoute.capacity || 40;

        for (var i = 1; i <= capacity; i++) {
            var seatNum = i.toString();
            var isTaken = takenSeats.includes(seatNum);
            var cls = isTaken ? 'seat-btn taken' : 'seat-btn available';
            var attr = isTaken ? 'disabled' : 'onclick="selectSeat(\'' + seatNum + '\', this)"';
            
            html += '<button class="' + cls + '" ' + attr + '>' + seatNum + '</button>';
        }
        mapContainer.innerHTML = html;

    } catch (e) {
        mapContainer.innerHTML = '<p style="color:var(--danger);grid-column:1/-1;text-align:center">Error al cargar el mapa de asientos</p>';
    }
}

function selectSeat(num, el) {
    // Desmarcar anterior
    document.querySelectorAll('.seat-btn.selected').forEach(function(b) {
        b.classList.remove('selected');
        b.classList.add('available');
    });

    // Marcar actual
    el.classList.remove('available');
    el.classList.add('selected');
    
    document.getElementById('seatInput').value = num;
    document.getElementById('seatSelectionText').innerHTML = 'Asiento seleccionado: <span style="color:var(--accent)">' + num + '</span>';
}

// PASO 1: Crear reserva → PASO 2: Abrir modal de pago simulado
async function confirmReservation() {
    var seat = document.getElementById('seatInput').value.trim();
    if (!seat) { showToast('Por favor, selecciona un asiento en el mapa', 'error'); return; }
    if (!selectedRoute) return;

    var userId    = getUserId();
    var userEmail = getUser() ? getUser().email : null;
    if (!userId) { showToast('Sesion expirada', 'error'); return; }

    var resBtnText = document.getElementById('resBtnText');
    var resSpinner = document.getElementById('resSpinner');
    resBtnText.textContent = 'Creando reserva...';
    resSpinner.style.display = 'inline-block';

    try {
        // PASO 1: crear reserva
        var resResp = await fetch(API + '/reservations', {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify({ userId: userId, routeId: selectedRoute.id, seat: seat })
        });
        var resData;
        try { resData = await resResp.json(); } catch(e) { resData = null; }

        if (!resResp.ok || !resData || !resData.id) {
            showToast(typeof resData === 'string' ? resData : 'Error al crear la reserva', 'error');
            return;
        }

        resBtnText.textContent = 'Iniciando pago...';

        // PASO 2: iniciar pago
        var payResp = await fetch(API + '/payments/init', {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify({ reservationId: resData.id, customerEmail: userEmail || '' })
        });
        var payData;
        try { payData = await payResp.json(); } catch(e) { payData = null; }

        if (!payResp.ok || !payData) {
            showToast('Error al iniciar el pago', 'error'); return;
        }

        closeModal('modalReserva');
        openPaymentModal(payData, resData);

    } catch(e) {
        showToast('Error de conexion', 'error');
    } finally {
        resBtnText.textContent = '💳 Continuar al Pago';
        resSpinner.style.display = 'none';
    }
}

// MODAL DE PAGO SIMULADO
function openPaymentModal(payData, reservation) {
    document.getElementById('pagoResumen').innerHTML =
        '<div style="text-align:center;padding:8px 0 20px">' +
        '<div style="font-size:12px;color:var(--muted);margin-bottom:4px">N° de Reserva</div>' +
        '<div style="font-size:20px;font-weight:800;color:var(--accent);font-family:monospace">' + reservation.reservationNumber + '</div>' +
        '<div style="font-size:13px;color:var(--muted);margin-top:8px">' + reservation.origin + ' → ' + reservation.destination + '</div>' +
        '<div style="font-size:24px;font-weight:700;color:var(--text);margin-top:10px">' + formatPrice(reservation.price) + '</div>' +
        '</div>';

    // Formulario visual de tarjeta (solo decorativo)
    document.getElementById('wompiContainer').innerHTML =
        '<div style="width:100%;background:var(--bg2);border-radius:12px;padding:20px;margin-bottom:16px">' +
        '<div style="font-size:11px;color:var(--muted);margin-bottom:6px;text-transform:uppercase;letter-spacing:.08em">Numero de tarjeta</div>' +
        '<div style="background:var(--bg3);border:1px solid var(--border);border-radius:8px;padding:12px;font-size:15px;color:var(--muted);font-family:monospace;letter-spacing:.15em">4242  4242  4242  4242</div>' +
        '<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:12px">' +
        '<div><div style="font-size:11px;color:var(--muted);margin-bottom:6px;text-transform:uppercase;letter-spacing:.08em">Vencimiento</div>' +
        '<div style="background:var(--bg3);border:1px solid var(--border);border-radius:8px;padding:12px;font-size:15px;color:var(--muted)">12/28</div></div>' +
        '<div><div style="font-size:11px;color:var(--muted);margin-bottom:6px;text-transform:uppercase;letter-spacing:.08em">CVV</div>' +
        '<div style="background:var(--bg3);border:1px solid var(--border);border-radius:8px;padding:12px;font-size:15px;color:var(--muted)">•••</div></div>' +
        '</div>' +
        '</div>' +
        '<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">' +
        '<button onclick="simulatePayment(\'' + payData.wompiReference + '\', \'APROBADO\')" ' +
        'style="background:linear-gradient(135deg,#16a34a,#15803d);color:#fff;border:none;border-radius:10px;' +
        'padding:14px;font-size:14px;font-weight:700;cursor:pointer;display:flex;align-items:center;justify-content:center;gap:8px">' +
        '✅ Pagar Ahora</button>' +
        '<button onclick="simulatePayment(\'' + payData.wompiReference + '\', \'RECHAZADO\')" ' +
        'style="background:var(--bg3);color:var(--danger);border:1px solid var(--danger);border-radius:10px;' +
        'padding:14px;font-size:14px;font-weight:600;cursor:pointer">' +
        '❌ Cancelar</button>' +
        '</div>';

    openModal('modalPago');
}

// SIMULAR PAGO
async function simulatePayment(reference, status) {
    try {
        var btns = document.querySelectorAll('#wompiContainer button');
        btns.forEach(function(b) { b.disabled = true; b.style.opacity = '0.6'; });

        var resp = await fetch(API + '/payments/simulate', {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify({ reference: reference, status: status })
        });

        if (resp.ok) {
            var data = await resp.json();
            closeModal('modalPago');
            // Redirigir a pagina de resultado
            var resNum = '';
            if (data.reservationId) {
                // Buscar el numero de reserva en las rutas cargadas
                var myRes = await fetch(API + '/reservations/user/' + getUserId(), { headers: authHeaders() });
                var myList = await myRes.json();
                var found = myList.find(function(r) { return r.id === data.reservationId; });
                if (found) resNum = found.reservationNumber;
            }
            window.location.href = 'payment-result.html?ref=' + reference + '&res=' + resNum;
        } else {
            showToast('Error al procesar el pago', 'error');
            var btns2 = document.querySelectorAll('#wompiContainer button');
            btns2.forEach(function(b) { b.disabled = false; b.style.opacity = '1'; });
        }
    } catch(e) {
        showToast('Error de conexion', 'error');
    }
}

// MIS RESERVAS
async function loadMyReservations() {
    var userId = getUserId();
    if (!userId) return;
    var container = document.getElementById('misReservasList');
    container.innerHTML = '<div style="text-align:center;padding:40px;color:var(--muted)"><div class="spinner" style="margin:0 auto 12px"></div><p>Cargando...</p></div>';
    try {
        var res  = await fetch(API + '/reservations/user/' + userId, { headers: authHeaders() });
        var list = await res.json();
        if (!list.length) {
            container.innerHTML = '<div class="empty-state"><div class="emoji">🎫</div><p>Aun no tienes reservas</p></div>';
            return;
        }
        list.sort(function(a, b) { return b.id - a.id; });
        var labels = {
            'RESERVADA': 'RESERVADA', 'PENDIENTE_PAGO': '⏳ PENDIENTE PAGO',
            'CONFIRMADA': '✅ CONFIRMADA', 'CANCELADA': 'CANCELADA'
        };
        container.innerHTML = list.map(function(r) {
            var cls       = r.status ? r.status.toLowerCase().replace('_', '-') : '';
            var canCancel = r.status !== 'CANCELADA' && r.status !== 'CONFIRMADA';
            var cancelBtn = canCancel
                ? '<button class="btn btn-danger btn-sm" onclick="cancelMyReservation(' + r.id + ')">Cancelar</button>'
                : '';
            return '<div class="reservation-card fade-in">' +
                '<div>' +
                '<div class="res-number">' + (r.reservationNumber || '—') + '</div>' +
                '<div class="res-route">' + r.origin + ' → ' + r.destination + '</div>' +
                '<div class="res-meta">🕐 ' + (r.schedule || '—') + ' &nbsp;·&nbsp; 💺 Asiento ' + r.seat + ' &nbsp;·&nbsp; 💰 ' + formatPrice(r.price) + '</div>' +
                '</div>' +
                '<div class="res-actions"><span class="badge badge-' + cls + '">' + (labels[r.status] || r.status) + '</span>' + cancelBtn + '</div>' +
                '</div>';
        }).join('');
    } catch(e) {
        container.innerHTML = '<div class="empty-state"><div class="emoji">⚠️</div><p>Error al cargar</p></div>';
    }
}

async function cancelMyReservation(id) {
    if (!confirm('Deseas cancelar esta reserva? Se liberara el asiento.')) return;
    try {
        var res = await fetch(API + '/reservations/' + id + '/cancel', { method: 'PUT', headers: authHeaders() });
        if (res.ok) { showToast('Reserva cancelada', 'info'); loadMyReservations(); loadRoutes(); }
        else { var t = await res.text(); showToast(t || 'Error al cancelar', 'error'); }
    } catch(e) { showToast('Error de conexion', 'error'); }
}

document.getElementById('modalReserva').addEventListener('click', function(e) { if (e.target === this) closeModal('modalReserva'); });
document.getElementById('modalPago').addEventListener('click',    function(e) { if (e.target === this) closeModal('modalPago'); });

loadRoutes();