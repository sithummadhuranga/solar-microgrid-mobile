# solar-microgrid-mobile

Smart Solar Microgrid Trading System, the Android app. Pure native Android with a local SQLite database, used by prosumers and by Grid Operators in operator mode. Talks only to the Web API in the solar-microgrid-web repo.

## Folders

Full scope and requirements are in PROJECT_SCOPE.md at the repo root. Coding rules are in .claude/skills/solar-microgrid-dev/SKILL.md.

Android project structure below, in Kotlin (D-1).

- `app/src/main/java` activities, models, `ApiClient`, `SQLiteOpenHelper`
- `app/src/main/res` layouts, strings, drawables

## Needed

Android Studio, a JDK, an emulator or a device with Google Play services for the map.

The gradle wrapper jar and gradlew scripts are not committed yet, since they need a machine with Gradle or Android Studio to generate. Open this folder in Android Studio and let it prompt to set up the wrapper on first sync, or run `gradle wrapper --gradle-version 8.7` yourself if you have Gradle installed.

## How we work

Every member commits from their own account, small steps, short lower case commit messages saying what changed.

## Who did what

| Member | Name | Contribution |
|---|---|---|
| 1 | | |
| 2 | | |
| 3 | | |
| 4 | | |
