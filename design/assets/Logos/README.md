# Logos

The mark is an eight-pointed star (khatam) from Persian tilework, drawn as a band and holding a
gold stone at its centre — Khazeena is خزینه, a treasury, and the star is the setting rather than
the ornament. The app has no wordmark: the name is set in type (Noto Naskh Arabic) wherever it
appears.

- `khazeena-mark.svg`: the mark as composed, band and stone raised off the teal field. The field
  is a gradient (`#0A7E84` → `#005459`) and the raise is a soft shadow below-right with a cyan
  bounce above-left, both mixed from the field, so the band reads as worked out of the surface.
- `khazeena-mark-mono.svg`: the flat silhouette, black on transparent. This is what Android 13's
  themed icons re-tint, and the only version most Material You users ever see.

## Why the launcher foreground ships as PNG

`VectorDrawable` has gradients but no blur, so the raise cannot be expressed in vector. The
adaptive foreground is therefore rendered per density (`drawable-mdpi` … `drawable-xxxhdpi`);
the background and the monochrome layer stay vector.

## Don't

Recolour the stone, add a wordmark, or outline the band. The gap between band and stone is
load-bearing: it is the only reason the mark survives being re-tinted to a single colour.
