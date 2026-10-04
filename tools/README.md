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

## Licences

Each row carries its `source`, so attribution stays accurate and either source can be dropped
without rebuilding the other.

- **Wiktionary** — CC BY-SA 3.0. The generated database is therefore also CC BY-SA 3.0.
- **Daneshjoo** — the repository states MIT. Note the underlying lexicon is a published Iranian
  dictionary, so that relicensing is worth verifying before relying on it.
