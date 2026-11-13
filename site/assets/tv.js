/* Oculo TV: a drawn, working copy of the app's interface for the website.
   Everything on screen is invented (channels, titles, pictures): nothing comes from YouTube.
   The stage is authored at 1920 x 1080 px, with the app's own measurements, and scaled to its container.
   Language follows <html lang> (en or fr). */
(function () {
  'use strict';

  var BASE = new URL('thumbs/', document.currentScript.src).href;
  var LANG = (document.documentElement.lang || 'en').slice(0, 2) === 'fr' ? 'fr' : 'en';

  var T = {
    en: {
      label: 'The Oculo home screen on a TV', search: 'Search', live: 'LIVE', skipped: 'Skipped: Sponsor', who: 'Alex',
      rail: ['Search', 'Home', 'Subscriptions', 'Live', 'Music', 'History', 'Playlists', 'Settings'],
      rows: ['Recommended', 'Continue watching', 'Subscriptions'],
      scan: 'Scan the code with your phone', enter: 'or enter this code on the sign-in page', made: 'This QR code is made here, on the TV.',
      locale: 'en-US'
    },
    fr: {
      label: 'L’écran d’accueil d’Oculo sur une télé', search: 'Rechercher', live: 'EN DIRECT', skipped: 'Segment passé : Commandite', who: 'Alex',
      rail: ['Rechercher', 'Accueil', 'Abonnements', 'En direct', 'Musique', 'Historique', 'Listes', 'Réglages'],
      rows: ['Recommandé', 'Continuer à regarder', 'Abonnements'],
      scan: 'Balayez le code avec votre téléphone', enter: 'ou entrez ce code sur la page de connexion', made: 'Ce code QR est fabriqué ici, sur la télé.',
      locale: 'fr-CA'
    }
  }[LANG];

  /* glow = the color the app's thumbnail glow would pick for the picture. */
  var V = {
    aurora:   { glow: '#5fe69a', len: '18:24', recent: true,
      en: ['Northern lights: a full night in Lapland', 'Open Sky', '412K views', '3 hours ago'],
      fr: ['Aurores boréales : une nuit complète en Laponie', 'Ciel ouvert', '412 k vues', 'il y a 3 h'] },
    ramen:    { glow: '#ea9b3e', len: '12:07',
      en: ['Homemade ramen in 40 minutes', 'The Little Pot', '1.2M views', '2 days ago'],
      fr: ['Un ramen maison en 40 minutes', 'La petite marmite', '1,2 M de vues', 'il y a 2 jours'] },
    reef:     { glow: '#23aeea', len: '42:10',
      en: ['Below the surface: the reef at dawn', 'Deep Blue', '96K views', '5 days ago'],
      fr: ['Sous la surface : le récif à l’aube', 'Grand Bleu', '96 k vues', 'il y a 5 jours'] },
    rocket:   { glow: '#4f86ea', live: true,
      en: ['Liftoff, live from the launch pad', 'Orbit', '14K watching'],
      fr: ['Décollage en direct depuis le pas de tir', 'Orbite', '14 k spectateurs'] },
    canyon:   { glow: '#ea5f3e', len: '9:48',
      en: ['Red canyon descent, helmet cam', 'Two Wheels', '230K views', '1 month ago'],
      fr: ['Descente du canyon rouge, caméra embarquée', 'Deux roues', '230 k vues', 'il y a 1 mois'] },
    vinyl:    { glow: '#d24fea', len: '1:04:30', recent: true,
      en: ['Vinyl session: late-night jazz', 'Side B', '58K views', '20 hours ago'],
      fr: ['Session vinyle, jazz de fin de soirée', 'Face B', '58 k vues', 'il y a 20 h'] },
    workshop: { glow: '#eab63a', len: '27:15',
      en: ['Building an oak chair, step by step', 'The Workbench', '310K views', '3 weeks ago'],
      fr: ['Fabriquer une chaise en chêne, étape par étape', 'L’établi', '310 k vues', 'il y a 3 semaines'] },
    city:     { glow: '#ea5fa2', len: '6:02',
      en: ['The city at night, in time-lapse', 'Long Exposure', '74K views', '6 days ago'],
      fr: ['La ville de nuit en accéléré', 'Longue pose', '74 k vues', 'il y a 6 jours'] }
  };

  var ROWS = [
    { items: ['aurora', 'ramen', 'reef', 'rocket', 'canyon', 'workshop', 'city'] },
    { items: ['vinyl', 'workshop', 'reef', 'city'], progress: { vinyl: 34, workshop: 62, reef: 81, city: 18 } },
    { items: ['canyon', 'city', 'ramen', 'vinyl', 'aurora', 'rocket'] }
  ];

  var ICON = {
    search: '<circle cx="10.5" cy="10.5" r="6.2" fill="none" stroke="currentColor" stroke-width="2.4"/><path d="M15.2 15.2 21 21" stroke="currentColor" stroke-width="2.6" stroke-linecap="round"/>',
    home: '<path fill="currentColor" d="M3 11.2 12 3.5l9 7.7V20a1 1 0 0 1-1 1h-5.2v-6.2H9.2V21H4a1 1 0 0 1-1-1z"/>',
    subs: '<path fill="currentColor" d="M3.5 9h17a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1h-17a1 1 0 0 1-1-1V10a1 1 0 0 1 1-1zm6.3 3.2v5.6l5-2.8z"/><path d="M5.5 6h13M7.5 3.2h9" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>',
    live: '<circle cx="12" cy="12" r="2.6" fill="currentColor"/><path d="M7.4 7.4a6.5 6.5 0 0 0 0 9.2M16.6 7.4a6.5 6.5 0 0 1 0 9.2M4.4 4.6a10.5 10.5 0 0 0 0 14.8M19.6 4.6a10.5 10.5 0 0 1 0 14.8" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>',
    music: '<circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2.2"/><circle cx="12" cy="12" r="3" fill="currentColor"/>',
    history: '<path d="M4.2 12a7.8 7.8 0 1 0 2.6-5.8" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/><path d="M3.4 4.2v3.6H7M12 8v4.4l3 1.8" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>',
    lists: '<path d="M4 6.5h11M4 11.5h11M4 16.5h7" stroke="currentColor" stroke-width="2.3" stroke-linecap="round"/><path fill="currentColor" d="M15.5 13.5v7l5.5-3.5z"/>',
    gear: '<circle cx="12" cy="12" r="3.1" fill="none" stroke="currentColor" stroke-width="2.2"/><circle cx="12" cy="12" r="7.6" fill="none" stroke="currentColor" stroke-width="3.4" stroke-dasharray="3.2 2.77"/>',
    mic: '<rect x="9" y="3" width="6" height="11" rx="3" fill="currentColor"/><path d="M6 11.5a6 6 0 0 0 12 0M12 17.5V21" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>',
    person: '<circle cx="12" cy="9" r="4" fill="currentColor"/><path fill="currentColor" d="M4 21a8 8 0 0 1 16 0z"/>',
    prev: '<path fill="currentColor" d="M6 5h2.4v14H6zM19 5v14L9.5 12z"/>',
    pause: '<path fill="currentColor" d="M6.5 5h4v14h-4zM13.5 5h4v14h-4z"/>',
    next: '<path fill="currentColor" d="M15.6 5H18v14h-2.4zM5 5v14l9.5-7z"/>',
    cc: '<rect x="3" y="5" width="18" height="14" rx="2.5" fill="none" stroke="currentColor" stroke-width="2"/><path d="M10.5 10.5a2.2 2.2 0 1 0 0 3M17 10.5a2.2 2.2 0 1 0 0 3" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>',
    speed: '<path d="M4.5 16a8 8 0 1 1 15 0" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/><path d="M12 14.5 16 9" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>'
  };
  var RAIL = ['search', 'home', 'subs', 'live', 'music', 'history', 'lists', 'gear'];
  function icon(name) { return '<svg viewBox="0 0 24 24" aria-hidden="true">' + ICON[name] + '</svg>'; }

  /* The wordmark is the page's own logo (the approved SVG, inlined in the header), reused as is. */
  var copies = 0;
  function wordmark() {
    var src = document.querySelector('[data-oculo-logo] svg');
    if (!src) return '';
    var id = 'tvw' + (++copies);
    return src.outerHTML.replace(/\bod([fsgc])\b/g, id + '$1').replace(/ role="img" aria-label="[^"]*"/, ' aria-hidden="true"');
  }

  function h(tag, cls, html) {
    var n = document.createElement(tag);
    if (cls) n.className = cls;
    if (html != null) n.innerHTML = html;
    return n;
  }

  function card(id, row) {
    var v = V[id], s = v[LANG], c = h('div', 'tv-card');
    c.dataset.glow = v.glow;
    var meta = s[1] + ' • ' + (v.live ? '<b>' + s[2] + '</b>' : s[2] + ' • ' + (v.recent ? '<b>' + s[3] + '</b>' : s[3]));
    var badge = v.live ? '<span class="tv-badge live">' + T.live + '</span>' : '<span class="tv-badge">' + v.len + '</span>';
    var prog = row.progress && row.progress[id] != null ? '<span class="tv-watched"><i style="width:' + row.progress[id] + '%"></i></span>' : '';
    c.innerHTML = '<div class="tv-thumb" style="background-image:url(' + BASE + id + '.webp)">' + badge + prog + '</div>' +
      '<div class="tv-text"><p class="tv-title">' + s[0] + '</p><p class="tv-meta">' + meta + '</p></div>';
    return c;
  }

  /* A QR-looking pattern (finder squares and noise). It is a drawing: it encodes nothing. */
  function qr() {
    var n = 29, s = 0x9e3779b1, out = '', x, y;
    function rnd() { s ^= s << 13; s ^= s >>> 17; s ^= s << 5; return ((s >>> 0) % 1000) / 1000; }
    function inF(fx, fy) { return x >= fx && x < fx + 7 && y >= fy && y < fy + 7; }
    function onF(fx, fy) { var dx = x - fx, dy = y - fy; return dx === 0 || dx === 6 || dy === 0 || dy === 6 || (dx > 1 && dx < 5 && dy > 1 && dy < 5); }
    for (y = 0; y < n; y++) for (x = 0; x < n; x++) {
      var on;
      if (inF(0, 0)) on = onF(0, 0);
      else if (inF(n - 7, 0)) on = onF(n - 7, 0);
      else if (inF(0, n - 7)) on = onF(0, n - 7);
      else if ((x < 8 && y < 8) || (x > n - 9 && y < 8) || (x < 8 && y > n - 9)) on = false;
      else on = rnd() > 0.52;
      if (on) out += '<rect x="' + x + '" y="' + y + '" width="1.02" height="1.02"/>';
    }
    return '<svg viewBox="-4 -4 ' + (n + 8) + ' ' + (n + 8) + '" aria-hidden="true"><rect x="-4" y="-4" width="' + (n + 8) + '" height="' + (n + 8) + '" fill="#fff"/><g fill="#000">' + out + '</g></svg>';
  }

  function OculoTV(root) {
    var self = this, R = V.ramen[LANG];
    this.root = root;
    this.row = 0;
    this.cols = ROWS.map(function () { return 0; });
    this.rail = false;
    this.timer = null;
    this.interactive = root.dataset.keys !== '0';
    root.classList.add('tv');
    if (this.interactive) root.tabIndex = 0;
    root.setAttribute('role', 'img');
    if (!root.getAttribute('aria-label')) root.setAttribute('aria-label', T.label);

    var stage = this.stage = h('div', 'tv-stage');
    stage.innerHTML =
      '<div class="tv-glow"></div>' +
      '<div class="tv-scene tv-home">' +
        '<div class="tv-rows"></div>' +
        '<div class="tv-rail"><span class="tv-avatar">' + icon('person') + '</span><span class="tv-who">' + T.who + '</span><div class="tv-list">' +
          RAIL.map(function (k, i) { return '<span class="tv-item' + (k === 'home' ? ' cur' : k === 'subs' ? ' dot' : '') + '">' + icon(k) + '<em>' + T.rail[i] + '</em></span>'; }).join('') + '</div></div>' +
        '<div class="tv-top"><span class="tv-mic">' + icon('mic') + '</span><span class="tv-pill">' + T.search + '</span>' +
          '<span class="tv-clock"><b></b><i></i></span><span class="tv-wm">' + wordmark() + '</span></div>' +
      '</div>' +
      '<div class="tv-scene tv-player">' +
        '<div class="tv-frame" style="background-image:url(' + BASE + 'ramen-wide.webp)"></div>' +
        '<div class="tv-shade"></div>' +
        '<div class="tv-skip"><i></i>' + T.skipped + '</div>' +
        '<p class="tv-ptitle">' + R[0] + '<span>' + R[1] + '</span></p>' +
        '<div class="tv-seek"><span class="buf"></span><span class="played"></span><span class="seg s1"></span><span class="seg s2"></span><span class="knob"></span></div>' +
        '<div class="tv-times"><span>4:51</span><span>12:07</span></div>' +
        '<div class="tv-btns"><span>' + icon('prev') + '</span><span class="on">' + icon('pause') + '</span><span>' + icon('next') + '</span><span class="gap"></span><span>' + icon('cc') + '</span><span>' + icon('speed') + '</span><span>' + icon('gear') + '</span></div>' +
      '</div>' +
      '<div class="tv-scene tv-signin">' +
        '<div class="tv-qr">' + qr() + '</div>' +
        '<div class="tv-sign"><p class="s1">' + T.scan + '</p><p class="s2">' + T.enter + '</p><p class="code">KXT-PBD-RWQ</p><p class="s3">' + T.made + '</p></div>' +
      '</div>';
    root.appendChild(stage);

    this.home = stage.querySelector('.tv-home');
    var rowsEl = this.rowsEl = stage.querySelector('.tv-rows');
    ROWS.forEach(function (r, i) {
      var row = h('div', 'tv-row');
      row.appendChild(h('p', 'tv-rowtitle', T.rows[i]));
      var track = h('div', 'tv-track');
      r.items.forEach(function (id) { track.appendChild(card(id, r)); });
      row.appendChild(track);
      rowsEl.appendChild(row);
    });

    function fit() { stage.style.setProperty('--s', root.clientWidth / 1920); }
    if (window.ResizeObserver) new ResizeObserver(fit).observe(root); else addEventListener('resize', fit);
    fit();

    function tick() {
      var d = new Date();
      stage.querySelector('.tv-clock b').textContent = d.toLocaleTimeString(T.locale, { hour: 'numeric', minute: '2-digit' });
      stage.querySelector('.tv-clock i').textContent = d.toLocaleDateString(T.locale, { weekday: 'short', month: 'short', day: 'numeric' }).replace(',', '');
    }
    tick(); setInterval(tick, 20000);

    if (this.interactive) {
      root.addEventListener('keydown', function (e) {
        var k = { ArrowLeft: [0, -1], ArrowRight: [0, 1], ArrowUp: [-1, 0], ArrowDown: [1, 0] }[e.key];
        if (!k || self.scene !== 'home') return;
        e.preventDefault();
        self.stopAuto();
        if (self.rail) { if (k[1] === 1) self.openRail(false); return; }
        if (k[1] === -1 && self.cols[self.row] === 0) { self.openRail(true); return; }
        self.move(k[0], k[1]);
      });
      root.addEventListener('pointerdown', function (e) {
        if (self.scene !== 'home' || !e.target.closest) return;
        var c = e.target.closest('.tv-card');
        self.stopAuto();
        if (e.target.closest('.tv-rail')) { self.openRail(!self.rail); return; }
        if (!c) return;
        var r = Array.prototype.indexOf.call(rowsEl.children, c.closest('.tv-row'));
        self.rail = false;
        self.focus(r, Array.prototype.indexOf.call(c.parentNode.children, c));
      });
    }

    this.show(root.dataset.scene || 'home');
    root.classList.add('ready');
    if (root.dataset.auto !== '0' && !matchMedia('(prefers-reduced-motion: reduce)').matches) this.startAuto();
  }

  OculoTV.prototype.move = function (dr, dc) {
    var r = Math.max(0, Math.min(ROWS.length - 1, this.row + dr));
    this.row = r;
    this.cols[r] = Math.max(0, Math.min(ROWS[r].items.length - 1, this.cols[r] + dc));
    this.render();
  };
  OculoTV.prototype.focus = function (r, c) { this.row = r; this.cols[r] = c; this.render(); };
  OculoTV.prototype.openRail = function (open) { this.rail = !!open; this.render(); };

  OculoTV.prototype.render = function () {
    var self = this, glow = null;
    Array.prototype.forEach.call(this.rowsEl.children, function (row, r) {
      var track = row.querySelector('.tv-track');
      track.style.transform = 'translateX(' + (-431 * self.cols[r]) + 'px)';
      Array.prototype.forEach.call(track.children, function (c, i) {
        var on = !self.rail && r === self.row && i === self.cols[r];
        c.classList.toggle('focus', on);
        c.classList.toggle('past', i < self.cols[r]);
        if (on) glow = c.dataset.glow;
      });
      row.classList.toggle('above', r < self.row);
    });
    this.rowsEl.style.transform = 'translate(' + (this.rail ? 346 : 0) + 'px,' + (-561 * this.row) + 'px)';
    this.home.classList.toggle('open', this.rail);
    this.setGlow(this.scene === 'home' ? glow : null);
  };

  OculoTV.prototype.setGlow = function (color) {
    var c = color || '#32e6dc';
    this.stage.style.setProperty('--glow', c);
    this.root.dispatchEvent(new CustomEvent('oculo:glow', { bubbles: true, detail: { color: c } }));
  };

  OculoTV.prototype.show = function (scene) {
    this.scene = scene;
    this.stage.dataset.scene = scene;
    this.render();
  };

  OculoTV.prototype.startAuto = function () {
    var self = this, path = [[0, 1], [0, 2], [0, 3], [1, 0], [1, 1], [0, 3], [0, 0]], i = 0;
    this.stopAuto();
    this.timer = setInterval(function () {
      if (self.scene !== 'home' || document.hidden) return;
      var p = path[i++ % path.length];
      self.focus(p[0], p[1]);
    }, 2600);
  };
  OculoTV.prototype.stopAuto = function () { clearInterval(this.timer); this.timer = null; };

  window.OculoTV = OculoTV;
  function boot() {
    Array.prototype.forEach.call(document.querySelectorAll('[data-oculo-tv]'), function (n) { if (!n.oculo) n.oculo = new OculoTV(n); });
    document.dispatchEvent(new CustomEvent('oculo:ready'));
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot();
})();
