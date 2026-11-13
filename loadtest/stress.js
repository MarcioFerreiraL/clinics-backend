// Estresse: aumenta a TAXA de requisições (sem pausas) até a API saturar, para achar o limite de capacidade.
// Os limites são frouxos de propósito: o objetivo é observar onde degrada, não aprovar.
import { discover, browse } from './lib/common.js';

export const options = {
  scenarios: {
    rampa: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      preAllocatedVUs: 200,
      maxVUs: 1500,
      stages: [
        { duration: '40s', target: 200 },
        { duration: '40s', target: 500 },
        { duration: '40s', target: 800 },
        { duration: '40s', target: 1200 },
        { duration: '40s', target: 1600 },
        { duration: '20s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1500'],
  },
};

export function setup() {
  return discover();
}

export default function (ids) {
  browse(ids);
}
