// Latência real em produção: SOMENTE leitura (não cadastra nem grava nada) e carga baixa,
// para não incomodar a VPS. Uso: loadtest/run.sh latency https://clinics.marciodev.com
import http from 'k6/http';
import { check, sleep } from 'k6';
import { API, discover, searchUrl, pick } from './lib/common.js';

export const options = {
  vus: 5,
  duration: '40s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:GET /exams/search}': ['p(95)<500'], // inclui rede + Cloudflare
  },
};

export function setup() {
  return discover();
}

export default function (ids) {
  check(http.get(searchUrl(), { tags: { name: 'GET /exams/search' } }), { 'busca 200': (r) => r.status === 200 });
  check(http.get(`${API}/exams/${pick(ids.exams)}`, { tags: { name: 'GET /exams/{id}' } }), { 'detalhe 200': (r) => r.status === 200 });
  check(http.get(`${API}/clinics/${pick(ids.clinics)}`, { tags: { name: 'GET /clinics/{id}' } }), { 'clínica 200': (r) => r.status === 200 });
  sleep(1);
}
