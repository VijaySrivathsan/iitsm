/* I-ITSM page behaviour: colour theme, active navigation link, flash messages, keyboard shortcut.
 * The theme is first applied by a tiny inline script in <head> (so there is no white flash);
 * this file only handles changing it. Charts listen for the "itsm:themechange" event. */
(function () {
    'use strict';

    var root = document.documentElement;
    var media = window.matchMedia('(prefers-color-scheme: dark)');
    var ICONS = { light: 'bi-sun', dark: 'bi-moon-stars', auto: 'bi-circle-half' };

    function readPref() {
        try {
            var v = localStorage.getItem('itsm-theme');
            return v === 'light' || v === 'dark' ? v : 'auto';
        } catch (e) {
            return 'auto';
        }
    }

    function savePref(pref) {
        try {
            if (pref === 'auto') localStorage.removeItem('itsm-theme');
            else localStorage.setItem('itsm-theme', pref);
        } catch (e) { /* storage blocked: the choice lasts for this page only */ }
    }

    function resolve(pref) {
        return pref === 'auto' ? (media.matches ? 'dark' : 'light') : pref;
    }

    function apply(pref) {
        var theme = resolve(pref);
        var changed = root.getAttribute('data-bs-theme') !== theme;
        root.setAttribute('data-bs-theme', theme);
        root.setAttribute('data-theme-pref', pref);
        document.querySelectorAll('.theme-icon').forEach(function (icon) {
            icon.className = 'bi theme-icon ' + ICONS[pref];
        });
        document.querySelectorAll('[data-theme-value]').forEach(function (btn) {
            var on = btn.getAttribute('data-theme-value') === pref;
            btn.classList.toggle('active-theme', on);
            btn.setAttribute('aria-pressed', on ? 'true' : 'false');
        });
        if (changed) {
            document.dispatchEvent(new CustomEvent('itsm:themechange', { detail: { theme: theme } }));
        }
    }

    function highlightActiveNav() {
        var path = window.location.pathname.replace(/\/+$/, '') || '/';
        var best = null;
        var bestLen = -1;
        document.querySelectorAll('.navbar a.nav-link[href], .navbar a.dropdown-item[href]').forEach(function (a) {
            if (a.getAttribute('href').charAt(0) === '#') return; // dropdown toggles, not pages
            var href = new URL(a.href, window.location.href).pathname.replace(/\/+$/, '') || '/';
            var matches = path === href || path.indexOf(href + '/') === 0;
            if (matches && href.length > bestLen) {
                best = a;
                bestLen = href.length;
            }
        });
        if (!best) return;
        best.classList.add('active');
        best.setAttribute('aria-current', 'page');
        var dropdown = best.closest('.dropdown');
        if (dropdown) {
            var toggle = dropdown.querySelector('.nav-link.dropdown-toggle');
            if (toggle) toggle.classList.add('active');
        }
    }

    function autoDismissSuccess() {
        document.querySelectorAll('.alert-success.alert-dismissible').forEach(function (el) {
            setTimeout(function () {
                if (window.bootstrap && document.body.contains(el)) {
                    window.bootstrap.Alert.getOrCreateInstance(el).close();
                }
            }, 6000);
        });
    }

    function searchShortcut() {
        document.addEventListener('keydown', function (e) {
            if (e.key !== '/' || e.ctrlKey || e.metaKey || e.altKey) return;
            var t = e.target;
            if (t && (t.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(t.tagName))) return;
            var box = document.querySelector('input[name="text"]');
            if (box) {
                e.preventDefault();
                box.focus();
                box.select();
            } else if (document.querySelector('.navbar a[href$="/search"]')) {
                e.preventDefault();
                window.location.href = document.querySelector('.navbar a[href$="/search"]').href;
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        apply(readPref());
        document.querySelectorAll('[data-theme-value]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var pref = btn.getAttribute('data-theme-value');
                savePref(pref);
                apply(pref);
            });
        });
        media.addEventListener('change', function () {
            if (readPref() === 'auto') apply('auto');
        });
        highlightActiveNav();
        autoDismissSuccess();
        searchShortcut();
        // enable colour transitions only after the first paint
        requestAnimationFrame(function () { root.classList.add('theme-ready'); });
    });

    // Shared helper for pages that draw charts
    window.itsmTheme = {
        current: function () { return root.getAttribute('data-bs-theme') || 'light'; },
        css: function (name) { return getComputedStyle(root).getPropertyValue(name).trim(); }
    };
})();
