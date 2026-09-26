const state = {
  clients: [],
  invoices: []
};

const $ = (selector) => document.querySelector(selector);

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json" },
    ...options
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new Error(data?.error || "Request failed");
  }
  return data;
}

function formData(form) {
  return Object.fromEntries(new FormData(form).entries());
}

async function loadAll() {
  const [summary, clients, invoices] = await Promise.all([
    api("/api/summary"),
    api("/api/clients"),
    api("/api/invoices")
  ]);
  state.clients = clients;
  state.invoices = invoices;
  renderSummary(summary);
  renderClients();
  renderInvoices();
  renderSelects();
}

function renderSummary(summary) {
  $("#metricClients").textContent = summary.clients;
  $("#metricInvoices").textContent = summary.invoices;
  $("#metricOpen").textContent = summary.open;
  $("#metricRevenue").textContent = summary.revenue;
}

function renderClients() {
  $("#clientCount").textContent = `${state.clients.length} records`;
  $("#clientsTable").innerHTML = state.clients.map(client => `
    <tr>
      <td><strong>${escapeHtml(client.name)}</strong></td>
      <td>${escapeHtml(client.email)}</td>
      <td>${escapeHtml(client.company || "-")}</td>
      <td><code>${escapeHtml(client.id)}</code></td>
    </tr>
  `).join("");
}

function renderInvoices() {
  $("#invoiceCount").textContent = `${state.invoices.length} records`;
  $("#invoiceList").innerHTML = state.invoices.map(invoice => `
    <article class="invoice-card">
      <div>
        <h3>${escapeHtml(invoice.invoiceNumber)} <span class="badge ${invoice.status}">${invoice.status}</span></h3>
        <div class="invoice-meta">
          <span>${escapeHtml(invoice.clientName)}</span>
          <span>Due ${escapeHtml(invoice.dueDate)}</span>
          <span>${escapeHtml(invoice.items.length.toString())} items</span>
        </div>
        <strong>${escapeHtml(invoice.total)}</strong>
      </div>
      <div class="invoice-actions">
        <button data-action="sent" data-id="${invoice.id}">Sent</button>
        <button data-action="paid" data-id="${invoice.id}">Paid</button>
        <a href="/api/export/${invoice.id}" target="_blank" rel="noreferrer">HTML</a>
      </div>
    </article>
  `).join("");
}

function renderSelects() {
  const clientOptions = `<option value="">Select client</option>` + state.clients
    .map(client => `<option value="${client.id}">${escapeHtml(client.name)}</option>`)
    .join("");
  document.querySelector("[name='clientId']").innerHTML = clientOptions;

  const invoiceOptions = `<option value="">Select invoice</option>` + state.invoices
    .map(invoice => `<option value="${invoice.id}">${escapeHtml(invoice.invoiceNumber)} - ${escapeHtml(invoice.clientName)}</option>`)
    .join("");
  document.querySelector("[name='invoiceId']").innerHTML = invoiceOptions;
}

async function submitJson(path, form) {
  const payload = formData(form);
  await api(path, { method: "POST", body: JSON.stringify(payload) });
  form.reset();
  await loadAll();
  toast("Saved");
}

function toast(message) {
  const element = $("#toast");
  element.textContent = message;
  element.hidden = false;
  setTimeout(() => { element.hidden = true; }, 2400);
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

$("#clientForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await submitJson("/api/clients", event.currentTarget);
  } catch (error) {
    toast(error.message);
  }
});

$("#invoiceForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await submitJson("/api/invoices", event.currentTarget);
  } catch (error) {
    toast(error.message);
  }
});

$("#itemForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await submitJson("/api/items", event.currentTarget);
  } catch (error) {
    toast(error.message);
  }
});

$("#invoiceList").addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-action]");
  if (!button) {
    return;
  }
  const path = button.dataset.action === "paid" ? "/api/invoices/paid" : "/api/invoices/sent";
  try {
    await api(path, {
      method: "POST",
      body: JSON.stringify({ invoiceId: button.dataset.id })
    });
    await loadAll();
    toast("Invoice updated");
  } catch (error) {
    toast(error.message);
  }
});

$("#refreshBtn").addEventListener("click", () => loadAll().then(() => toast("Refreshed")));

loadAll().catch(error => toast(error.message));
