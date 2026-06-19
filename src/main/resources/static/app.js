const page = document.body.dataset.page;
const statusText = document.getElementById('statusText');

async function api(path, method = 'GET', body = null) {
  const response = await fetch(path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `${response.status} ${response.statusText}`);
  }
  if (response.status === 204 || response.headers.get('Content-Length') === '0') {
    return null;
  }
  return response.json();
}

function escapeHtml(value) {
  if (value == null) return '';
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function formatDate(value) {
  if (!value) return '-';
  try {
    return new Date(value).toLocaleString();
  } catch {
    return String(value);
  }
}

function formatTags(tags) {
  if (!Array.isArray(tags) || tags.length === 0) return '-';
  return tags.map(tag => `<span>${escapeHtml(tag)}</span>`).join(' ');
}

function renderArrayRows(items, rowRenderer) {
  const body = items.map(rowRenderer).join('');
  return body;
}

function setStatus(message, type = 'text') {
  if (!statusText) return;
  statusText.textContent = message;
  if (type === 'error') {
    statusText.style.color = '#b91c1c';
  } else {
    statusText.style.color = '';
  }
}

function wireButton(id, handler) {
  const button = document.getElementById(id);
  if (!button) return;
  button.addEventListener('click', handler);
}

async function initStatus() {
  wireButton('refreshBtn', loadStatus);
  await loadStatus();
}

async function loadStatus() {
  setStatus('Loading status...');
  try {
    const status = await api('/api/monitor/status');
    document.getElementById('lastScan').textContent = formatDate(status.lastScanTime);
    document.getElementById('activeConnections').textContent = status.activeConnections;
    document.getElementById('configuredServices').textContent = status.configuredServices;
    document.getElementById('configuredClients').textContent = status.configuredClients;
    document.getElementById('lastDuration').textContent = `${status.lastScanDurationMs} ms`;
    document.getElementById('lastError').textContent = status.lastError || 'None';
    document.getElementById('connectionEvents').textContent = status.connectionEvents;
    document.getElementById('connectedEvents').textContent = status.connectedEvents;
    document.getElementById('disconnectedEvents').textContent = status.disconnectedEvents;
    setStatus('Status loaded.');
  } catch (error) {
    setStatus(error.message || 'Failed to load status.', 'error');
  }
}

async function initConnections() {
  wireButton('refreshBtn', loadConnections);
  await loadConnections();
}

async function loadConnections() {
  setStatus('Loading connections...');
  try {
    const connections = await api('/api/connections');
    const rows = connections.map(conn => `
      <tr>
        <td>${escapeHtml(conn.serviceName)}</td>
        <td>${escapeHtml(conn.clientName)}</td>
        <td>${escapeHtml(conn.localIp)}:${conn.localPort}</td>
        <td>${escapeHtml(conn.remoteIp)}:${conn.remotePort}</td>
        <td>${formatDate(conn.connectedAt)}</td>
        <td>${formatDate(conn.lastSeenAt)}</td>
        <td><div class="tagList">${formatTags(conn.tags)}</div></td>
      </tr>
    `);
    document.getElementById('connectionRows').innerHTML = rows.join('');
    setStatus(`Loaded ${connections.length} connection${connections.length === 1 ? '' : 's'}.`);
  } catch (error) {
    setStatus(error.message || 'Failed to load connections.', 'error');
  }
}

async function initEvents() {
  wireButton('refreshBtn', loadEvents);
  await loadEvents();
}

async function loadEvents() {
  setStatus('Loading events...');
  try {
    const events = await api('/api/events');
    const body = events.map(evt => `
      <tr>
        <td>${formatDate(evt.occurredAt)}</td>
        <td>${escapeHtml(evt.type)}</td>
        <td>${escapeHtml(evt.serviceName)}</td>
        <td>${escapeHtml(evt.clientName)}</td>
        <td>${escapeHtml(evt.localIp)}:${evt.localPort}</td>
        <td>${escapeHtml(evt.remoteIp)}:${evt.remotePort}</td>
        <td><div class="tagList">${formatTags(evt.tags)}</div></td>
      </tr>
    `).join('');
    document.getElementById('eventRows').innerHTML = body;
    setStatus(`Loaded ${events.length} event${events.length === 1 ? '' : 's'}.`);
  } catch (error) {
    setStatus(error.message || 'Failed to load events.', 'error');
  }
}

function makeTagString(source) {
  if (!source) return null;
  return source.split(',').map(value => value.trim()).filter(Boolean);
}

function fillServiceForm(service = null) {
  document.getElementById('serviceName').value = service?.serviceName || '';
  document.getElementById('serviceDescription').value = service?.description || '';
  document.getElementById('servicePort').value = service?.port || '';
  document.getElementById('serviceTags').value = Array.isArray(service?.tags) ? service.tags.join(', ') : '';
  document.getElementById('serviceEnabled').checked = service?.enabled !== false;
  document.getElementById('serviceFormTitle').textContent = service ? 'Edit Service' : 'Add Service';
}

async function initServices() {
  wireButton('refreshBtn', loadServices);
  wireButton('newServiceBtn', () => {
    document.getElementById('serviceFormContainer').classList.remove('hidden');
    document.getElementById('serviceId').value = '';
    fillServiceForm(null);
  });
  wireButton('cancelServiceBtn', () => document.getElementById('serviceFormContainer').classList.add('hidden'));
  document.getElementById('serviceForm').addEventListener('submit', handleServiceSubmit);
  await loadServices();
}

let serviceId = null;

async function loadServices() {
  setStatus('Loading services...');
  try {
    const services = await api('/api/services');
    const body = renderArrayRows(services, service => `
      <tr>
        <td>${escapeHtml(service.serviceName)}</td>
        <td>${escapeHtml(service.description)}</td>
        <td>${service.port}</td>
        <td><div class="tagList">${formatTags(service.tags)}</div></td>
        <td>${service.enabled ? 'Yes' : 'No'}</td>
        <td>
          <button type="button" data-id="${service.id}" class="editServiceBtn">Edit</button>
          <button type="button" data-id="${service.id}" class="deleteServiceBtn">Delete</button>
        </td>
      </tr>
    `);
    document.getElementById('serviceRows').innerHTML = body;
    document.querySelectorAll('.editServiceBtn').forEach(button => {
      button.addEventListener('click', async () => {
        const id = button.dataset.id;
        const service = services.find(s => s.id === id);
        if (!service) return;
        serviceId = id;
        fillServiceForm(service);
        document.getElementById('serviceFormContainer').classList.remove('hidden');
      });
    });
    document.querySelectorAll('.deleteServiceBtn').forEach(button => {
      button.addEventListener('click', async () => {
        if (!confirm('Delete this service?')) return;
        const id = button.dataset.id;
        try {
          await api(`/api/services/${id}`, 'DELETE');
          setStatus('Service deleted.');
          await loadServices();
        } catch (error) {
          setStatus(error.message || 'Failed to delete service.', 'error');
        }
      });
    });
    setStatus(`Loaded ${services.length} service${services.length === 1 ? '' : 's'}.`);
  } catch (error) {
    setStatus(error.message || 'Failed to load services.', 'error');
  }
}

async function handleServiceSubmit(event) {
  event.preventDefault();
  const service = {
    serviceName: document.getElementById('serviceName').value.trim(),
    description: document.getElementById('serviceDescription').value.trim() || null,
    port: Number(document.getElementById('servicePort').value),
    tags: makeTagString(document.getElementById('serviceTags').value),
    enabled: document.getElementById('serviceEnabled').checked
  };
  try {
    if (!service.serviceName) throw new Error('Service name is required.');
    if (!Number.isInteger(service.port) || service.port < 1 || service.port > 65535) throw new Error('Port must be between 1 and 65535.');
    const method = serviceId ? 'PUT' : 'POST';
    const path = serviceId ? `/api/services/${serviceId}` : '/api/services';
    await api(path, method, service);
    setStatus(serviceId ? 'Service updated.' : 'Service created.');
    serviceId = null;
    document.getElementById('serviceFormContainer').classList.add('hidden');
    await loadServices();
  } catch (error) {
    setStatus(error.message || 'Failed to save service.', 'error');
  }
}

function fillClientForm(client = null) {
  document.getElementById('clientName').value = client?.clientName || '';
  document.getElementById('clientDescription').value = client?.description || '';
  document.getElementById('clientIpAddress').value = client?.ipAddress || '';
  document.getElementById('clientTags').value = Array.isArray(client?.tags) ? client.tags.join(', ') : '';
  document.getElementById('clientEnabled').checked = client?.enabled !== false;
  document.getElementById('clientFormTitle').textContent = client ? 'Edit Client' : 'Add Client';
}

async function initClients() {
  wireButton('refreshBtn', loadClients);
  wireButton('newClientBtn', () => {
    document.getElementById('clientFormContainer').classList.remove('hidden');
    clientId = null;
    fillClientForm(null);
  });
  wireButton('cancelClientBtn', () => document.getElementById('clientFormContainer').classList.add('hidden'));
  document.getElementById('clientForm').addEventListener('submit', handleClientSubmit);
  await loadClients();
}

let clientId = null;

async function loadClients() {
  setStatus('Loading clients...');
  try {
    const clients = await api('/api/clients');
    const body = renderArrayRows(clients, client => `
      <tr>
        <td>${escapeHtml(client.clientName)}</td>
        <td>${escapeHtml(client.description)}</td>
        <td>${escapeHtml(client.ipAddress)}</td>
        <td><div class="tagList">${formatTags(client.tags)}</div></td>
        <td>${client.enabled ? 'Yes' : 'No'}</td>
        <td>
          <button type="button" data-id="${client.id}" class="editClientBtn">Edit</button>
          <button type="button" data-id="${client.id}" class="deleteClientBtn">Delete</button>
        </td>
      </tr>
    `);
    document.getElementById('clientRows').innerHTML = body;
    document.querySelectorAll('.editClientBtn').forEach(button => {
      button.addEventListener('click', () => {
        const id = button.dataset.id;
        const client = clients.find(c => c.id === id);
        if (!client) return;
        clientId = id;
        fillClientForm(client);
        document.getElementById('clientFormContainer').classList.remove('hidden');
      });
    });
    document.querySelectorAll('.deleteClientBtn').forEach(button => {
      button.addEventListener('click', async () => {
        if (!confirm('Delete this client?')) return;
        const id = button.dataset.id;
        try {
          await api(`/api/clients/${id}`, 'DELETE');
          setStatus('Client deleted.');
          await loadClients();
        } catch (error) {
          setStatus(error.message || 'Failed to delete client.', 'error');
        }
      });
    });
    setStatus(`Loaded ${clients.length} client${clients.length === 1 ? '' : 's'}.`);
  } catch (error) {
    setStatus(error.message || 'Failed to load clients.', 'error');
  }
}

async function handleClientSubmit(event) {
  event.preventDefault();
  const client = {
    clientName: document.getElementById('clientName').value.trim(),
    description: document.getElementById('clientDescription').value.trim() || null,
    ipAddress: document.getElementById('clientIpAddress').value.trim(),
    tags: makeTagString(document.getElementById('clientTags').value),
    enabled: document.getElementById('clientEnabled').checked
  };
  try {
    if (!client.clientName) throw new Error('Client name is required.');
    if (!client.ipAddress) throw new Error('IP address is required.');
    const method = clientId ? 'PUT' : 'POST';
    const path = clientId ? `/api/clients/${clientId}` : '/api/clients';
    await api(path, method, client);
    setStatus(clientId ? 'Client updated.' : 'Client created.');
    clientId = null;
    document.getElementById('clientFormContainer').classList.add('hidden');
    await loadClients();
  } catch (error) {
    setStatus(error.message || 'Failed to save client.', 'error');
  }
}

async function initConfig() {
  wireButton('refreshBtn', loadConfig);
  wireButton('downloadConfigBtn', downloadConfig);
  wireButton('uploadConfigBtn', uploadConfig);
  await loadConfig();
}

async function loadConfig() {
  if (document.getElementById('configurationJson')) {
    setStatus('Loading configuration...');
    try {
      const config = await api('/api/configuration');
      const json = JSON.stringify(config, null, 2);
      document.getElementById('configurationJson').textContent = json;
      document.getElementById('configurationFile').value = json;
      setStatus('Configuration loaded.');
    } catch (error) {
      setStatus(error.message || 'Failed to load configuration.', 'error');
    }
  }
}

function downloadConfig() {
  try {
    const json = document.getElementById('configurationJson').textContent;
    const blob = new Blob([json], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'conmon-configuration.json';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    setStatus('Configuration downloaded.');
  } catch (error) {
    setStatus(error.message || 'Failed to download configuration.', 'error');
  }
}

async function uploadConfig() {
  setStatus('Uploading configuration...');
  try {
    const raw = document.getElementById('configurationFile').value.trim();
    if (!raw) throw new Error('Configuration JSON is required.');
    const documentBody = JSON.parse(raw);
    const replaceExisting = document.getElementById('replaceExisting').checked;
    const config = await api(`/api/configuration?replaceExisting=${replaceExisting}`, 'POST', documentBody);
    const json = JSON.stringify(config, null, 2);
    document.getElementById('configurationJson').textContent = json;
    setStatus('Configuration uploaded successfully.');
  } catch (error) {
    setStatus(error.message || 'Failed to upload configuration.', 'error');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  if (!page) return;
  switch (page) {
    case 'status':
      initStatus();
      break;
    case 'connections':
      initConnections();
      break;
    case 'events':
      initEvents();
      break;
    case 'services':
      initServices();
      break;
    case 'clients':
      initClients();
      break;
    case 'config':
      initConfig();
      break;
    default:
      break;
  }
});
