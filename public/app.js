const eventsBody = document.getElementById("eventsBody");
const emptyState = document.getElementById("emptyState");
const searchBox = document.getElementById("searchBox");
const statusFilter = document.getElementById("statusFilter");
const severityFilter = document.getElementById("severityFilter");
const eventForm = document.getElementById("eventForm");
const alertsSection = document.getElementById("alertsSection");
const alertsList = document.getElementById("alertsList");
const modalOverlay = document.getElementById("modalOverlay");
const modalContent = document.getElementById("modalContent");
const modalClose = document.getElementById("modalClose");

function updateLastRefreshed() {
  const el = document.getElementById("lastUpdated");
  if (el) el.textContent = "Last updated: " + new Date().toLocaleTimeString();
}

async function fetchSummary() {
  const res = await fetch("/api/summary");
  const s = await res.json();
  document.getElementById("sTotal").textContent = s.total;
  document.getElementById("sOpen").textContent = s.open;
  document.getElementById("sProgress").textContent = s.inProgress;
  document.getElementById("sResolved").textContent = s.resolved;
  document.getElementById("sCritical").textContent = s.criticalOpen;
}

async function fetchEvents() {
  const params = new URLSearchParams();
  if (searchBox.value) params.set("search", searchBox.value);
  if (statusFilter.value) params.set("status", statusFilter.value);
  if (severityFilter.value) params.set("severity", severityFilter.value);

  const res = await fetch("/api/events?" + params.toString());
  const events = await res.json();
  renderTable(events);
  renderAlerts(events);
  updateLastRefreshed();
}

function renderTable(events) {
  eventsBody.innerHTML = "";
  emptyState.style.display = events.length ? "none" : "block";
  events.forEach((e) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${escapeHtml(e.station)}</td>
      <td>${escapeHtml(e.issueType)}</td>
      <td><span class="badge ${e.severity}">${e.severity}</span></td>
      <td><span class="badge ${e.status}">${e.status}</span></td>
      <td>${new Date(e.timestamp).toLocaleString()}</td>
      <td>→</td>
    `;
    tr.addEventListener("click", () => openDrilldown(e.id));
    eventsBody.appendChild(tr);
  });
}

function renderAlerts(events) {
  const critical = events.filter(
    (e) => e.severity === "Critical" && e.status !== "Resolved",
  );
  if (!critical.length) {
    alertsSection.style.display = "none";
    return;
  }
  alertsSection.style.display = "block";
  alertsList.innerHTML = "";
  critical.forEach((e) => {
    const div = document.createElement("div");
    div.className = "alert-item";
    div.textContent = `${e.station} — ${e.issueType} (${e.status})`;
    div.addEventListener("click", () => openDrilldown(e.id));
    alertsList.appendChild(div);
  });
}

async function openDrilldown(id) {
  const res = await fetch("/api/events/" + id);
  const e = await res.json();
  modalContent.innerHTML = `
    <h3>${escapeHtml(e.station)}</h3>
    <p><strong>Issue:</strong> ${escapeHtml(e.issueType)}</p>
    <p><strong>Severity:</strong> <span class="badge ${e.severity}">${e.severity}</span></p>
    <p><strong>Status:</strong> <span class="badge ${e.status}">${e.status}</span></p>
    <p><strong>Logged:</strong> ${new Date(e.timestamp).toLocaleString()}</p>
    ${e.notes ? `<p><strong>Notes:</strong> ${escapeHtml(e.notes)}</p>` : ""}
    <div class="modal-actions">
      <button data-status="In-Progress">Mark In-Progress</button>
      <button data-status="Resolved">Mark Resolved</button>
    </div>
  `;
  modalContent.querySelectorAll("button[data-status]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      await fetch("/api/events/" + id, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status: btn.dataset.status }),
      });
      closeModal();
      fetchEvents();
      fetchSummary();
    });
  });
  modalOverlay.style.display = "flex";
}

function closeModal() {
  modalOverlay.style.display = "none";
}
modalClose.addEventListener("click", closeModal);
modalOverlay.addEventListener("click", (e) => {
  if (e.target === modalOverlay) closeModal();
});

eventForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const body = {
    station: document.getElementById("station").value,
    issueType: document.getElementById("issueType").value,
    severity: document.getElementById("severity").value,
    notes: document.getElementById("notes").value,
  };
  await fetch("/api/events", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  eventForm.reset();
  fetchEvents();
  fetchSummary();
});

[searchBox, statusFilter, severityFilter].forEach((el) => {
  el.addEventListener("input", fetchEvents);
  el.addEventListener("change", fetchEvents);
});

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

updateLastRefreshed();
fetchEvents();
fetchSummary();
setInterval(() => {
  fetchEvents();
  fetchSummary();
}, 5000);
