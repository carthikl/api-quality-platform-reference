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
  tags: { component: 'prescription-api' },
};

export default function () {
  const payload = JSON.stringify({
    userId: Math.floor(Math.random() * 10) + 1,
    title: 'Prescription Refill RX-' + Math.floor(Math.random() * 99999),
    body: 'Automated prescription refill request',
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'prescription-submit' },
  };

  const response = http.post(
    `${config.baseUrl}/posts`,
    payload,
    params
  );

  check(response, {
    'status is 201': (r) => r.status === 201,
    'response time under SLA': (r) => r.timings.duration < 500,
    'prescription id returned': (r) => JSON.parse(r.body).id !== undefined,
  });

  sleep(1);
}
