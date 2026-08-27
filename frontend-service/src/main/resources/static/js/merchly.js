/* =========================================================================
   Merchly — общий фронтенд-слой.
   Подключается одной строкой из fragments/layout.html :: scripts.
   Отвечает только за поведение интерфейса: уведомления, счётчик корзины,
   появление блоков, «полёт» товара в корзину.
   ========================================================================= */

(function () {
    'use strict';

    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    /* ---------------------------------------------------------------------
       Уведомления. Заменяют alert(): не блокируют страницу и не прерывают
       действие пользователя.
       ------------------------------------------------------------------- */

    function getToastRail() {
        let rail = document.querySelector('.toast-rail');
        if (!rail) {
            rail = document.createElement('div');
            rail.className = 'toast-rail';
            rail.setAttribute('role', 'status');
            rail.setAttribute('aria-live', 'polite');
            document.body.appendChild(rail);
        }
        return rail;
    }

    function notify(message, kind) {
        const rail = getToastRail();
        const note = document.createElement('div');
        note.className = 'toast-note' + (kind ? ' toast-note--' + kind : '');

        const icon = document.createElement('i');
        icon.className = 'bi ' + (kind === 'error' ? 'bi-exclamation-octagon-fill' : 'bi-check-circle-fill');
        icon.setAttribute('aria-hidden', 'true');

        const text = document.createElement('span');
        text.textContent = message;

        note.append(icon, text);
        rail.appendChild(note);

        window.setTimeout(function () {
            note.classList.add('is-out');
            note.addEventListener('animationend', function () {
                note.remove();
            }, {once: true});
        }, 3600);
    }

    /* ---------------------------------------------------------------------
       Счётчик корзины в панели и в нижнем доке
       ------------------------------------------------------------------- */

    function setCartCount(count, stamp) {
        const targets = document.querySelectorAll('[data-cart-count]');
        targets.forEach(function (el) {
            el.textContent = count > 99 ? '99+' : String(count);
            el.hidden = count <= 0;
        });

        if (!stamp) {
            return;
        }

        const pill = document.querySelector('.cart-pill');
        if (pill) {
            pill.classList.remove('is-stamped');
            // Перезапуск анимации: браузеру нужен новый кадр
            void pill.offsetWidth;
            pill.classList.add('is-stamped');
        }
    }

    // Счётчик живёт в модуле: панель отрисовывается до ответа сервера
    let cartCount = 0;

    function readCartCount() {
        return cartCount;
    }

    function writeCartCount(count) {
        cartCount = Math.max(0, count);
    }

    // Кнопка корзины отрисовывается только вошедшим пользователям —
    // её наличие и есть признак того, что корзину можно запрашивать.
    function isAuthenticated() {
        return !!document.querySelector('.cart-pill');
    }

    function refreshCartCount() {
        if (!isAuthenticated()) {
            return;
        }

        fetch('/cart/count', {headers: {'Accept': 'application/json'}})
            .then(function (response) {
                return response.ok ? response.json() : null;
            })
            .then(function (data) {
                if (data && typeof data.count === 'number') {
                    writeCartCount(data.count);
                    setCartCount(data.count, false);
                }
            })
            .catch(function () {
                /* Счётчик — вспомогательная информация: молча пропускаем сбой */
            });
    }

    /* ---------------------------------------------------------------------
       «Полёт в корзину» — изображение товара по дуге уходит в панель.
       Показывает, что именно добавлено и куда оно попало.
       ------------------------------------------------------------------- */

    function flyToCart(sourceImage) {
        const target = document.querySelector('.cart-pill');
        if (reducedMotion || !sourceImage || !target || !sourceImage.getBoundingClientRect) {
            return;
        }

        const from = sourceImage.getBoundingClientRect();
        const to = target.getBoundingClientRect();
        if (!from.width || !to.width) {
            return;
        }

        const clone = document.createElement('img');
        clone.src = sourceImage.currentSrc || sourceImage.src;
        clone.className = 'flying-item';
        clone.alt = '';
        clone.style.left = from.left + 'px';
        clone.style.top = from.top + 'px';
        clone.style.width = from.width + 'px';
        clone.style.height = from.height + 'px';
        document.body.appendChild(clone);

        const dx = (to.left + to.width / 2) - (from.left + from.width / 2);
        const dy = (to.top + to.height / 2) - (from.top + from.height / 2);

        // Подъём в середине пути превращает прямую в дугу
        const lift = Math.min(160, Math.abs(dy) * 0.45 + 60);

        const animation = clone.animate([
            {transform: 'translate(0, 0) scale(1)', opacity: 1},
            {
                transform: 'translate(' + dx * 0.5 + 'px, ' + (dy * 0.5 - lift) + 'px) scale(0.6)',
                opacity: 0.95,
                offset: 0.55
            },
            {transform: 'translate(' + dx + 'px, ' + dy + 'px) scale(0.08)', opacity: 0.2}
        ], {
            duration: 720,
            easing: 'cubic-bezier(0.4, 0, 0.2, 1)'
        });

        animation.onfinish = function () {
            clone.remove();
        };
    }

    /* ---------------------------------------------------------------------
       Добавление товара в корзину (каталог и карточка товара)
       ------------------------------------------------------------------- */

    const CSRF_COOKIE = 'XSRF-TOKEN';
    const CSRF_HEADER = 'X-XSRF-TOKEN';
    const CSRF_PARAM = '_csrf';

    function csrfToken() {
        const match = document.cookie.match(new RegExp('(^|; )' + CSRF_COOKIE + '=([^;]*)'));
        return match ? decodeURIComponent(match[2]) : null;
    }

    function csrfHeaders(base) {
        const headers = base || {};
        const token = csrfToken();

        if (token) {
            headers[CSRF_HEADER] = token;
        }
        return headers;
    }

    function injectCsrfIntoForms(root) {
        const token = csrfToken();
        if (!token) {
            return;
        }

        (root || document).querySelectorAll('form').forEach(function (form) {
            const method = (form.getAttribute('method') || 'get').toLowerCase();
            if (method !== 'post') {
                return;
            }

            if ((form.getAttribute('enctype') || '').toLowerCase() === 'multipart/form-data') {
                const action = form.getAttribute('action') || window.location.pathname;
                if (!/[?&]_csrf=/.test(action)) {
                    form.setAttribute('action',
                        action + (action.indexOf('?') === -1 ? '?' : '&')
                        + CSRF_PARAM + '=' + encodeURIComponent(token));
                }
                return;
            }

            let field = form.querySelector('input[name="' + CSRF_PARAM + '"]');
            if (!field) {
                field = document.createElement('input');
                field.type = 'hidden';
                field.name = CSRF_PARAM;
                form.appendChild(field);
            }
            field.value = token;
        });
    }

    async function addToCart(button) {
        const productId = button.dataset.productId;
        const originalHtml = button.innerHTML;

        button.disabled = true;
        button.innerHTML = '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span>' +
            '<span class="visually-hidden">Добавляем</span>';

        try {
            const response = await fetch('/cart/add', {
                method: 'POST',
                headers: csrfHeaders({'Content-Type': 'application/json'}),
                body: JSON.stringify({productId: productId, quantity: 1})
            });

            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }

            // Источник «полёта» — изображение товара. В каталоге оно лежит
            // в той же карточке, на странице товара — в соседней колонке.
            const scope = button.closest('.card');
            const image = (scope && scope.querySelector('[data-cart-image]'))
                || document.querySelector('[data-cart-image]');
            flyToCart(image);

            const next = readCartCount() + 1;
            writeCartCount(next);
            setCartCount(next, true);

            button.classList.add('is-done');
            button.innerHTML = '<i class="bi bi-check2" aria-hidden="true"></i> В корзине';

            window.setTimeout(function () {
                button.classList.remove('is-done');
                button.innerHTML = originalHtml;
                button.disabled = false;
            }, 1600);
        } catch (error) {
            console.error('Не удалось добавить товар в корзину:', error);
            notify('Товар не добавлен в корзину. Попробуйте ещё раз.', 'error');
            button.innerHTML = originalHtml;
            button.disabled = false;
        }
    }

    /* ---------------------------------------------------------------------
       Появление блоков при прокрутке
       ------------------------------------------------------------------- */

    function initReveal() {
        const items = document.querySelectorAll('.reveal');
        if (!items.length) {
            return;
        }

        if (reducedMotion || !('IntersectionObserver' in window)) {
            items.forEach(function (item) {
                item.classList.add('is-in');
            });
            return;
        }

        const observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) {
                    return;
                }
                entry.target.classList.add('is-in');
                observer.unobserve(entry.target);
            });
        }, {rootMargin: '0px 0px -8% 0px', threshold: 0.05});

        items.forEach(function (item, index) {
            // Каскад внутри одной группы: сетка проявляется по очереди
            const group = item.dataset.revealGroup;
            const step = group ? index % 9 : Math.min(index, 6);
            item.style.setProperty('--reveal-delay', (step * 55) + 'ms');
            observer.observe(item);
        });
    }

    /* ---------------------------------------------------------------------
       Состояние верхней панели при прокрутке
       ------------------------------------------------------------------- */

    function initRail() {
        const rail = document.querySelector('.top-rail');
        if (!rail) {
            return;
        }

        let ticking = false;
        const update = function () {
            rail.classList.toggle('is-scrolled', window.scrollY > 8);
            ticking = false;
        };

        update();
        window.addEventListener('scroll', function () {
            if (!ticking) {
                ticking = true;
                window.requestAnimationFrame(update);
            }
        }, {passive: true});
    }

    /* ---------------------------------------------------------------------
       Запуск
       ------------------------------------------------------------------- */

    function init() {
        initRail();
        initReveal();
        injectCsrfIntoForms();
        refreshCartCount();

        document.addEventListener('click', function (event) {
            const button = event.target.closest('[data-add-to-cart]');
            if (button) {
                event.preventDefault();
                addToCart(button);
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    window.Merchly = {
        notify: notify,
        setCartCount: setCartCount,
        readCartCount: readCartCount,
        writeCartCount: writeCartCount,
        csrfHeaders: csrfHeaders,
        csrfToken: csrfToken,
        injectCsrfIntoForms: injectCsrfIntoForms,
        flyToCart: flyToCart
    };
})();
