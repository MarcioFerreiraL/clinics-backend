// Fumaça: 1 usuário por 30 s. Confirma que todos os fluxos principais respondem.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { API, BASE, discover, browse, registerPatient, authHeaders, baseThresholds } from './lib/common.js';

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: { ...baseThresholds, http_req_duration: ['p(95)<500'] },
};

export function setup() {
  return discover();
}

export default function (ids) {
  check(http.get(`${BASE}/actuator/health`), { 'saúde UP': (r) => r.status === 200 });
  browse(ids);
  const user = registerPatient();
  if (user) {
    check(http.get(`${API}/me`, authHeaders(user.token)), { 'perfil 200': (r) => r.status === 200 });
    check(http.get(`${API}/me/savings`, authHeaders(user.token)), { 'economia 200': (r) => r.status === 200 });
  }
  sleep(1);
}
