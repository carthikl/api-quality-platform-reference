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
  tags: { component: 'patient-lookup' },
};

export default function () {
  const patientId = Math.floor(Math.random() * 10) + 1;

  const response = http.get(
    `${config.baseUrl}/users/${patientId}`,
    { tags: { endpoint: 'patient-lookup' } }
  );

  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time under SLA': (r) => r.timings.duration < 500,
    'patient id present': (r) => JSON.parse(r.body).id !== undefined,
    'patient name present': (r) => JSON.parse(r.body).name !== undefined,
  });

  sleep(1);
}
