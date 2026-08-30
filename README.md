# 茸帳 kinokochō — Mushroom Journal

A mushroom field journal for the [Mudita Kompakt](https://mudita.com/products/kompakt/),
built for its E Ink screen.

Not a fork. Written from scratch in Kotlin and Jetpack Compose, using Mudita's own
[MMD](https://github.com/mudita/MMD) design system so it looks like the apps the phone
already ships with.

*Kinokochō* is 茸帳 — a mushroom notebook. The 帳 is the one in 手帳 (pocket notebook)
and 野帳 (a surveyor's field book): a plain book you fill by going places.

## What it is, and what it is not

**It does not tell you what a mushroom is, and it will never tell you whether one is
edible.** There is no edibility field anywhere in it, by design. A phone that answers that
question confidently is a phone that eventually kills someone.

What it does is let you *write a mushroom down properly* while you are standing over it,
and then help you work out what you are looking at once you get home.

- **In the field:** record the characters you can actually see — substrate, hymenophore,
  gills, cap, stipe, flesh — in any order and however few of them. Add a note. Take
  photographs into labelled slots. The candidate list narrows as you go, and is always
  presented as *not yet ruled out*, never as an answer.
- **At home:** review the entry on a real screen with the photographs in colour, add the
  spore print you set overnight, and see which unrecorded character would have narrowed
  things most — the part that trains the next walk.
- **Then ask someone:** send the find out as plain text and photographs through whatever
  the phone already has — a message, an email, a forum post. It goes as a description and
  a shortlist of what has *not* been ruled out, with what you looked at and could not say
  said plainly, because that is the first thing anybody experienced will ask.
- **And keep a copy:** the whole journal, photographs and all, written to a zip and handed
  wherever you want it, and read back in on the other side of a lost phone. The database
  holds notes that cannot be taken again — the mushroom is gone and the season is over —
  and a book that lives in exactly one place is a book with a date on it. Reading a copy
  back in only ever adds: an old backup onto a journal that has been used since keeps
  both.
- **And write down the answer:** when somebody tells you what it was, there is a line to
  put it on. That is the whole point of keeping the book.

## Why E Ink is the right screen for this

Colour is diagnostic in mushroom identification and E Ink cannot show it. That would be
fatal for an identification app — but this is a *recording* app. Colour matters when you
decide, and deciding happens at home, on a screen that has it. In the field what you want
is a notebook that lasts days on a charge, reads in direct sun, and does not have anything
else on it.

## Structure

The character schema is global; the taxon data is a swappable region pack. Southern
Appalachia is pack one.

Everything generated is generated: the drawings from `tools/make-art.py`, the pack from
the `tools/add-*.py` batches, the illustrator's brief from the pack itself. A file that
has to be kept in step by hand is a file that drifts, and a work order that has drifted
from the data is worse than none.

## Also in it

- **A month view.** What is about now, split evenly between the ones people go looking for
  and the ones that hurt people — which are often out at the same time. Anything lethal is
  listed however rarely it turns up. "Sought after" means people look for it; it is not
  the app saying anything is safe.
- **A page per mushroom.** What it looks like, where and when, what it is confused with and
  how to tell them apart, and how what you recorded lines up against it. Reachable from a
  shortlist, from the month view, or by looking a name up in any month.
- **Measurements, age and condition.** Facts about the specimen rather than the species,
  so they are recorded rather than asked. Age earns its place: a ring or a veil that is
  missing from an old mushroom proves nothing, and the key stops ruling things out on it.

## Status

Working and in daily use, and not finished. A hundred and ten taxa in the Southern
Appalachia pack across forty characters, eighty-three pairs that get confused for one
another, a hundred and seventy-four drawings of single characters and thirty-two of whole
mushrooms, and a key that reaches the right taxon in about four questions when the answers
are true.

Nothing in the pack has been checked by a mycologist. Every row says so on its own page.

The launcher mark is a drawing of a chanterelle by the author, traced rather than
imitated. The rest of the drawings are worked to the same hand — thin, few lines, no
solid black — which is written down at the top of `tools/make-art.py`.

## Building

```sh
./gradlew testDebugUnitTest
./gradlew assembleRelease
```

The release build is minified. It is worth actually running rather than only building:
R8 takes it from 32 MB to under 3, and a minified Android app is where the surprises
live. This one has no keep rules and needs none — see `app/proguard-rules.pro` for why.

Builds are signed with a keystore in `signing/`, which is gitignored. There is no
fallback: without it the build produces an *unsigned* APK, which will not install
anywhere. That is deliberate — a signing key committed to a public repo is not a signing
key, it is a formality, and a missing one should stop you rather than quietly hand you
something installable.

**The debug build takes the same key**, which this file used to deny. It matters: the
first install of any variant fixes the signer for good, so a debug build put on a phone is
not a throwaway — install one signed with the Android debug key and the real one will not
go over the top of it.

## Licence

GPL-3.0-only. See [LICENSE](LICENSE).

Copyright (C) 2026 wander wildwood.
