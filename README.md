# Shop-Floor Andon Dashboard

A lightweight web app for real-time shop-floor issue reporting and monitoring — built as the application vehicle for a 15-week DevOps pipeline project (Git → Jenkins CI/CD → Selenium → Docker → Ansible/Puppet).

## Problem
Machine breakdowns, material shortages, quality issues, and process delays are often reported manually on shop floors, leading to delayed response and poor visibility for supervisors. This app gives operators a way to log issues in real time and gives supervisors a live dashboard to track and resolve them.

## MVP Features
- **Event/data entry** — log an issue with station, issue type, severity, and notes
- **Searchable dashboard** — filter/search events by station, issue type, status, severity
- **Summary indicators** — live counts of open / in-progress / resolved / critical events
- **Status drill-down** — click any event to see full detail and update its status
- **Alert/exception view** — critical unresolved issues are surfaced at the top of the page

## Tech Stack
- Node.js + Express (backend REST API)
- Vanilla HTML/CSS/JS (frontend, no build step needed)
- JSON file storage (`data/events.json`) — lightweight for MVP

## Running Locally
```bash
npm install
npm start
```
Then open **http://localhost:8081**

## API
| Method | Route | Description |
|---|---|---|
| GET | `/api/events` | List events (supports `?search=&status=&severity=`) |
| GET | `/api/events/:id` | Get single event (drill-down) |
| POST | `/api/events` | Log a new event |
| PATCH | `/api/events/:id` | Update event status |
| GET | `/api/summary` | Summary counts for dashboard cards |

## Branching Strategy
- `main` — stable, release-ready code
- `develop` — integration branch for ongoing feature work
- `feature/*` — individual features, merged into `develop` via PR

## Project Status
See project board / weekly deliverables for current DevOps pipeline progress (Jenkins, Selenium, Docker, Ansible/Puppet stages).

## License
Academic project — for coursework purposes.
