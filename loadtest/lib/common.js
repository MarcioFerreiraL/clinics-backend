// Utilitários compartilhados pelos cenários k6.
import http from 'k6/http';
import { check } from 'k6';

export const BASE = __ENV.BASE_URL || 'http://localhost:8091';
export const API = `${BASE}/api/v1`;
const JSON_HEADERS = { 'Content-Type': 'application/json' };

// Termos reais de busca: nome clínico, nome popular e frases, como o paciente digita.
export const QUERIES = [
  'hemograma', 'ultrassom abdominal', 'raio x', 'colesterol', 'diabetes', 'esteira', 'chapa do braço',
  'ecografia', 'eletro', 'preventivo', 'pediatra', 'exame de sangue', 'quero fazer exame de colesterol',
  'tireoide', 'gravidez', 'pressão alta', 'consulta', '',
];
export const CATEGORIES = ['', 'Sangue', 'Raio-X', 'Consultas', 'Cardio', 'Ultrassom'];
export const SORTS = ['price', 'rating', 'name'];
export const NEIGHBORHOODS = ['', 'Centro', 'São José', 'Santa Rita', 'Bairro Novo', 'Cohab'];

export function pick(list) {
  return list[Math.floor(Math.random() * list.length)];
}

/** Descobre ids reais (ofertas, clínicas, tipos) para os cenários usarem. */
export function discover() {
  const res = http.get(`${API}/exams/search?size=50`);
  check(res, { 'descoberta: busca respondeu 200': (r) => r.status === 200 });
  const data = res.json('data');
  return {
    exams: data.map((e) => e.id),
    clinics: [...new Set(data.map((e) => e.clinic.id))],
    types: [...new Set(data.map((e) => e.exam_type.id))],
  };
}

export function searchUrl() {
  const p = [];
  const q = pick(QUERIES);
  if (q) p.push(`query=${encodeURIComponent(q)}`);
  const c = pick(CATEGORIES);
  if (c) p.push(`category=${encodeURIComponent(c)}`);
  p.push(`sort_by=${pick(SORTS)}`);
  const n = pick(NEIGHBORHOODS);
  if (n && Math.random() < 0.3) p.push(`neighborhood=${encodeURIComponent(n)}`);
  if (Math.random() < 0.2) p.push('max_price=150');
  if (Math.random() < 0.2) p.push('min_rating=4.5');
  return `${API}/exams/search?${p.join('&')}`;
}

/** CPF válido e aleatório (dígitos verificadores calculados). */
export function randomCpf() {
  const d = Array.from({ length: 9 }, () => Math.floor(Math.random() * 10));
  if (d.every((x) => x === d[0])) d[8] = (d[8] + 1) % 10;
  for (const n of [9, 10]) {
    let sum = 0;
    for (let i = 0; i < n; i++) sum += d[i] * (n + 1 - i);
    const rest = (sum * 10) % 11;
    d.push(rest === 10 ? 0 : rest);
  }
  return d.join('');
}

/** Cadastra um paciente novo e devolve { token }. */
export function registerPatient() {
  const n = () => String(Math.floor(Math.random() * 10000)).padStart(4, '0');
  const body = JSON.stringify({
    full_name: 'Paciente de Carga',
    phone: `(81) 9${n()}-${n()}`,
    cpf: randomCpf(),
    email: `carga${Date.now()}${Math.floor(Math.random() * 1e6)}@teste.com`,
    birth_date: '1990-05-20',
    password: 'Senha1234',
  });
  const res = http.post(`${API}/auth/register`, body, { headers: JSON_HEADERS, tags: { name: 'POST /auth/register' } });
  check(res, { 'cadastro 201': (r) => r.status === 201 });
  return res.status === 201 ? { token: res.json('token') } : null;
}

export function authHeaders(token) {
  return { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } };
}

/** Mistura de navegação pública, parecida com o uso real do app. */
export function browse(ids) {
  const r = Math.random();
  if (r < 0.60) {
    const res = http.get(searchUrl(), { tags: { name: 'GET /exams/search' } });
    check(res, { 'busca 200': (x) => x.status === 200 });
  } else if (r < 0.78) {
    const res = http.get(`${API}/exams/${pick(ids.exams)}`, { tags: { name: 'GET /exams/{id}' } });
    check(res, { 'detalhe 200': (x) => x.status === 200 });
  } else if (r < 0.88) {
    const res = http.get(`${API}/clinics/${pick(ids.clinics)}`, { tags: { name: 'GET /clinics/{id}' } });
    check(res, { 'clínica 200': (x) => x.status === 200 });
  } else if (r < 0.94) {
    const res = http.get(`${API}/exam-types/trending`, { tags: { name: 'GET /exam-types/trending' } });
    check(res, { 'em alta 200': (x) => x.status === 200 });
  } else {
    const res = http.get(`${API}/neighborhoods`, { tags: { name: 'GET /neighborhoods' } });
    check(res, { 'bairros 200': (x) => x.status === 200 });
  }
}

export const baseThresholds = {
  http_req_failed: ['rate<0.01'], // menos de 1% de erros
  checks: ['rate>0.99'],
};
