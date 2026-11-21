# Fihirana & Baiboly  (mg.sitraka.fihirana)

Application Android (Android 5.0+ / API 21) : cantiques malgaches, français et anglais + 6 versions de la Bible.
Concepteur : **Sitraka Nambinintsoa**

## Fonctions
- Fihirana MG (800), FR (654), EN (830) : par numéro, par thème (lohahevitra), recherche titre / numéro / mots dans les paroles, favoris, mode présentation, audio (stream + téléchargement), « mitovy feo », clavier numérique, police réglable.
- Baiboly : MG 1865, MG DIEM, KJV, Louis Segond, Martin 1744, Hébreu moderne. Livre/chapitre, recherche (mots, phrase ou référence « Jaona 3:16 »), comparaison de versions, copier, partager, surligner en couleur, favoris, notes, audio par chapitre.
- Balayage gauche/droite : hira / toko manaraka sy teo aloha.
- Drawer, thème clair / sombre / système, 6 couleurs d'accent.

## Compiler avec GitHub Actions
1. Envoyez tout le contenu de ce dossier sur un dépôt GitHub (branche main).
2. Onglet **Actions** → « Build APK Fihirana & Baiboly » (se lance seul au push, ou « Run workflow »).
3. Téléchargez l'artefact **Fihirana-Baiboly-APK** → `Fihirana-Baiboly.apk`.

### Clé de signature (recommandé)
Sans secret, une clé est générée à chaque build (l'APK ne se met pas à jour par-dessus une version précédente).
Pour garder la même clé : ajoutez les secrets `KEYSTORE_BASE64`, `KS_PASS`, `KEY_ALIAS`, `KEY_PASS`.

## Régénérer la base de données
`python3 tools/build_db.py fihirana2.db3 app/src/main/assets/fihirana_baiboly.db`
(si vous modifiez la base, augmentez `CONTENT_VERSION` dans `Repo.kt`).
