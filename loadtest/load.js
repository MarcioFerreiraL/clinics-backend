// Carga nominal: sobe até 50 usuários simultâneos e mantém. Verifica o RNF03 (busca com p95 < 300 ms).
import http from 'k6/http';
import { check, sleep } from 'k6';
import { API, discover, browse, registerPatient, authHeaders, pick, baseThresholds } from './lib/common.js';

export const options = {
  scenarios: {
    visitantes: {
      executor: 'ramping-vus',
      exec: 'visitor',
      stages: [
        { duration: '30s', target: 30 },
        { duration: '2m', target: 30 },
        { duration: '20s', target: 0 },
      ],
    },
    pacientes: {
      executor: 'ramping-vus',
      exec: 'patient',
      stages: [
        { duration: '30s', target: 10 },
        { duration: '2m', target: 10 },
        { duration: '20s', target: 0 },
      ],
    },
  },
  thresholds: {
    ...baseThresholds,
    'http_req_duration{name:GET /exams/search}': ['p(95)<300', 'p(99)<800'], // RNF03
    'http_req_duration{name:GET /exams/{id}}': ['p(95)<300'],
    'http_req_duration{name:GET /clinics/{id}}': ['p(95)<300'],
    http_req_duration: ['p(95)<400'],
  },
};

export function setup() {
  return discover();
}

export function visitor(ids) {
  browse(ids);
  sleep(Math.random() * 2 + 0.5);
}

let session = null; // um cadastro por usuário virtual (BCrypt é caro, não faz sentido repetir)

export function patient(ids) {
  if (!session) session = registerPatient();
  if (!session) return sleep(2);
  const h = authHeaders(session.token);
  check(http.get(`${API}/me`, h), { 'perfil 200': (r) => r.status === 200 });
  browse(ids);
  const type = pick(ids.types);
  check(http.put(`${API}/me/favorites/${type}`, null, h), { 'favoritar 204': (r) => r.status === 204 });
  check(http.get(`${API}/me/favorites`, h), { 'favoritos 200': (r) => r.status === 200 });
  check(http.post(`${API}/exams/${pick(ids.exams)}/contacts`, null, h), { 'contato 201': (r) => r.status === 201 });
  check(http.get(`${API}/me/savings`, h), { 'economia 200': (r) => r.status === 200 });
  sleep(Math.random() * 2 + 1);
}
