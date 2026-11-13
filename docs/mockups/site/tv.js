/* Oculo TV: a drawn, working copy of the app's interface for the website.
   Everything on screen is invented (channels, titles, pictures): nothing comes from YouTube.
   Stage is authored at 1920 x 1080 px and scaled to its container. */
(function () {
  'use strict';

  var THUMBS = (document.currentScript && document.currentScript.dataset.thumbs) || 'thumbs/';

  var V = {
    aurora:   { t: 'Aurores boréales : une nuit complète en Laponie', ch: 'Ciel ouvert', views: '412 k vues', age: 'il y a 3 h', recent: true, len: '18:24', glow: '#35d49a', bg: 'linear-gradient(160deg,#06281f,#1fae7a 55%,#0a3a4a)' },
    ramen:    { t: 'Un ramen maison en 40 minutes', ch: 'La petite marmite', views: '1,2 M de vues', age: 'il y a 2 jours', len: '12:07', glow: '#f08a3c', bg: 'linear-gradient(160deg,#3a1606,#d9742a 60%,#5a2a0c)' },
    reef:     { t: 'Sous la surface : le récif à l’aube', ch: 'Grand Bleu', views: '96 k vues', age: 'il y a 5 jours', len: '42:10', glow: '#2f8fe8', bg: 'linear-gradient(160deg,#04214a,#1f7fd6 60%,#0a3a6a)' },
    rocket:   { t: 'Décollage en direct depuis le pas de tir', ch: 'Orbite', live: '14 k spectateurs', glow: '#5a84e6', bg: 'linear-gradient(180deg,#0a1a4a,#35509a 60%,#e08a3a)' },
    canyon:   { t: 'Descente du canyon rouge, caméra embarquée', ch: 'Deux roues', views: '230 k vues', age: 'il y a 1 mois', len: '9:48', glow: '#e4583a', bg: 'linear-gradient(160deg,#4a1206,#d2502a 60%,#7a2a10)' },
    vinyl:    { t: 'Session vinyle, jazz de fin de soirée', ch: 'Platine 33', views: '58 k vues', age: 'il y a 20 h', recent: true, len: '1:04:30', glow: '#b45ae6', bg: 'linear-gradient(160deg,#1e0a3a,#9a3ad6 60%,#d23a9a)' },
    workshop: { t: 'Fabriquer une chaise en chêne, étape par étape', ch: 'L’établi', views: '310 k vues', age: 'il y a 3 semaines', len: '27:15', glow: '#e6b43a', bg: 'linear-gradient(160deg,#3a2606,#d6a02a 60%,#6a4a10)' },
    city:     { t: 'La ville de nuit en accéléré', ch: 'Longue pose', views: '74 k vues', age: 'il y a 6 jours', len: '6:02', glow: '#e65aa6', bg: 'linear-gradient(160deg,#1a0a3a,#d23a8a 60%,#3a1a6a)' }
  };

  var ROWS = [
    { title: 'Recommandé', items: ['aurora', 'ramen', 'reef', 'rocket', 'canyon', 'workshop', 'city'] },
    { title: 'Continuer à regarder', progress: { vinyl: 34, workshop: 62, reef: 81, city: 18 }, items: ['vinyl', 'workshop', 'reef', 'city'] },
    { title: 'Abonnements', items: ['canyon', 'city', 'ramen', 'vinyl', 'aurora', 'rocket'] }
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
  function icon(name) { return '<svg viewBox="0 0 24 24" aria-hidden="true">' + ICON[name] + '</svg>'; }

  function wordmark() { return '<span class="tv-wm">' + (window.oculoLogo ? window.oculoLogo('dark') : '') + '</span>'; }

  function h(tag, cls, html) {
    var n = document.createElement(tag);
    if (cls) n.className = cls;
    if (html != null) n.innerHTML = html;
    return n;
  }

  function card(id, row) {
    var v = V[id];
    var c = h('div', 'tv-card');
    c.dataset.glow = v.glow;
    var meta = v.ch + ' <i>•</i> ';
    if (v.live) meta += '<b class="acc">' + v.live + '</b>';
    else meta += v.views + ' <i>•</i> ' + (v.recent ? '<b class="acc">' + v.age + '</b>' : v.age);
    var badge = v.live ? '<span class="tv-badge live">EN DIRECT</span>' : '<span class="tv-badge">' + v.len + '</span>';
    var prog = row.progress && row.progress[id] != null ? '<span class="tv-watched"><i style="width:' + row.progress[id] + '%"></i></span>' : '';
    c.innerHTML = '<div class="tv-thumb" style="background-image:url(' + THUMBS + id + '.jpg),' + v.bg + '">' + badge + prog + '</div>' +
      '<div class="tv-text"><p class="tv-title">' + v.t + '</p><p class="tv-meta">' + meta + '</p></div>';
    return c;
  }

  function qr() {
    var n = 29, s = 0x9e3779b1, out = '', x, y;
    function rnd() { s ^= s << 13; s ^= s >>> 17; s ^= s << 5; return ((s >>> 0) % 1000) / 1000; }
    function finder(fx, fy) { return x >= fx && x < fx + 7 && y >= fy && y < fy + 7; }
    function finderOn(fx, fy) { var dx = x - fx, dy = y - fy; return dx === 0 || dx === 6 || dy === 0 || dy === 6 || (dx > 1 && dx < 5 && dy > 1 && dy < 5); }
    for (y = 0; y < n; y++) for (x = 0; x < n; x++) {
      var on;
      if (finder(0, 0)) on = finderOn(0, 0);
      else if (finder(n - 7, 0)) on = finderOn(n - 7, 0);
      else if (finder(0, n - 7)) on = finderOn(0, n - 7);
      else if ((x < 8 && y < 8) || (x > n - 9 && y < 8) || (x < 8 && y > n - 9)) on = false;
      else on = rnd() > 0.52;
      if (on) out += '<rect x="' + x + '" y="' + y + '" width="1.02" height="1.02"/>';
    }
    return '<svg viewBox="-4 -4 ' + (n + 8) + ' ' + (n + 8) + '" aria-hidden="true"><rect x="-4" y="-4" width="' + (n + 8) + '" height="' + (n + 8) + '" fill="#fff"/><g fill="#000">' + out + '</g></svg>';
  }

  function OculoTV(root) {
    var self = this;
    this.root = root;
    this.row = 0;
    this.cols = ROWS.map(function () { return 0; });
    this.auto = root.dataset.auto !== '0';
    this.timer = null;
    root.classList.add('tv');
    if (!root.hasAttribute('tabindex') && root.dataset.keys !== '0') root.tabIndex = 0;
    root.setAttribute('role', 'img');
    root.setAttribute('aria-label', root.dataset.label || 'L’écran d’accueil d’Oculo sur une télé');

    var stage = this.stage = h('div', 'tv-stage');
    stage.innerHTML =
      '<div class="tv-glow"></div>' +
      '<div class="tv-scene tv-home">' +
        '<div class="tv-rows"></div>' +
        '<div class="tv-rail"><span class="tv-avatar">' + icon('person') + '</span>' +
          ['search', 'home', 'subs', 'live', 'music', 'history', 'lists', 'gear'].map(function (k) { return '<span class="tv-ic' + (k === 'home' ? ' cur' : '') + '">' + icon(k) + '</span>'; }).join('') + '</div>' +
        '<div class="tv-top"><span class="tv-mic">' + icon('mic') + '</span><span class="tv-pill">Rechercher</span>' +
          '<span class="tv-clock"><b></b><i></i></span>' + wordmark() + '</div>' +
      '</div>' +
      '<div class="tv-scene tv-player">' +
        '<div class="tv-frame" style="background-image:url(' + THUMBS + 'ramen.jpg),' + V.ramen.bg + '"></div>' +
        '<div class="tv-shade"></div>' +
        '<div class="tv-skip"><i></i>Segment passé : Commandite</div>' +
        '<p class="tv-ptitle">' + V.ramen.t + '<span>' + V.ramen.ch + '</span></p>' +
        '<div class="tv-seek"><span class="buf"></span><span class="played"></span><span class="seg s1"></span><span class="seg s2"></span><span class="knob"></span></div>' +
        '<div class="tv-times"><span>4:51</span><span>12:07</span></div>' +
        '<div class="tv-btns"><span>' + icon('prev') + '</span><span class="on">' + icon('pause') + '</span><span>' + icon('next') + '</span><span class="gap"></span><span>' + icon('cc') + '</span><span>' + icon('speed') + '</span><span>' + icon('gear') + '</span></div>' +
      '</div>' +
      '<div class="tv-scene tv-signin">' +
        '<div class="tv-qr">' + qr() + '</div>' +
        '<div class="tv-sign"><p class="s1">Balayez le code avec votre téléphone</p><p class="s2">ou entrez ce code sur la page de connexion</p><p class="code">KXT-PBD-RWQ</p><p class="s3">Le code QR est fabriqué ici, sur la télé.</p></div>' +
      '</div>';
    root.appendChild(stage);

    var rowsEl = this.rowsEl = stage.querySelector('.tv-rows');
    ROWS.forEach(function (r) {
      var row = h('div', 'tv-row');
      row.appendChild(h('p', 'tv-rowtitle', r.title));
      var track = h('div', 'tv-track');
      r.items.forEach(function (id) { track.appendChild(card(id, r)); });
      row.appendChild(track);
      rowsEl.appendChild(row);
    });

    function fit() { stage.style.setProperty('--s', root.clientWidth / 1920); }
    if (window.ResizeObserver) new ResizeObserver(fit).observe(root);
    fit();

    function tick() {
      var d = new Date();
      stage.querySelector('.tv-clock b').textContent = d.toLocaleTimeString('fr-CA', { hour: 'numeric', minute: '2-digit' });
      stage.querySelector('.tv-clock i').textContent = d.toLocaleDateString('fr-CA', { weekday: 'short', day: 'numeric', month: 'short' });
    }
    tick(); setInterval(tick, 20000);

    root.addEventListener('keydown', function (e) {
      var k = { ArrowLeft: [0, -1], ArrowRight: [0, 1], ArrowUp: [-1, 0], ArrowDown: [1, 0] }[e.key];
      if (!k || self.scene !== 'home') return;
      e.preventDefault();
      self.stopAuto();
      self.move(k[0], k[1]);
    });
    root.addEventListener('pointerdown', function (e) {
      var c = e.target.closest && e.target.closest('.tv-card');
      if (!c || self.scene !== 'home') return;
      var rows = Array.prototype.slice.call(rowsEl.children);
      var r = rows.indexOf(c.closest('.tv-row'));
      self.stopAuto();
      self.row = r;
      self.cols[r] = Array.prototype.indexOf.call(c.parentNode.children, c);
      self.render();
    });

    this.show(root.dataset.scene || 'home');
    this.render();
    if (this.auto && !matchMedia('(prefers-reduced-motion: reduce)').matches) this.startAuto();
  }

  OculoTV.prototype.move = function (dr, dc) {
    var r = Math.max(0, Math.min(ROWS.length - 1, this.row + dr));
    this.row = r;
    this.cols[r] = Math.max(0, Math.min(ROWS[r].items.length - 1, this.cols[r] + dc));
    this.render();
  };

  OculoTV.prototype.focus = function (r, c) { this.row = r; this.cols[r] = c; this.render(); };

  OculoTV.prototype.render = function () {
    var self = this, glow = null;
    Array.prototype.forEach.call(this.rowsEl.children, function (row, r) {
      var track = row.querySelector('.tv-track');
      track.style.transform = 'translateX(' + (-431 * self.cols[r]) + 'px)';
      Array.prototype.forEach.call(track.children, function (c, i) {
        var on = r === self.row && i === self.cols[r];
        c.classList.toggle('focus', on);
        c.classList.toggle('past', i < self.cols[r]);
        if (on) glow = c.dataset.glow;
      });
      row.classList.toggle('above', r < self.row);
    });
    this.rowsEl.style.transform = 'translateY(' + (-561 * this.row) + 'px)';
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
    if (this.rowsEl) this.render();
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
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot();
})();
