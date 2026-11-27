# Oculo — notes de version

Shown by the in-app updater (« Update available » panel) and used as the GitHub Release notes by
`scripts/publish-release.sh`. One section per version, `## MAJOR.MINOR.PATCH`, then short user-facing bullets
(`- `), in French. Newest first. Everything outside the bullets (this paragraph, sub-headings) is ignored.

## 0.14.2

- Mise à jour dans l'app : l'écran final « App installed. » affiche maintenant « Oculo » au lieu d'un long nom technique.

## 0.14.1

- Basé sur SmartTube 32.85 : corrections du lecteur (reprise après erreur, vidéo qui recommençait à zéro, directs…).
- Nouveau raccourci : la flèche du haut passe au chapitre suivant pendant une vidéo (Réglages › Télécommande et appareils › Raccourcis des boutons, mode avancé).

## 0.14.0

- Écran de pause : après quelques secondes en pause, le titre, la chaîne, le temps restant et l'heure de fin sur l'image figée.
- « Ajouter à Regarder plus tard » est maintenant le premier choix du menu d'une vidéo (appui long), avec une confirmation.
- La barre de lecture est découpée en chapitres, et les segments SponsorBlock s'y affichent en même temps.
- Le nom du chapitre en cours s'affiche à côté du temps quand les commandes sont à l'écran.

## 0.13.0

- Pastille « nouveau » sur chaque vidéo d'Abonnements publiée depuis ta dernière visite.
- Masquer les vidéos vues dans Abonnements, en un geste (réglage et menu de la vidéo).
- Minuterie de sommeil dans le panneau du lecteur : 15, 30, 45, 60, 90 min ou fin de la vidéo.
- Écran de veille Oculo : heure, date et miniatures de tes vidéos.
- Depuis 0.9.0 : numéros de version Oculo, typographie revue, nouveaux Réglages et panneau Oculo partout.
