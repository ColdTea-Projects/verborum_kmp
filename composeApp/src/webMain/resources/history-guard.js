// Keeps `window.history.state` either null or a string, for as long as the page lives.
//
// Why: Compose Navigation's browser binding declares the history state as `String?` and compares it
// without checking. Any other value in there - an object some script or page left - makes the next
// navigation throw "bad cast" (Chrome: "illegal cast"), after which the app's tabs stop responding
// until the tab is closed. The app only ever writes strings, so a value that is not text is never ours.
//
// Loaded before the app (index.html) and as a separate file, not inline, so the CSP's
// `script-src 'self'` still holds. It is data-only: no eval, no markup, nothing from the network.
(function () {
    'use strict';

    // Loaded twice (a cached copy, a second tag) must not wrap the history twice.
    if (window.__verborumHistoryGuard) { return; }
    window.__verborumHistoryGuard = true;

    var replace = History.prototype.replaceState;
    var push = History.prototype.pushState;

    function foreign(state) {
        return state !== null && typeof state !== 'string';
    }

    // Visible on purpose: if this ever fires, something is putting objects into the history and it
    // should be found, not just hidden.
    function report(where, state) {
        console.warn('Verborum: removed a history state that is not text (' + where + ')', state);
    }

    function clean(where) {
        if (foreign(history.state)) {
            report(where, history.state);
            replace.call(history, null, '', location.href);
        }
    }

    // The last few writes, kept only to explain a failure (see the error listener at the end).
    var writes = [];
    function remember(kind, state, url) {
        writes.push(kind + ' ' + typeof state + ' ' + String(url));
        if (writes.length > 6) { writes.shift(); }
    }

    // What this page writes itself.
    history.replaceState = function (state, title, url) {
        remember('replace', state, url);
        if (foreign(state)) {
            report('replaceState', state);
            state = null;
        }
        return replace.call(history, state, title, url);
    };
    history.pushState = function (state, title, url) {
        remember('push', state, url);
        if (foreign(state)) {
            report('pushState', state);
            state = null;
        }
        return push.call(history, state, title, url);
    };

    // What was already there when the page started, or came back from the back/forward cache.
    clean('start');
    window.addEventListener('pageshow', function () { clean('pageshow'); });

    // A traversal onto an entry another page wrote: swallow the event before the navigation binding
    // reads it, and fix the entry.
    window.addEventListener('popstate', function (event) {
        if (foreign(event.state)) {
            event.stopImmediatePropagation();
            clean('popstate');
        }
    }, true);

    // The last line of defence: a state planted any other way is removed before the click or key
    // press that would navigate on top of it.
    ['pointerdown', 'keydown'].forEach(function (type) {
        window.addEventListener(type, function () { clean(type); }, true);
    });

    // If the navigation binding fails anyway, say what the history looked like. This is what tells a
    // foreign state apart from some other cause.
    window.addEventListener('error', function (event) {
        if (/cast/i.test(String(event.message))) {
            var state = history.state;
            console.warn('Verborum: a cast failed. history.state is ' + typeof state + ': ' +
                (typeof state === 'string' ? JSON.stringify(state) : String(state)) +
                ' | entries: ' + history.length + ' | url: ' + location.href +
                ' | last history writes: ' + writes.join(' ; '));
        }
    });
})();
