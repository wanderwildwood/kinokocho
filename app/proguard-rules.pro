# Log.d and Log.v are stripped from release builds by proguard-android-optimize.txt's
# assumenosideeffects rules. Use Log.i for anything that has to survive into a release
# build, and do not trust the absence of a Log.d line as evidence of anything.
