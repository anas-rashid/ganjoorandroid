"""Builds the app's dictionary from Wiktionary (CC BY-SA 3.0) and Daneshjoo (MIT).

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
        entries.append((normalise(word), word, f"({pos}) {gloss}" if pos else gloss, 'wiktionary'))
    for f in e.get('forms', []):
        t = f.get('form')
        if t and t != word and not t.startswith('-') and len(t) > 1:
            forms.add((normalise(t), normalise(word)))

print(f"wiktionary: {len(entries)} entries, {len(forms)} forms")

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
