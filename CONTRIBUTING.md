# Contributing

This is a personal app, written for one phone and one pair of hands. It is public because
it may as well be, not because it is looking for maintainers. That shapes what is useful to
send.

## What is genuinely wanted

**Corrections to the pack.** This is the part that can hurt somebody if it is wrong. A name
that is out of date, a lookalike pair that is missing, a season that is too narrow, a
"tells them apart" line that is not actually diagnostic — those are worth your time and
mine. Open a **Pack correction** issue.

Please say where you are getting it from. Not because the claim is doubted, but because the
pack has a rule: every taxon in it has to be vouched for by an outside authority before the
test suite will accept it, and a correction needs the same footing as the thing it
corrects.

**Bugs.** Especially anything that loses a journal entry or a photograph, and anything that
makes the app look like it is deciding. Open a **Bug report** issue.

## What is probably not

**Feature pull requests.** They are unlikely to be merged, and it is fairer to say so than
to let you write one. If you want the app to do something it does not, open an issue and
say what you were trying to do in the field — that is more useful than a patch, and it does
not cost you an evening.

**Anything that adds edibility.** There is no edibility field, there is nowhere to put one,
and this is not a gap. A phone that answers that question confidently is a phone that
eventually kills somebody. This will not change.

## Security

Do not open a public issue. Use
[private vulnerability reporting](https://github.com/wanderwildwood/kinokocho/security/advisories/new).

## If you do send code

```sh
./gradlew :app:testDebugUnitTest      # what CI runs
python3 tools/make-art.py             # then check `git diff` is empty
```

Both run on every push and pull request. The second one matters more than it looks: every
drawing and the whole taxon pack are generated, and a generated file that has been edited
by hand is a file that will be overwritten one day without anyone noticing. If your change
touches art or pack data, change the generator, not its output.

`tools/verify-names.py --new` before adding a taxon. If no authority vouches for the name,
the mushroom does not exist and it has to come out — a fabricated species otherwise has a
perfect shape and passes every other test.

Commit messages here are plain sentences saying what changed and why, in the present tense.
Have a look at `git log` before writing one.

## Licence

GPL-3.0-only. By contributing you agree your contribution is licensed under it.
