#!/usr/bin/env python3
"""Construit la base compacte de l'application a partir de fihirana2.db3.

Usage : python3 tools/build_db.py chemin/vers/fihirana2.db3 app/src/main/assets/fihirana_baiboly.db
"""
import re, sqlite3, sys, unicodedata, os

SRC = sys.argv[1] if len(sys.argv) > 1 else "fihirana2.db3"
DST = sys.argv[2] if len(sys.argv) > 2 else "fihirana_baiboly.db"


def norm(s):
    """Meme regle que Norm.kt : minuscules, sans accents, apostrophes -> espace."""
    if not s:
        return ""
    s = s.lower()
    for ch in "'’‘`ʼ´":
        s = s.replace(ch, " ")
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    s = re.sub(r"[^\w\s]", " ", s, flags=re.UNICODE)
    return re.sub(r"\s+", " ", s).strip()


if os.path.exists(DST):
    os.remove(DST)
src = sqlite3.connect(SRC)
dst = sqlite3.connect(DST)
dst.executescript("""
PRAGMA page_size=4096;
CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT);
CREATE TABLE hymn_langs(code TEXT PRIMARY KEY, name TEXT, short TEXT, ord INTEGER);
CREATE TABLE hymn_themes(lang TEXT, id INTEGER, name TEXT, parent TEXT, PRIMARY KEY(lang,id));
CREATE TABLE hymns(lang TEXT, num INTEGER, title TEXT, tnorm TEXT, key TEXT, author TEXT,
                   theme INTEGER, sub INTEGER, url TEXT, dl TEXT, PRIMARY KEY(lang,num));
CREATE TABLE stanzas(lang TEXT, num INTEGER, idx INTEGER, kind TEXT, text TEXT, norm TEXT);
CREATE INDEX idx_stanzas ON stanzas(lang,num,idx);
CREATE TABLE melodies(lang TEXT, num INTEGER, other INTEGER, grp TEXT);
CREATE INDEX idx_mel ON melodies(lang,num);
CREATE TABLE bible_tr(id INTEGER PRIMARY KEY, code TEXT, name TEXT, short TEXT, lang TEXT, rtl INTEGER, ord INTEGER);
CREATE TABLE bible_books(book INTEGER PRIMARY KEY, abbr TEXT, color TEXT, chapters INTEGER, ord INTEGER);
CREATE TABLE bible_book_names(tr INTEGER, book INTEGER, name TEXT, PRIMARY KEY(tr,book));
CREATE TABLE bible_verses(tr INTEGER, book INTEGER, chapter INTEGER, verse INTEGER, text TEXT,
                          heading TEXT, note TEXT, PRIMARY KEY(tr,book,chapter,verse));
CREATE TABLE bible_audio(tr INTEGER, book INTEGER, chapter INTEGER, url TEXT, url2 TEXT, PRIMARY KEY(tr,book,chapter));
""")


def clean_lines(t):
    t = t.replace("\r\n", "\n").replace("\r", "\n")
    parts = re.split(r"\s*\*\s*|\n", t)
    return "\n".join(p.strip() for p in parts if p.strip())


# ---------------------------------------------------------------- Fihirana MG
dst.execute("INSERT INTO hymn_langs VALUES('mg','Fihirana Malagasy','MG',1)")
dst.execute("INSERT INTO hymn_langs VALUES('fr','Cantiques français','FR',2)")
dst.execute("INSERT INTO hymn_langs VALUES('en','English hymnal','EN',3)")

themes = dict(src.execute("select Id_theme, Theme from theme"))
for i, n in themes.items():
    dst.execute("INSERT INTO hymn_themes VALUES('mg',?,?,NULL)", (i, n.strip()))
sub_parent = {}
for i, p, n in src.execute("select Id_southeme, Id_theme, Soustheme from soustheme"):
    sub_parent[i] = p
    dst.execute("INSERT INTO hymn_themes VALUES('mg',?,?,?)", (1000 + i, n.strip(), themes.get(p, "")))
authors = dict(src.execute("select Id_auteur, Nom_auteur from auteur"))

n_h = 0
for num, aut, cle, titre, th, sub, url, dl in src.execute(
        "select Id_cant,Id_auteur,Cle,Titre,Id_theme,Id_SouTheme,url,urldownload from chanson order by Id_cant"):
    cle = re.sub(r"\s*\*\s*", " ", cle or "").strip()
    real_theme = sub_parent.get(sub, th)
    dst.execute("INSERT INTO hymns VALUES('mg',?,?,?,?,?,?,?,?,?)",
                (num, titre.strip(), norm(titre), cle, authors.get(aut, ""), real_theme, 1000 + (sub or 0),
                 (url or "").strip(), (dl or "").strip()))
    n_h += 1

stz = {}
for rid, vid, cant, txt in src.execute("select rowid, Id_verset, Id_cant, Cantique from versets order by rowid"):
    stz.setdefault(cant, []).append((vid, txt or ""))
for cant, rows in stz.items():
    idx = 0
    for vid, txt in rows:
        lines = clean_lines(txt)
        if not lines:
            continue
        kind = "v"
        if vid in (None, ""):
            kind = "r"
            first, _, rest = lines.partition("\n")
            if re.fullmatch(r"[A-ZÀ-Ý' ’\-\.]{4,}", first.strip()):
                lines = rest
        if not lines.strip():
            continue
        idx += 1
        dst.execute("INSERT INTO stanzas VALUES('mg',?,?,?,?,?)", (cant, idx, kind, lines, norm(lines)))

for a, b, g in src.execute("select Id_cant, Id_melodie, groupe from melodie"):
    dst.execute("INSERT INTO melodies VALUES('mg',?,?,?)", (a, b, g))

# ---------------------------------------------------------------- Cantiques FR
for i, n in src.execute("select Id_theme, Theme from themefr"):
    parent = n.split(" - ")[0].strip() if " - " in n else None
    label = n.split(" - ", 1)[1].strip() if " - " in n else n.strip()
    dst.execute("INSERT INTO hymn_themes VALUES('fr',?,?,?)", (i, label, parent))
for num, titre, th in src.execute("select Id_cant,Titre,Id_theme from hymnalfr order by Id_cant"):
    dst.execute("INSERT INTO hymns VALUES('fr',?,?,?,?,?,?,?,?,?)",
                (num, titre.strip(), norm(titre), "", "", th, 0, "", ""))
for cant, vid, txt in src.execute("select Id_cant, Id_verset, Cantique from versetfr order by Id_cant, rowid"):
    lines = clean_lines(txt or "")
    kind = "v"
    if re.match(r"(?i)^refrain\b", lines):
        kind = "r"
        lines = lines.split("\n", 1)[1] if "\n" in lines else ""
    if not lines.strip():
        continue
    dst.execute("INSERT INTO stanzas VALUES('fr',?,?,?,?,?)", (cant, vid or 0, kind, lines, norm(lines)))

# ---------------------------------------------------------------- Hymnal EN
for i, n in src.execute("select Id_theme, Theme from themeus"):
    dst.execute("INSERT INTO hymn_themes VALUES('en',?,?,NULL)", (i, n.strip()))
for num, titre, th, dl in src.execute("select Id_cant,Titre,Id_theme,urldownload from hymnalus order by Id_cant"):
    dst.execute("INSERT INTO hymns VALUES('en',?,?,?,?,?,?,?,?,?)",
                (num, titre.strip(), norm(titre), "", "", th, 0, (dl or "").strip(), ""))
for cant, vid, txt in src.execute("select Id_cant, Id_verset, Cantique from versetus order by Id_cant, rowid"):
    lines = clean_lines(txt or "")
    if not lines.strip():
        continue
    dst.execute("INSERT INTO stanzas VALUES('en',?,?,?,?,?)", (cant, vid or 0, "v", lines, norm(lines)))

# renumeroter idx 1..n pour fr/en
for lang in ("fr", "en"):
    rows = dst.execute("select rowid, num from stanzas where lang=? order by num, rowid", (lang,)).fetchall()
    cur, k = None, 0
    for rid, num in rows:
        if num != cur:
            cur, k = num, 0
        k += 1
        dst.execute("update stanzas set idx=? where rowid=?", (k, rid))

# ---------------------------------------------------------------- Baiboly
TR = [
    (1, "mg1865", "Baiboly Malagasy 1865", "MG65", "mg", 0, "verses", "books", None),
    (2, "diem", "Baiboly Malagasy DIEM", "DIEM", "mg", 0, "bibleDIEMverses", "bibleDIEMbook", None),
    (3, "kjv", "King James Version", "KJV", "en", 0, "KJV_verses", "KJV_books", None),
    (4, "ls", "Louis Segond", "LSG", "fr", 0, "LS_verses", "LS_books", None),
    (5, "bdm", "Bible de David Martin 1744", "BDM", "fr", 0, "FreBDM1744_verses", "FreBDM1744_books", None),
    (6, "heb", "עברית מודרנית (Hébreu moderne)", "HEB", "he", 1, "HebModern_verses", "HebModern_books", None),
]
for i, code, name, short, lang, rtl, vt, bt, _ in TR:
    dst.execute("INSERT INTO bible_tr VALUES(?,?,?,?,?,?,?)", (i, code, name, short, lang, rtl, i))

tag_re = re.compile(r"<n>\s*\[?(.*?)\]?\s*</n>", re.S)
unknown_tags = set()
chap_count = {}
for i, code, name, short, lang, rtl, vt, bt, _ in TR:
    if vt == "verses":
        q = "select book_number, chapter, verse, text from verses"
    else:
        q = f"select book_id, chapter, verse, text from {vt}"
    seen = set()
    for b, c, v, t in src.execute(q):
        b, c, v = int(b), int(c), int(v)
        if (b, c, v) in seen:
            continue
        seen.add((b, c, v))
        t = (t or "").strip()
        heading = None
        notes = []
        if "<" in t:
            m = re.match(r"^<n>\s*\[?(.*?)\]?\s*</n>\s*", t, re.S)
            if m:
                heading = m.group(1).strip()
                t = t[m.end():]
            for m in tag_re.finditer(t):
                notes.append(m.group(1).strip())
            t = tag_re.sub("", t).strip()
            for tg in re.findall(r"</?([a-zA-Z]+)", t):
                unknown_tags.add(tg)
            t = re.sub(r"</?[a-zA-Z][^>]*>", "", t)
        t = re.sub(r"\s+", " ", t).strip()
        dst.execute("INSERT INTO bible_verses VALUES(?,?,?,?,?,?,?)",
                    (i, b, c, v, t, heading, " | ".join(notes) if notes else None))
        chap_count[b] = max(chap_count.get(b, 0), c)
    if bt == "books":
        for bn, ln in src.execute("select book_number, long_name from books"):
            dst.execute("INSERT INTO bible_book_names VALUES(?,?,?)", (i, int(bn), ln.strip()))
    else:
        for bid, nm in src.execute(f"select book_id, name from {bt}"):
            dst.execute("INSERT OR REPLACE INTO bible_book_names VALUES(?,?,?)", (i, int(bid), nm.strip()))

for k, (bn, abbr, color) in enumerate(src.execute("select book_number, short_name, book_color from books order by book_number")):
    dst.execute("INSERT INTO bible_books VALUES(?,?,?,?,?)", (int(bn), abbr, color, chap_count.get(int(bn), 1), k + 1))

# audio
for rows, tr in ((src.execute("select book_number, chapter, sound_url, sound_url1 from soundbible"), 1),
                 (src.execute("select book_number, chapter, sound_url, '' from KJV_soundbible"), 3),
                 (src.execute("select book_number, chapter, sound_url, '' from LS_soundbible"), 4)):
    for b, c, u, u2 in rows:
        dst.execute("INSERT OR REPLACE INTO bible_audio VALUES(?,?,?,?,?)", (tr, b, c, u, u2))

dst.execute("INSERT INTO meta VALUES('content_version','1')")
dst.commit()
dst.execute("VACUUM")
dst.close()
print("tags inconnus:", unknown_tags)
print("taille:", os.path.getsize(DST) / 1e6, "Mo")
