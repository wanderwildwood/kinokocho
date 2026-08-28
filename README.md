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
- **Then ask someone:** push the observation to your own iNaturalist account, photographs
  and characters and all, and let the community identify it. The identification comes back
  into your journal. The app never guesses; it helps you ask.

## Why E Ink is the right screen for this

Colour is diagnostic in mushroom identification and E Ink cannot show it. That would be
fatal for an identification app — but this is a *recording* app. Colour matters when you
decide, and deciding happens at home, on a screen that has it. In the field what you want
is a notebook that lasts days on a charge, reads in direct sun, and does not have anything
else on it.

## Structure

The character schema is global; the taxon data is a swappable region pack. Southern
Appalachia is pack one.

## Status

Early. Repository scaffolding and a placeholder screen — the character schema is being
drawn up now. The launcher icon is a placeholder and not a design.

## Building

```sh
./gradlew testDebugUnitTest
./gradlew assembleRelease
```

Release builds are signed with a keystore in `signing/`, which is gitignored. Without it
the build falls back to the default debug key rather than to a checked-in one, because a
signing key committed to a public repo is not a signing key, it is a formality.

## Licence

GPL-3.0-only. See [LICENSE](LICENSE).

Copyright (C) 2026 wander wildwood.
