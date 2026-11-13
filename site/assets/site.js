/* Page behaviour: the room takes the TV's light, and the tour's TV follows the text. */
(function () {
  'use strict';
  document.documentElement.classList.add('js');
  var still = matchMedia('(prefers-reduced-motion: reduce)').matches;
  var lead = 'tv-hero';
  var room = document.querySelector('.room');

  // Each set glows with its own TV; the room follows the TV that leads (hero, then tour).
  document.addEventListener('oculo:glow', function (e) {
    var set = e.target.closest('.set');
    if (set) set.style.setProperty('--halo', e.detail.color);
    if (e.target.id === lead && room) room.style.setProperty('--amb', e.detail.color);
  });

  // tv.js may already have built the TVs by the time this script runs.
  if (document.getElementById('tv-hero').oculo) init(); else document.addEventListener('oculo:ready', init);

  function init() {
    var hero = document.getElementById('tv-hero').oculo;
    var tour = document.getElementById('tv-tour').oculo;
    var cycle = null;

    function show(kind) {
      clearInterval(cycle);
      tour.rail = false;
      if (kind === 'continue') { tour.show('home'); tour.focus(1, 0); }
      else if (kind === 'glow') {
        var i = 0;
        tour.show('home'); tour.focus(0, 0);
        if (!still) cycle = setInterval(function () { tour.focus(0, ++i % 5); }, 1900);
      }
      else if (kind === 'remote') { tour.show('home'); tour.focus(0, 0); tour.openRail(true); }
      else tour.show(kind);
    }

    var steps = Array.prototype.slice.call(document.querySelectorAll('.step'));
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        if (!e.isIntersecting) return;
        steps.forEach(function (s) { s.classList.toggle('on', s === e.target); });
        lead = 'tv-tour';
        show(e.target.dataset.show);
      });
    }, { rootMargin: '-45% 0px -45% 0px' });
    steps.forEach(function (s) { io.observe(s); });

    new IntersectionObserver(function (entries) {
      if (entries[0].isIntersecting) { lead = 'tv-hero'; hero.render(); }
    }, { threshold: .5 }).observe(hero.root);

    hero.render();
  }
})();
