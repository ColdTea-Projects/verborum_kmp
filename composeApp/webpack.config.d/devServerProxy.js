// Makes local development same-origin, the way production is: the app, the API and Keycloak all
// answer on the dev server's own origin. Deployed, a reverse proxy serves `/api` and `/auth` next to
// the app; here the dev server does the same job, so `ApiConfig` and `AuthConfig` need no
// dev-only special case and no CORS is involved at any point.
//
// Without this the token exchange is a cross-origin XHR, which fails unless the Keycloak client
// lists the dev origin in its Web origins — a trap that changes every time the dev port does. The
// three backend services are worse still: they emit no CORS headers at all yet (backend phase 5),
// so a browser cannot reach them any other way.
//
// Targets match the Android debug build: Keycloak on 8180, ms_dictionary on 8085, ms_user on 8086,
// ms_marketplace on 8087. The path split under `/api` is the routing the API gateway will do, so the
// app's URLs do not change when it lands.
config.devServer = config.devServer || {};

// webpack-dev-server 5 takes an array here; the object-map form was removed. First match wins, so
// the two specific services come before the `/api` catch-all.
config.devServer.proxy = [
    {
        context: ['/auth'],
        target: 'http://localhost:8180',
        // Keycloak is mounted at the root of its own port, so the prefix is ours alone to strip.
        pathRewrite: {'^/auth': ''},
        changeOrigin: true,
    },
    {
        context: ['/api/users'],
        target: 'http://localhost:8086',
        pathRewrite: {'^/api': ''},
        changeOrigin: true,
    },
    {
        context: ['/api/marketplace'],
        target: 'http://localhost:8087',
        pathRewrite: {'^/api': ''},
        changeOrigin: true,
    },
    {
        context: ['/api'],
        target: 'http://localhost:8085',
        pathRewrite: {'^/api': ''},
        changeOrigin: true,
    },
];
