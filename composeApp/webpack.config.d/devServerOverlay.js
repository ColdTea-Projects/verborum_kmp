// "ResizeObserver loop completed with undelivered notifications" is a benign browser notice: the
// observer callback changed layout, so the browser defers the rest to the next frame. Compose
// renders into a canvas it observes for size, so this fires on the first layout after a reload —
// including the reload the OAuth redirect causes. Nothing is broken, but webpack-dev-server's
// overlay reports every window error as a full-screen crash, which is misleading.
//
// Dev server only: the production bundle has no overlay, so this file cannot hide a real error from
// users. Every other runtime error still shows.
config.devServer = config.devServer || {};
config.devServer.client = Object.assign({}, config.devServer.client, {
    overlay: {
        errors: true,
        warnings: false,
        runtimeErrors: (error) =>
            !(error &&
                typeof error.message === 'string' &&
                error.message.includes('ResizeObserver loop completed')),
    },
});
