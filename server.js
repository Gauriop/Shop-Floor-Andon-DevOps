const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 8081;
const DATA_FILE = path.join(__dirname, 'data', 'events.json');

app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// ---- Storage helpers ----
function loadEvents() {
  if (!fs.existsSync(DATA_FILE)) return [];
  return JSON.parse(fs.readFileSync(DATA_FILE, 'utf-8'));
}

function saveEvents(events) {
  fs.writeFileSync(DATA_FILE, JSON.stringify(events, null, 2));
}

// ---- API Routes ----

// Get all events (supports ?search= & ?status= & ?severity= query filters)
app.get('/api/events', (req, res) => {
  let events = loadEvents();
  const { search, status, severity } = req.query;

  if (search) {
    const q = search.toLowerCase();
    events = events.filter(e =>
      e.station.toLowerCase().includes(q) ||
      e.issueType.toLowerCase().includes(q) ||
      (e.notes || '').toLowerCase().includes(q)
    );
  }
  if (status) events = events.filter(e => e.status === status);
  if (severity) events = events.filter(e => e.severity === severity);

  events.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
  res.json(events);
});

// Get single event (drill-down)
app.get('/api/events/:id', (req, res) => {
  const events = loadEvents();
  const event = events.find(e => e.id === req.params.id);
  if (!event) return res.status(404).json({ error: 'Event not found' });
  res.json(event);
});

// Create new event (data entry)
app.post('/api/events', (req, res) => {
  const { station, issueType, severity, notes } = req.body;
  if (!station || !issueType || !severity) {
    return res.status(400).json({ error: 'station, issueType and severity are required' });
  }
  const events = loadEvents();
  const newEvent = {
    id: 'EVT-' + Date.now(),
    station,
    issueType,
    severity,          // Low | Medium | Critical
    status: 'Open',    // Open | In-Progress | Resolved
    notes: notes || '',
    timestamp: new Date().toISOString(),
    history: [{ status: 'Open', at: new Date().toISOString() }]
  };
  events.push(newEvent);
  saveEvents(events);
  res.status(201).json(newEvent);
});

// Update event status (resolve / progress)
app.patch('/api/events/:id', (req, res) => {
  const { status } = req.body;
  const events = loadEvents();
  const event = events.find(e => e.id === req.params.id);
  if (!event) return res.status(404).json({ error: 'Event not found' });
  event.status = status;
  event.history.push({ status, at: new Date().toISOString() });
  saveEvents(events);
  res.json(event);
});

// Summary indicators
app.get('/api/summary', (req, res) => {
  const events = loadEvents();
  const summary = {
    total: events.length,
    open: events.filter(e => e.status === 'Open').length,
    inProgress: events.filter(e => e.status === 'In-Progress').length,
    resolved: events.filter(e => e.status === 'Resolved').length,
    criticalOpen: events.filter(e => e.severity === 'Critical' && e.status !== 'Resolved').length
  };
  res.json(summary);
});

app.listen(PORT, () => {
  console.log(`Shop-Floor Andon Dashboard running at http://localhost:${PORT}`);
});
