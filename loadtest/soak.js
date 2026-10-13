// Resistência: carga moderada por bastante tempo (padrão 10 min; mude com DURATION=30m).
// Procura vazamento de memória e degradação ao longo do tempo.
import { sleep } from 'k6';
import { discover, browse, baseThresholds } from './lib/common.js';

export const options = {
  vus: 20,
  duration: __ENV.DURATION || '10m',
  thresholds: { ...baseThresholds, 'http_req_duration{name:GET /exams/search}': ['p(95)<300'] },
};

export function setup() {
  return discover();
}

export default function (ids) {
  browse(ids);
  sleep(Math.random() * 2 + 0.5);
}
