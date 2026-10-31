// Makes the LOGIN same-origin, the way production is: the app and Keycloak answer on the dev server's own
// origin. Deployed, a reverse proxy serves `/auth` next to the app; here the dev server does the same job,
// so `AuthConfig` needs no dev-only special case.
//
// Without this the token exchange is a cross-origin XHR that needs a CORS preflight. Even proxied it carries an
// `Origin` header, and Keycloak 25+ refuses it (403 "Invalid origin") unless the `verborum-app` client lists
// this dev origin in its Web origins. The backend's realm import and `configure.sh` list http://localhost:8280:
// if the dev port (composeApp/build.gradle.kts) ever changes, change it there too.
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
    {
        // Everything else the login page links, from the ROOT of the page's origin rather than under `/auth`:
        //   /resources/<version>/login/<theme>/...   its stylesheets, scripts and fonts
        //   /realms/verborum/login-actions/...       the Register link, "Forgot password", "Try another way"
        //   /realms/verborum/broker/<idp>/login      the Google and Facebook buttons
        // Without this rule the dev server answers them with the app's index.html: the first login page shows
        // unstyled, and the links fail with "Cannot GET /realms/...". Keycloak serves them without a prefix.
        context: ['/resources', '/realms'],
        target: 'http://localhost:8180',
        changeOrigin: true,
    },
];
