// Makes the LOGIN same-origin, the way production is: the app and Keycloak answer on the dev server's own
// origin. Deployed, a reverse proxy serves `/auth` next to the app; here the dev server does the same job,
// so `AuthConfig` needs no dev-only special case.
//
// Without this the token exchange is a cross-origin XHR, which fails unless the Keycloak client lists the
// dev origin in its Web origins — a trap that changes every time the dev port does.
//
// The API is NOT proxied any more (roadmap P5-10). The app calls the local `ms_gateway` on :8080 directly,
// and the gateway answers CORS for this dev origin (`CORS_ALLOWED_ORIGINS`, default http://localhost:8280).
// See `core:network` ApiConfig.web.kt.
config.devServer = config.devServer || {};

// webpack-dev-server 5 takes an array here; the object-map form was removed.
config.devServer.proxy = [
    {
        context: ['/auth'],
        target: 'http://localhost:8180',
        // Keycloak is mounted at the root of its own port, so the prefix is ours alone to strip.
        pathRewrite: {'^/auth': ''},
        changeOrigin: true,
    },
];
