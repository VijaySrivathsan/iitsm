/* I-ITSM page behaviour: colour theme, active navigation link, flash messages, keyboard shortcut.
 * The theme is first applied by a tiny inline script in <head> (so there is no white flash);
 * this file only handles changing it. Charts listen for the "itsm:themechange" event. */
(function () {
    'use strict';

    var root = document.documentElement;
    var media = window.matchMedia('(prefers-color-scheme: dark)');

    function savedTheme() {
        try {
            var v = localStorage.getItem('itsm-theme');
            return v === 'light' || v === 'dark' ? v : null;
        } catch (e) {
            return null;
        }
    }

    function save(theme) {
        try { localStorage.setItem('itsm-theme', theme); } catch (e) { /* storage blocked: lasts for this page only */ }
    }

    // Until the user clicks the toggle, the theme follows the operating system.
    function wanted() {
        return savedTheme() || (media.matches ? 'dark' : 'light');
    }

    function updateButton(theme) {
        var next = theme === 'dark' ? 'light' : 'dark';
        document.querySelectorAll('#themeToggle').forEach(function (btn) {
            var label = 'Switch to ' + next + ' theme';
            btn.setAttribute('aria-label', label);
            btn.setAttribute('title', label);
            var icon = btn.querySelector('.theme-icon');
            if (icon) icon.className = 'bi theme-icon ' + (theme === 'dark' ? 'bi-sun' : 'bi-moon-stars');
        });
    }

    // Switch instantly: colour transitions are suspended for the one frame in which the theme changes,
    // so the page repaints once instead of animating every element.
    function apply(theme) {
        var changed = root.getAttribute('data-bs-theme') !== theme;
        if (changed) {
            root.classList.add('theme-switching');
            root.setAttribute('data-bs-theme', theme);
            requestAnimationFrame(function () {
                requestAnimationFrame(function () { root.classList.remove('theme-switching'); });
            });
        }
        root.setAttribute('data-theme-pref', savedTheme() || 'auto');
        updateButton(theme);
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

    var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Cards fade up one after another (the delay is read by the CSS animation)
    function staggerCards() {
        document.querySelectorAll('main .card, main .hero').forEach(function (el, i) {
            el.style.setProperty('--i', Math.min(i, 12));
        });
    }

    // Dashboard numbers count up from zero ("88%" keeps its suffix)
    function countUp() {
        if (reduceMotion) return;
        document.querySelectorAll('.stat').forEach(function (el) {
            var m = el.textContent.trim().match(/^(\d+)(\D*)$/);
            if (!m) return;
            var target = parseInt(m[1], 10), suffix = m[2], start = null, duration = 900;
            if (target === 0) return;
            el.textContent = '0' + suffix;
            function step(ts) {
                if (start === null) start = ts;
                var p = Math.min((ts - start) / duration, 1);
                var eased = 1 - Math.pow(1 - p, 3);
                el.textContent = Math.round(target * eased) + suffix;
                if (p < 1) requestAnimationFrame(step);
            }
            requestAnimationFrame(step);
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        apply(wanted());
        staggerCards();
        countUp();
        document.querySelectorAll('#themeToggle').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var next = root.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
                save(next);
                apply(next);
            });
        });
        media.addEventListener('change', function () {
            if (!savedTheme()) apply(wanted());
        });
        highlightActiveNav();
        autoDismissSuccess();
        searchShortcut();
    });

    // Shared helper for pages that draw charts
    window.itsmTheme = {
        current: function () { return root.getAttribute('data-bs-theme') || 'light'; },
        css: function (name) { return getComputedStyle(root).getPropertyValue(name).trim(); }
    };
})();
