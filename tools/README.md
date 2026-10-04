# Building the dictionary

`app/src/main/assets/dictionary.db.gz` is generated, not hand-written. This rebuilds it:

```sh
pip install readmdict python-lzo          # lzo needs the C library: brew install lzo
curl -L -o fa.jsonl https://kaikki.org/dictionary/Persian/kaikki.org-dictionary-Persian.jsonl
curl -L -o daneshjoo.mdx \
  "https://raw.githubusercontent.com/0xdolan/Daneshjoo/main/Daneshjoo%20Dictionary/Daneshjoo%20Dictionary.mdx"
python3 build_dictionary.py
gzip -9 ganjoor-dictionary.db
```

## Why two sources

Neither alone is enough. Measured against every distinct word in five real poems (Hafez ×2,
Saadi's Golestan, Rumi's Masnavi, a Khayyam rubaʿi — 485 words):

| | coverage |
|---|---|
| Daneshjoo alone | 71% |
| Wiktionary alone + its form index | ~80% |
| **Both, with the lookup chain** | **88%** |

13 of the 55 remaining misses are Arabic lines quoted inside Persian poems, so Persian coverage
is about 91%.

Wiktionary's export is 93 MB, of which the definitions are 0.94 MB — the rest is inflection
tables, etymology templates, IPA and descendants. The build keeps the definitions and the
form→lemma index (149,589 pairs) and drops the rest. That index is what resolves conjugated
verbs: `افتاد → افتادن`, `بگشاید → گشودن`, `دانند → دانستن`.

## Urdu sources

```sh
curl -L -o ur.jsonl https://kaikki.org/dictionary/Urdu/kaikki.org-dictionary-Urdu.jsonl
curl -L -o urwikt.xml.bz2 \
  https://dumps.wikimedia.org/urwiktionary/latest/urwiktionary-latest-pages-articles.xml.bz2
bunzip2 -k urwikt.xml.bz2
```

The first gives Urdu headwords glossed in English. The second is Urdu Wiktionary itself, the only
source here whose definitions are written **in Urdu** — thin (around 3,100 usable entries out of
31,000 pages, many being stubs), but for a word it does carry an Urdu reader is better served by
it than by a translation into English.

## Arabic

```sh
curl -L -o ar.jsonl https://kaikki.org/dictionary/Arabic/kaikki.org-dictionary-Arabic.jsonl
```

521 MB, almost all of it the inflection index, and that index is the point: the Arabic quoted
inside Persian verse is conjugated, so السّاقی, الناس, تَلْقَ and تَهْوی only reach a definition
through it. 36,627 entries and 819,608 new form pairs for about 59 MB of database.

## Why there is no Persian-to-Urdu

Wiktionary's Persian entries carry no translations at all — the translation tables live only on
English pages, in a 3.3 GB export. Going Persian to Urdu would mean pivoting through an English
sense, and a sample of that file projects only about 6,900 Persian words with any Urdu
equivalent, most of them modern dictionary vocabulary rather than the language of the poems. The
definitions written in Urdu therefore come from Urdu Wiktionary directly, and are preferred over
the English ones wherever they exist.

## Licences

Each row carries its `source`, so attribution stays accurate and either source can be dropped
without rebuilding the other.

- **Wiktionary** — CC BY-SA 3.0. The generated database is therefore also CC BY-SA 3.0.
- **Daneshjoo** — the repository states MIT. Note the underlying lexicon is a published Iranian
  dictionary, so that relicensing is worth verifying before relying on it.
