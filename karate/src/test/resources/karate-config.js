function fn() {
    // karate.env is set via -Dkarate.env=<env> at runtime.
    // Defaults to 'dev' for local runs.
    var env = karate.env || 'dev';

    // api.base.url system property takes precedence over env block defaults.
    // This allows CI to inject the URL without modifying this file.
    var apiBaseUrl = java.lang.System.getProperty('api.base.url');

    var config = {
        env: env,
        baseUrl: apiBaseUrl || 'https://jsonplaceholder.typicode.com'
    };

    if (env === 'staging') {
        config.authToken = karate.properties['AUTH_TOKEN'] || '';
        config.timeoutMs = 10000;
    } else if (env === 'ci') {
        config.timeoutMs = 8000;
    } else {
        // dev / local
        config.timeoutMs = 5000;
    }

    karate.configure('connectTimeout', config.timeoutMs);
    karate.configure('readTimeout', config.timeoutMs);

    return config;
}
