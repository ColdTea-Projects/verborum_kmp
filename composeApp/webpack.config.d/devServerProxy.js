// Makes local development same-origin, the way production is: the app, the API and Keycloak all
// answer on the dev server's own origin. Deployed, a reverse proxy serves `/api` and `/auth` next to
// the app; here the dev server does the same job, so `ApiConfig` and `AuthConfig` need no
// dev-only special case and no CORS is involved at any point.
//
// Without this the token exchange is a cross-origin XHR, which fails unless the Keycloak client
// lists the dev origin in its Web origins — a trap that changes every time the dev port does.
//
// Targets match the Android debug build: Keycloak on 8180, ms_dictionary on 8085.
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
    {
        context: ['/api'],
        target: 'http://localhost:8085',
        pathRewrite: {'^/api': ''},
        changeOrigin: true,
    },
];
