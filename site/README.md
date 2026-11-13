# Oculo website

The site for Oculo: one dark page where a drawn, working copy of the app's interface is the subject.
English is the main language (`index.html`), French is second (`fr/index.html`). Plain static files,
no dependency, nothing loaded from another host (fonts and pictures are local).

**Not published.** GitHub Pages is not enabled for this repository and there is no download link:
publishing is François's decision, not taken yet. Planned address: `https://francoisauger.github.io/oculo/`.

## Files

```
build.mjs            the template and every piece of copy (EN and FR); writes the three HTML files
index.html           English page (generated, committed)
fr/index.html        French page (generated, committed)
404.html             GitHub Pages error page (generated; absolute /oculo/ paths)
assets/site.css      the page
assets/site.js       the room takes the TV's light; the tour's TV follows the text
assets/tv.css|js     the TV: Oculo's home screen, player and sign-in screen, drawn at 1920 x 1080
assets/thumbs/       invented video pictures (gen.sh = how they were made, with Codex CLI)
assets/fonts/        Nunito (site) and Roboto Condensed (TV), SIL Open Font License
assets/logo-*.svg    the approved logo, from docs/mockups/logos-v3.html, unchanged
assets/tv-poster.webp  still of the TV shown before the script runs; og.jpg = share image
```

## Change the text

Edit `COPY` in `build.mjs` (both languages), then:

```bash
node site/build.mjs
```

The TV's own strings (rows, video titles, rail labels) are in `assets/tv.js`, per language.

## See it locally

The pages use relative paths, except `404.html`. To get the same paths as on GitHub Pages, serve the
folder under `/oculo/`:

```bash
mkdir -p site/.serve && ln -sfn .. site/.serve/oculo
python3 -m http.server 3043 --bind 127.0.0.1 --directory site/.serve
# http://127.0.0.1:3043/oculo/   and   http://127.0.0.1:3043/oculo/fr/
# stop: kill $(lsof -ti tcp:3043 -sTCP:LISTEN)
```

## Rules for this site

- Nothing from YouTube: no screenshot of real videos, no logo, no red play button, and no "YouTube"
  in a title or heading. Channels, titles and pictures on the TV are invented.
- Footer keeps "Oculo is not affiliated with YouTube or Google" and the credit to SmartTube by
  yuliskov (MIT), with links to the repository and the license.
- No mention of ads, no donations, no APK link until François says so.
- "by Arix" may sign the site, never the logo.
- The "What Oculo talks to" table follows the network audit (`docs/mockups/communications.html`):
  update it when the app's network behaviour changes.

## Publishing, when decided

GitHub Pages can only serve a branch's root or `/docs`, and `docs/` holds the mockup boards, so the
site would be deployed from `site/` by a GitHub Actions workflow (`actions/upload-pages-artifact`
with `path: site`, then `actions/deploy-pages`), with Pages set to "GitHub Actions" in the
repository settings. Nothing of this exists yet, on purpose.
