// Stress test — identifies break point and
// recovery behavior. Run scheduled, not on every
// deployment. Results inform capacity planning
// and auto-scaling thresholds.

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { config } from '../config/environments.js';
import { stressThresholds } from '../config/thresholds.js';

export const options = {
  thresholds: stressThresholds,
  stages: [
    { duration: '2m', target: 50 },
    { duration: '3m', target: 100 },
    { duration: '2m', target: 200 },
    { duration: '2m', target: 200 },
    { duration: '1m', target: 0 },
  ],
};

export default function () {
  const patientId = Math.floor(Math.random() * 10) + 1;

  group('Patient Record Lookup', function () {
    const r = http.get(
      `${config.baseUrl}/users/${patientId}`,
      { tags: { step: 'patient-lookup' } }
    );
    check(r, {
      'patient lookup 200': (r) => r.status === 200,
      'patient lookup SLA': (r) => r.timings.duration < 2000,
    });
    sleep(Math.random() * 2 + 1);
  });

  group('Prescription History', function () {
    const r = http.get(
      `${config.baseUrl}/posts?userId=${patientId}`,
      { tags: { step: 'prescription-history' } }
    );
    check(r, {
      'history 200': (r) => r.status === 200,
      'history SLA': (r) => r.timings.duration < 2000,
    });
    sleep(Math.random() * 2 + 1);
  });

  group('Submit Prescription Refill', function () {
    const payload = JSON.stringify({
      userId: patientId,
      title: 'Prescription Refill RX-' + Math.floor(Math.random() * 99999),
      body: 'Refill request',
    });
    const r = http.post(
      `${config.baseUrl}/posts`,
      payload,
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { step: 'prescription-submit' },
      }
    );
    check(r, {
      'submit 201': (r) => r.status === 201,
      'submit SLA': (r) => r.timings.duration < 3000,
    });
    sleep(Math.random() * 2 + 1);
  });
}
