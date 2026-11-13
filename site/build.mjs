// Builds the two pages of the Oculo site from one template: index.html (English, the main language)
// and fr/index.html (French). No dependency: `node site/build.mjs`.
// The output is committed, so the site stays plain static files (GitHub Pages serves them as is).
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const SITE = 'https://francoisauger.github.io/oculo/';
const REPO = 'https://github.com/francoisauger/oculo';
const UPSTREAM = 'https://github.com/yuliskov/SmartTube';
const logo = readFileSync(join(here, 'assets/logo-dark.svg'), 'utf8').trim().replace(' xmlns="http://www.w3.org/2000/svg"', '');

const a = (href, text) => `<a href="${href}">${text}</a>`;

const COPY = {
  en: {
    path: '', other: { href: 'fr/', lang: 'fr', label: 'Français' },
    title: 'Oculo: a calm video app for Android TV',
    description: 'Oculo is a free, open-source app for Android TV and Google TV, with an interface made for the remote: Continue watching, a glow that follows the picture, SponsorBlock, no tracking.',
    skip: 'Skip to content', home: 'Oculo, home', navLabel: 'Main',
    nav: ['Features', 'Privacy', 'Source'],
    h1: 'Your videos, on the big screen.',
    lead: 'Oculo is a free, open-source app for Android TV and Google TV. It plays YouTube in a calm interface made for the remote and readable from the couch.',
    cta: 'View the code on GitHub', ctaNote: 'Install: coming with version 1.0.',
    heroTv: 'The Oculo home screen. The arrow keys move the selection.',
    keys: 'This TV works. Click it, then try the arrow keys.', touch: 'This TV works. Tap a video.',
    tourTv: 'Oculo, showing the feature described beside it',
    steps: [
      ['continue', 'Pick up where you left off', ['The Continue watching row keeps what you started in the last 48 hours and did not finish. It is built from what is on the TV, with no network call.', 'Long-press a video and it stops showing up there.']],
      ['glow', 'The light follows the picture', ['The background takes the color of the video in focus, and a white ring breathes around it. You always know where you are, even from the back of the room.']],
      ['remote', 'Made for a remote', ['One press left opens the sections. One press up reaches search, by voice or by typing. The clock stays in its corner.']],
      ['player', 'Sponsor segments skip themselves', ['SponsorBlock skips the segments its community has flagged. A small notice says so, then gets out of the way.']],
      ['signin', 'Nothing leaves the TV without a reason', ['No analytics, no crash reports. Even the sign-in QR code is made on the TV instead of by an outside service.']],
    ],
    talksH: 'What Oculo talks to',
    talksIntro: 'Every network call in the code has been read, submodules included. This is where the app connects, when, and what goes there.',
    talksCols: ['Where', 'When', 'What goes there'],
    talks: [
      ['YouTube and Google', 'Always', 'Browsing and playback requests, as with any YouTube app. If you sign in: your account, and your watch history unless you turn it off.'],
      ['SponsorBlock', 'At each video, until you turn it off', 'The ID of the video. Read only: Oculo never votes.'],
      ['Return YouTube Dislike', 'At each video, until you turn it off', 'The ID of the video.'],
      ['Your TV’s speech service', 'When you press the mic', 'Your voice, handled by the TV itself. Oculo only receives the text.'],
      ['GitHub', 'At launch, to look for an update', 'Nothing about you or your TV.'],
      ['Analytics and crash reports', 'Never', 'That code is not in the app.', true],
      ['QR code services', 'Never', 'The codes are drawn on the TV.', true],
    ],
    talksNote: 'Off until you turn them on: DeArrow titles, the phone remote and Google Drive backup.',
    sourceH: 'Where Oculo comes from',
    source: [
      `Oculo is based on ${a(UPSTREAM, 'SmartTube by yuliskov')} (MIT license). The playback engine is his, untouched. Oculo changes what you see: the home screen, the navigation, the focus and the colors.`,
      `All of it is public: ${a(REPO, 'the repository')}, ${a(REPO + '/blob/oculo/LICENSE', 'the license')} and ${a(REPO + '/blob/oculo/OCULO.md', 'the list of every changed file')}.`,
    ],
    installH: 'Installing',
    install: [
      'Not yet. Version 1.0 will be the first public release, and the steps will be here.',
      'It will run on Android TV and Google TV, Android 5 and up. It is not made for phones or tablets.',
    ],
    foot: ['Oculo is not affiliated with YouTube or Google.', 'This site loads nothing from anywhere else: no trackers, no outside fonts.'],
    by: 'Oculo, by Arix',
    nf: ['Nothing on this channel', 'This page does not exist.', 'Back to Oculo'],
  },
  fr: {
    path: 'fr/', other: { href: '../', lang: 'en', label: 'English' },
    title: 'Oculo : une app vidéo calme pour Android TV',
    description: 'Oculo est une app libre et gratuite pour Android TV et Google TV, avec une interface faite pour la télécommande : Continuer à regarder, une lueur qui suit l’image, SponsorBlock, aucun pistage.',
    skip: 'Aller au contenu', home: 'Oculo, accueil', navLabel: 'Principale',
    nav: ['Fonctions', 'Vie privée', 'Code source'],
    h1: 'Vos vidéos, sur le grand écran.',
    lead: 'Oculo est une app libre et gratuite pour Android TV et Google TV. Elle lit YouTube dans une interface calme, faite pour la télécommande et lisible du divan.',
    cta: 'Voir le code sur GitHub', ctaNote: 'Installation : à la sortie de la version 1.0.',
    heroTv: 'L’accueil d’Oculo. Les flèches du clavier déplacent la sélection.',
    keys: 'Cette télé fonctionne. Cliquez dessus, puis essayez les flèches du clavier.', touch: 'Cette télé fonctionne. Touchez une vidéo.',
    tourTv: 'Oculo, montrant la fonction décrite à côté',
    steps: [
      ['continue', 'Reprendre où vous étiez', ['La rangée « Continuer à regarder » garde ce que vous avez commencé depuis 48 heures sans le terminer. Elle se remplit à partir de ce qui est sur la télé, sans aucun appel réseau.', 'Un appui long sur une vidéo, et elle n’y revient plus.']],
      ['glow', 'La lumière suit l’image', ['Le fond prend la couleur de la vidéo sélectionnée, et un anneau blanc respire autour d’elle. On sait toujours où on est, même du fond du salon.']],
      ['remote', 'Faite pour la télécommande', ['Une pression à gauche ouvre les sections. Une pression en haut mène à la recherche, à la voix ou au clavier. L’heure reste dans son coin.']],
      ['player', 'Les commandites passent toutes seules', ['SponsorBlock saute les segments signalés par sa communauté. Un petit avis le dit, puis s’efface.']],
      ['signin', 'Rien ne sort de la télé sans raison', ['Aucune statistique d’usage, aucun rapport de plantage. Même le code QR de connexion est fabriqué sur la télé plutôt que par un service externe.']],
    ],
    talksH: 'Ce qu’Oculo contacte',
    talksIntro: 'Chaque appel réseau du code a été lu, sous-modules compris. Voici où l’app se connecte, quand, et ce qui y part.',
    talksCols: ['Où', 'Quand', 'Ce qui y part'],
    talks: [
      ['YouTube et Google', 'Toujours', 'Les demandes de navigation et de lecture, comme toute app YouTube. Si vous vous connectez : votre compte, et votre historique de visionnage sauf si vous le coupez.'],
      ['SponsorBlock', 'À chaque vidéo, tant que vous ne le coupez pas', 'L’identifiant de la vidéo. En lecture seule : Oculo ne vote jamais.'],
      ['Return YouTube Dislike', 'À chaque vidéo, tant que vous ne le coupez pas', 'L’identifiant de la vidéo.'],
      ['La reconnaissance vocale de la télé', 'Quand vous appuyez sur le micro', 'Votre voix, traitée par la télé elle-même. Oculo ne reçoit que le texte.'],
      ['GitHub', 'Au démarrage, pour chercher une mise à jour', 'Rien sur vous ni sur votre télé.'],
      ['Statistiques et rapports de plantage', 'Jamais', 'Ce code n’est pas dans l’app.', true],
      ['Services de codes QR', 'Jamais', 'Les codes sont dessinés sur la télé.', true],
    ],
    talksNote: 'Coupés tant que vous ne les activez pas : les titres DeArrow, la télécommande par téléphone et la sauvegarde Google Drive.',
    sourceH: 'D’où vient Oculo',
    source: [
      `Oculo est basé sur ${a(UPSTREAM, 'SmartTube de yuliskov')} (licence MIT). Le moteur de lecture est le sien, intact. Oculo change ce qu’on voit : l’accueil, la navigation, la sélection et les couleurs.`,
      `Tout est public : ${a(REPO, 'le dépôt')}, ${a(REPO + '/blob/oculo/LICENSE', 'la licence')} et ${a(REPO + '/blob/oculo/OCULO.md', 'la liste de chaque fichier changé')} (en anglais).`,
    ],
    installH: 'Installer',
    install: [
      'Pas encore. La version 1.0 sera la première publiée, et les étapes seront ici.',
      'Elle fonctionnera sur Android TV et Google TV, Android 5 et plus. Elle n’est pas faite pour les téléphones ni les tablettes.',
    ],
    foot: ['Oculo n’est pas affilié à YouTube ni à Google.', 'Ce site ne charge rien d’ailleurs : ni pisteur, ni police externe.'],
    by: 'Oculo, by Arix',
    nf: ['Rien sur cette chaîne', 'Cette page n’existe pas.', 'Retour à Oculo'],
  },
};

function head(c, lang, root, extra = '') {
  return `<!doctype html>
<html lang="${lang}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${c.title}</title>
<meta name="description" content="${c.description}">
<meta name="theme-color" content="#020505">
${extra}<link rel="icon" href="${root}assets/favicon.png" type="image/png">
<link rel="apple-touch-icon" href="${root}assets/apple-touch-icon.png">
<link rel="preload" href="${root}assets/fonts/nunito-latin-800-normal.woff2" as="font" type="font/woff2" crossorigin>
<link rel="stylesheet" href="${root}assets/site.css">
<link rel="stylesheet" href="${root}assets/tv.css">
</head>`;
}

function page(lang) {
  const c = COPY[lang];
  const root = lang === 'en' ? '' : '../';
  const meta = `<link rel="canonical" href="${SITE}${c.path}">
<link rel="alternate" hreflang="en" href="${SITE}">
<link rel="alternate" hreflang="fr" href="${SITE}fr/">
<link rel="alternate" hreflang="x-default" href="${SITE}">
<meta property="og:type" content="website">
<meta property="og:title" content="${c.title}">
<meta property="og:description" content="${c.description}">
<meta property="og:url" content="${SITE}${c.path}">
<meta property="og:image" content="${SITE}assets/og.jpg">
<meta name="twitter:card" content="summary_large_image">
`;
  const steps = c.steps.map(([show, h, ps], i) => `      <article class="step${i === 0 ? ' on' : ''}" data-show="${show}">
        <h2>${h}</h2>
${ps.map((p) => `        <p>${p}</p>`).join('\n')}
      </article>`).join('\n');
  const rows = c.talks.map(([where, when, what, never]) => `          <tr${never ? ' class="never"' : ''}><th scope="row">${where}</th><td>${when}</td><td>${what}</td></tr>`).join('\n');

  return `${head(c, lang, root, meta)}
<body>
<div class="room" aria-hidden="true"></div>
<a class="skip" href="#main">${c.skip}</a>
<header class="wrap head">
  <a class="logo" href="./" aria-label="${c.home}" data-oculo-logo>${logo}</a>
  <nav aria-label="${c.navLabel}">
    <a href="#tour">${c.nav[0]}</a>
    <a href="#privacy">${c.nav[1]}</a>
    <a href="#source">${c.nav[2]}</a>
    <a class="lang" href="${c.other.href}" lang="${c.other.lang}" hreflang="${c.other.lang}">${c.other.label}</a>
  </nav>
</header>

<main id="main">
  <section class="wrap hero">
    <h1>${c.h1}</h1>
    <p class="lead">${c.lead}</p>
    <div class="cta">
      <a class="btn" href="${REPO}">${c.cta}</a>
      <span>${c.ctaNote}</span>
    </div>
    <div class="set"><div id="tv-hero" data-oculo-tv aria-label="${c.heroTv}"></div></div>
    <p class="keys" aria-hidden="true"><kbd>←</kbd><kbd>↑</kbd><kbd>↓</kbd><kbd>→</kbd><span class="mouse">${c.keys}</span><span class="touch">${c.touch}</span></p>
  </section>

  <section class="wrap tour" id="tour">
    <div class="stick"><div class="set"><div id="tv-tour" data-oculo-tv data-auto="0" data-keys="0" aria-label="${c.tourTv}"></div></div></div>
    <div>
${steps}
    </div>
  </section>

  <section class="wrap" id="privacy">
    <div class="part">
      <h2>${c.talksH}</h2>
      <p style="margin-top:14px">${c.talksIntro}</p>
      <table class="talks">
        <thead><tr><th scope="col">${c.talksCols[0]}</th><th scope="col">${c.talksCols[1]}</th><th scope="col">${c.talksCols[2]}</th></tr></thead>
        <tbody>
${rows}
        </tbody>
      </table>
      <p class="note">${c.talksNote}</p>
    </div>
  </section>

  <section class="wrap" id="source">
    <div class="part pair">
      <h2>${c.sourceH}</h2>
      <div>
${c.source.map((p) => `        <p>${p}</p>`).join('\n')}
      </div>
    </div>
    <div class="part pair" id="install">
      <h2>${c.installH}</h2>
      <div>
${c.install.map((p) => `        <p>${p}</p>`).join('\n')}
      </div>
    </div>
  </section>
</main>

<footer class="wrap foot">
  <span>${c.foot[0]}</span>
  <span>${c.foot[1]}</span>
  <span class="by">${c.by}</span>
</footer>

<script src="${root}assets/tv.js" defer></script>
<script src="${root}assets/site.js" defer></script>
</body>
</html>
`;
}

// GitHub Pages serves one 404.html for the whole site: English, with absolute paths under /oculo/.
function notFound() {
  const c = COPY.en;
  return `${head({ ...c, title: 'Oculo: page not found', description: c.nf[1] }, 'en', '/oculo/', '<meta name="robots" content="noindex">\n')}
<body>
<div class="room" aria-hidden="true"></div>
<header class="wrap head"><a class="logo" href="/oculo/" aria-label="${c.home}">${logo}</a></header>
<main class="wrap hero">
  <h1>${c.nf[0]}</h1>
  <p class="lead">${c.nf[1]}</p>
  <div class="cta"><a class="btn" href="/oculo/">${c.nf[2]}</a></div>
</main>
</body>
</html>
`;
}

mkdirSync(join(here, 'fr'), { recursive: true });
writeFileSync(join(here, 'index.html'), page('en'));
writeFileSync(join(here, 'fr/index.html'), page('fr'));
writeFileSync(join(here, '404.html'), notFound());
console.log('Built index.html, fr/index.html, 404.html');
