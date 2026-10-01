# solar-microgrid-mobile

Smart Solar Microgrid Trading System, the Android app. Pure native Android with a local SQLite database, used by prosumers and by Grid Operators in operator mode. Talks only to the Web API in the solar-microgrid-web repo.

## Folders

app is the Android Studio module. Source lives in app/src/main/java/com/solarmicrogrid/app, screens and layouts in app/src/main/res.

## Needed

Android Studio, a JDK, an emulator or a device with Google Play services for the map.

The gradle wrapper (gradlew, gradlew.bat, gradle/) is committed. Open this folder in Android Studio and let it sync, or run `./gradlew build` from a terminal.

## How we work

Every member commits from their own account, small steps, short lower case commit messages saying what changed.

## Who did what

| Member | Name | Contribution |
|---|---|---|
| 1 | Sithum Madhuranga | Login and roles, prosumer registration and profile, account deactivation |
| 2 | Christine Lowe | Nearby nodes map, node details and slots |
| 3 | Sathush Nanayakkara | Reserve, modify and cancel energy slots, the 7-day and 12-hour rules |
| 4 | Nimnath Nadushka | Dashboard, booking history and search, operator QR scan and verify, SQLite storage |
