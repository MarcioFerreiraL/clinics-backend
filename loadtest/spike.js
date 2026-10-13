// Pico: salto súbito de tráfego (ex.: divulgação nas redes), e volta ao normal. Mede a recuperação.
import { sleep } from 'k6';
import { discover, browse } from './lib/common.js';

export const options = {
  stages: [
    { duration: '20s', target: 10 },
    { duration: '10s', target: 150 }, // pico
    { duration: '1m', target: 150 },
    { duration: '10s', target: 10 }, // queda
    { duration: '1m', target: 10 }, // recuperação: a latência deve voltar ao normal
    { duration: '10s', target: 0 },
  ],
  thresholds: { http_req_failed: ['rate<0.05'] },
};

export function setup() {
  return discover();
}

export default function (ids) {
  browse(ids);
  sleep(Math.random() + 0.2);
}
