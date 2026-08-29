# Log.d and Log.v are stripped from release builds by proguard-android-optimize.txt's
# assumenosideeffects rules. Use Log.i for anything that has to survive into a release
# build, and do not trust the absence of a Log.d line as evidence of anything.

# There are no keep rules here, and that is a finding rather than an omission.
#
# The release build was taken end to end on a device: R8 shrinks 32 MB to 2.9 MB, and
# the app then loads its JSON schema and region pack, opens Room, and survives three
# thousand random input events without one error from its own process. Nothing in it
# reaches for a class by name — the pack is parsed field by field with org.json rather
# than reflected into, and Room generates its own code — so there is nothing for R8 to
# strip out from under it.
#
# Do not add speculative keep rules. A keep rule that is not needed is a piece of the
# app that can never be optimised again, and nobody afterwards can tell which of them
# were load-bearing. If a release build ever breaks, find the class that actually went
# missing and keep that one.
