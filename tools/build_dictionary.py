"""Builds the app's dictionary from Wiktionary (CC BY-SA 3.0) and Daneshjoo (MIT).

Three sources, each row tagged so the app can say which one answered and in which language:
  wiktionary-fa  Persian headwords, English definitions
  daneshjoo      Persian headwords, English definitions
  wiktionary-ur  Urdu headwords, English definitions


Wiktionary's export is 93 MB of linguistic metadata around 0.94 MB of definitions, so this keeps
the definitions and the form->lemma index and discards the rest. The `source` column is what
keeps the attribution honest and lets either source be dropped later.
"""
import json, re, sqlite3, unicodedata, os, html
from readmdict import MDX

HARAKAT = set(range(0x064B, 0x0653)) | {0x0670, 0x0640} | set(range(0x0610, 0x0616))
# Arabic letters that Persian writes differently; headwords and poems disagree constantly.
FOLD = {'ي': 'ی', 'ى': 'ی', 'ك': 'ک', 'ة': 'ه'}

def normalise(s: str) -> str:
    d = unicodedata.normalize('NFD', s)
    d = ''.join(c for c in d if ord(c) not in HARAKAT)
    s = unicodedata.normalize('NFC', d)
    return ''.join(FOLD.get(c, c) for c in s).replace('‌', '').strip()

db = 'ganjoor-dictionary.db'
if os.path.exists(db): os.remove(db)
c = sqlite3.connect(db)
c.executescript("""
CREATE TABLE entry (word TEXT NOT NULL, display TEXT NOT NULL, gloss TEXT NOT NULL, source TEXT NOT NULL);
CREATE TABLE form  (form TEXT NOT NULL, lemma TEXT NOT NULL);
""")

entries, forms = [], set()

for line in open('fa.jsonl', encoding='utf-8'):
    try: e = json.loads(line)
    except Exception: continue
    word = e.get('word')
    if not word: continue
    gs = [g.strip() for s in e.get('senses', []) for g in (s.get('glosses') or []) if g.strip()]
    if gs:
        pos = e.get('pos') or ''
        gloss = '; '.join(dict.fromkeys(gs))[:600]
        entries.append((normalise(word), word, f"({pos}) {gloss}" if pos else gloss, 'wiktionary-fa'))
    for f in e.get('forms', []):
        t = f.get('form')
        if t and t != word and not t.startswith('-') and len(t) > 1:
            forms.add((normalise(t), normalise(word)))

print(f"wiktionary-fa: {len(entries)} entries, {len(forms)} forms")

# Urdu shares a great deal of vocabulary with Persian, so these entries answer words the
# Persian sources miss. Headwords are Urdu; the definitions are still English.
n_fa = len(entries)
for line in open('ur.jsonl', encoding='utf-8'):
    try: e = json.loads(line)
    except Exception: continue
    word = e.get('word')
    if not word: continue
    gs = [g.strip() for s in e.get('senses', []) for g in (s.get('glosses') or []) if g.strip()]
    if gs:
        pos = e.get('pos') or ''
        gloss = '; '.join(dict.fromkeys(gs))[:600]
        entries.append((normalise(word), word, f"({pos}) {gloss}" if pos else gloss, 'wiktionary-ur'))
    for f in e.get('forms', []):
        t = f.get('form')
        if t and t != word and not t.startswith('-') and len(t) > 1:
            forms.add((normalise(t), normalise(word)))
print(f"wiktionary-ur: {len(entries) - n_fa} entries, {len(forms)} forms total")

# Arabic, for the lines classical Persian quotes outright — Hafez opens with one.
if os.path.exists('ar.jsonl'):
    n_ar, f_ar = len(entries), len(forms)
    for line in open('ar.jsonl', encoding='utf-8'):
        try: e = json.loads(line)
        except Exception: continue
        word = e.get('word')
        if not word: continue
        gs = [g.strip() for s in e.get('senses', []) for g in (s.get('glosses') or []) if g.strip()]
        if gs:
            pos = e.get('pos') or ''
            gloss = '; '.join(dict.fromkeys(gs))[:600]
            entries.append((normalise(word), word, f"({pos}) {gloss}" if pos else gloss, 'wiktionary-ar'))
        for f in e.get('forms', []):
            t = f.get('form')
            if t and t != word and not t.startswith('-') and len(t) > 1:
                forms.add((normalise(t), normalise(word)))
    print(f"wiktionary-ar: {len(entries) - n_ar} entries, {len(forms) - f_ar} new forms")

tag = re.compile(r'<[^>]+>')
n0 = len(entries)
for k, v in MDX('daneshjoo.mdx').items():
    word = k.decode('utf-8', 'ignore')
    if not re.match(r'^[؀-ۿ]', word):
        continue                                    # the en->fa half isn't useful here
    txt = html.unescape(tag.sub(' ', v.decode('utf-8', 'ignore')))
    txt = re.sub(r'\s+', ' ', txt).strip()
    if txt.startswith(word):
        txt = txt[len(word):].strip(' -–—')
    if txt:
        entries.append((normalise(word), word, txt[:600], 'daneshjoo'))
print(f"daneshjoo : {len(entries) - n0} entries")

# Urdu Wiktionary, the only source here whose definitions are written in Urdu rather than
# English. Thin — a few thousand usable entries, many pages being stubs — but for a word it
# does carry, an Urdu reader is better served by it than by a translation into English.
if os.path.exists('urwikt.xml'):
    import html as _html
    raw = open('urwikt.xml', encoding='utf-8', errors='ignore').read()
    n_ur = len(entries)
    for title, ns, body in re.findall(
        r'<title>(.*?)</title>.*?<ns>(\d+)</ns>.*?<text[^>]*>(.*?)</text>', raw, re.S
    ):
        if ns != '0' or not re.match(r'^[\u0600-\u06FF]', title):
            continue
        t = re.sub(r'\{\{[^}]*\}\}', ' ', body)
        t = re.sub(r'\[\[([^\]|]*\|)?([^\]]*)\]\]', r'\2', t)
        t = re.sub(r"'{2,}|<[^>]+>", '', _html.unescape(t))
        section = re.search(r'==\s*معانی\s*==(.*?)(?:\n==|\Z)', t, re.S)
        lines = (section.group(1) if section else
                 '\n'.join(l.strip(' #') for l in t.split('\n') if l.strip().startswith('#')))
        kept = []
        for line in lines.split('\n'):
            line = re.sub(r'^\d+\.\s*', '', line.strip())
            # ؎ introduces a verse citation, and "ref"/a year starts the source note; the
            # definition itself is what comes before either.
            if line.startswith('؎') or re.match(r'^\(?\s*\d{3,4}ء', line):
                break
            line = re.split(r'\bref\b|؎', line)[0].strip()
            if not line or not re.search(r'[\u0600-\u06FF]', line):
                continue
            kept.append(line)
            if len(' '.join(kept)) > 220:
                break
        gloss = ' '.join(kept).strip()[:300]
        if len(gloss) > 3:
            entries.append((normalise(title), title, gloss, 'urwiktionary'))
    print(f"urwiktionary : {len(entries) - n_ur} entries (definitions in Urdu)")

c.executemany("INSERT INTO entry VALUES (?,?,?,?)", entries)
c.executemany("INSERT INTO form  VALUES (?,?)", sorted(forms))
c.executescript("""
CREATE INDEX idx_entry_word ON entry(word);
CREATE INDEX idx_form_form  ON form(form);
""")
c.commit()
c.execute("VACUUM")
c.close()
print(f"\n{db}: {os.path.getsize(db)/1e6:.1f} MB")
