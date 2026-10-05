# solar-microgrid-mobile

The Android app for our SE4040 assignment, written in Kotlin. Prosumers use it, and grid operators use it in operator mode. It keeps the login in SQLite on the phone and talks only to the Web API in the solar-microgrid-web repo.

Git repositories:

- Android app: https://github.com/sithummadhuranga/solar-microgrid-mobile
- Web API and web app: https://github.com/sithummadhuranga/solar-microgrid-web

## Folders

app is the Android Studio module. The code is in app/src/main/java/com/solarmicrogrid/app and the screens are in app/src/main/res.

## Needed

Android Studio, a JDK, and an emulator or phone with Google Play services for the map. The app needs Android 7.0 (API 24) or newer. The gradle wrapper is in the repo, so open the folder in Android Studio and let it sync.

## Settings

Put the Google Maps key in `local.properties`. That file is not committed.

```
MAPS_API_KEY=your-key
```

The api address is `baseUrl` in `app/src/main/java/com/solarmicrogrid/app/api/ApiClient.kt`. It points at the hosted api, https://api-solarmicrogrid.sithum.dev/api. To use an api on your own computer, change it to `http://10.0.2.2:5080/api` (the emulator reaches your computer at 10.0.2.2) and do not commit the change.

## Logging in

A prosumer registers in the app and stays pending until a Backoffice user activates the account in the web app. A grid operator is added by a Backoffice user in the web app. Both log in on the same screen. Backoffice accounts cannot log in on the phone.

## Who did what

| Member | Name | Contribution |
|---|---|---|
| 1 | Sithum Madhuranga | Login and roles. Web users, prosumers and pending activations. The web layout and api helper. IIS hosting. On Android: login, register and profile. |
| 2 | Christine Lowe | Nodes and slots in the api and on the web. Sample node data. The map screen on Android. |
| 3 | Sathush Nanayakkara | Reservations: create, change, cancel and approve, with the 7 day and 12 hour rules. The web reservations page. On Android: reserve, modify, cancel, summary and QR screens. |
| 4 | Nimnath Nadushka | Booking lists, history and dashboards. QR verify and complete. The web booking monitor and operator home. On Android: dashboard, history, search, SQLite and the operator scan screens. |

Everyone commits from their own account. Sathush commits as Sathufit and as G S R Nanayakkara.

## Demo video

[Watch the demo video](https://mysliit-my.sharepoint.com/:v:/g/personal/it23294066_my_sliit_lk/IQAljAkOOszgTb6KeODPJiayAUKaTudZiSvAkkNeI6I1FOU?nav=eyJyZWZlcnJhbEluZm8iOnsicmVmZXJyYWxBcHAiOiJTdHJlYW1XZWJBcHAiLCJyZWZlcnJhbFZpZXciOiJTaGFyZURpYWxvZy1MaW5rIiwicmVmZXJyYWxBcHBQbGF0Zm9ybSI6IldlYiIsInJlZmVycmFsTW9kZSI6InZpZXcifX0%3D&e=oiL80W)
