// Stage 1 — Component (PR gate)
// Single endpoint, no integration overhead
// Strict and fast
export const componentThresholds = {
  http_req_duration: ['p95<500', 'p99<1000'],
  http_req_failed: ['rate<0.01'],
};

// Stage 2 — System (staging/scheduled)
// Full e2e journey across multiple services
// Realistic production thresholds
export const systemThresholds = {
  http_req_duration: ['p95<2000', 'p99<3000'],
  http_req_failed: ['rate<0.01'],
  http_reqs: ['rate>10'],
};

// Stress test — break point discovery
export const stressThresholds = {
  http_req_duration: ['p99<5000'],
  http_req_failed: ['rate<0.05'],
};
