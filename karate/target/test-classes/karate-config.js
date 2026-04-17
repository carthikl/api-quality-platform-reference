function fn() {
    // karate.env is set at runtime via -Dkarate.env=<env>.
    // Defaults to 'dev' so local runs work without any configuration.
    var env = karate.env || 'dev';

    var config = { env: env };

    if (env === 'prod') {
        config.baseUrl   = 'https://api.walgreens.com';          // replace in deployment
        config.authToken = karate.properties['AUTH_TOKEN'] || '';
        config.timeoutMs = 10000;
    } else if (env === 'staging') {
        config.baseUrl   = 'https://staging-api.walgreens.com';  // replace in deployment
        config.authToken = karate.properties['AUTH_TOKEN'] || '';
        config.timeoutMs = 8000;
    } else {
        // dev — JSONPlaceholder, no auth required
        config.baseUrl   = java.lang.System.getProperty('api.base.url') ||
                           'https://jsonplaceholder.typicode.com';
        config.timeoutMs = 5000;
    }

    // CI override: -Dapi.base.url always wins over environment defaults.
    // This lets the pipeline point staging/prod env blocks at a different URL
    // without modifying this file.
    var urlOverride = java.lang.System.getProperty('api.base.url');
    if (urlOverride && env !== 'dev') {
        config.baseUrl = urlOverride;
    }

    karate.configure('connectTimeout', config.timeoutMs);
    karate.configure('readTimeout',    config.timeoutMs);

    karate.log('[Karate] Environment:', env, '| Base URL:', config.baseUrl);

    return config;
}
