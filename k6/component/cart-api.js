import http from 'k6/http';
import { check, sleep } from 'k6';
import { config } from '../config/environments.js';
import { componentThresholds } from '../config/thresholds.js';

export const options = {
  thresholds: componentThresholds,
  stages: [
    { duration: '15s', target: 5 },
    { duration: '30s', target: 5 },
    { duration: '15s', target: 0 },
  ],
  tags: { component: 'cart-api' },
};

export default function () {
  const patientId = Math.floor(Math.random() * 10) + 1;

  const response = http.get(
    `${config.baseUrl}/posts?userId=${patientId}`,
    { tags: { endpoint: 'cart-lookup' } }
  );

  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time under SLA': (r) => r.timings.duration < 500,
    'response is an array': (r) => Array.isArray(JSON.parse(r.body)),
  });

  sleep(1);
}
