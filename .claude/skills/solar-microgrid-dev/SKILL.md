---
name: solar-microgrid-dev
description: Rules for writing all code, comments, UI text, commit messages and docs for the Smart Solar Microgrid Trading System (C# Web API with MongoDB on IIS, a web app, and a pure native Android app with SQLite). Use this skill whenever writing, editing, reviewing or explaining any code or text for this project, including controllers, services, models, web pages, Android activities, layouts, SQLite code, README files and commit messages, even for a small change. It makes the output plain, simple and human-written with none of the usual AI patterns, and it points to PROJECT_SCOPE.md for the full scope. Read PROJECT_SCOPE.md before writing anything.
---

# Smart Solar Microgrid Trading System: development rules

## This repo

This clone (`solar-microgrid-mobile`) holds Member 3's working copy. Member 3's slice, from PROJECT_SCOPE.md section 11:

- Reservations: `W-6` (web, as a UI layer only), `M-4`, `M-5`.
- API and database: reservation endpoints, `BR-4`, `BR-5`, the `Energy Reservation` collection.

Other members' web, node/map and view/operator work lives in their own clones of the same scope document. Do not build outside this slice without checking with the team first.

## Read this first

1. Read `PROJECT_SCOPE.md` at the repo root before you write anything. It has the requirements, the business rules and the open decisions.
2. Every task maps to requirement IDs in that file (W-4, M-5, S-1 and so on). Say which IDs you are working on before you write code.
3. If the scope does not say something, ask one short question. Do not invent fields, endpoints, screens, libraries or rules. An empty entry in the decisions section means "not decided", so ask.
4. Build one small working piece at a time. Finish it, check it, then move on.
5. Do only what the task asks. Do not add features, layers or files nobody asked for.

A good task from the user looks like this. If a part is missing, ask for it.

```
Task: one thing to build
Requirement IDs: M-4, BR-5
My member number: 3
Files that already exist: list them
```

## Who you are writing as

Write like a careful junior developer on a normal day. The code is plain and direct. It is fine if it is a little repetitive. It should never look like it was designed to impress someone.

## The hard rules

1. No emojis anywhere. Not in code, comments, logs, screen text, docs or commit messages.
2. No em dashes, no en dashes, no arrows as punctuation, no curly quotes, no ellipsis character. Use a comma, a period, a colon or a plain hyphen. Use straight quotes.
3. Keep it simple. Use the fewest files, classes and lines that meet the requirement.
4. Business logic lives only in the API. The web app and the Android app show data and send requests.
5. Never say something works unless you ran it. Say what you checked and what you did not check.
6. Every .cs file starts with a comment header block, and every method, in any language, starts with a comment. The document says code without these will not be marked.
7. Screenshots must be unique and come from the real running app. The document says work without unique screenshots will not be marked. Never invent or mock up a screenshot. Ask the user to capture it.

## Names

- Use plain words a student would type: `reservation`, `station`, `slots`, `user`, `nic`.
- One item is singular, a list is plural: `reservation`, `reservations`.
- Booleans start with `is` or `has`: `isActive`.
- Follow the language habit: PascalCase for C# classes, methods and properties, camelCase for locals and fields, camelCase in Java, Kotlin, JavaScript and TypeScript.
- Do not end names with Manager, Helper, Handler, Processor, Provider, Factory, Wrapper, Util, Impl or Base unless the framework needs it.
- Use one word for one thing everywhere. The document calls the same thing a hub, a node, a station and a grid node. The Words table in PROJECT_SCOPE.md says which word to use in code and which to show on screen.

Good and bad:

```
reservations             not  reservationCollectionResult
cancelReservation        not  ProcessReservationCancellationRequest
user                     not  currentAuthenticatedUserObject
isActive                 not  activeStatusFlag
```

## Structure

- Backend folders are Controllers, Services and Models. Nothing else unless the user asks.
- Do not add a repository layer, interfaces for every service, MediatR, AutoMapper, FluentValidation, a Result wrapper type, a base controller, or a response envelope like `ApiResponse<T>`. Return plain objects and normal status codes.
- Make a separate request class only when the request body is different from the model, for example login.
- Android has activities, layouts, model classes, one database helper and one class for calling the API.
- Web has pages, one small file for API calls and one shared layout.
- If a method is over about 30 lines, a file is over about 250 lines, or a class has more than about 8 methods, stop and ask if it needs to be that big.
- Do not add things the scope does not ask for: caching, retry logic, a logging framework, translations, dark mode, pagination on small lists, test scaffolding.

## Errors and validation

- The API checks everything: required fields, business rules and roles. It answers with a short plain message and the right status code (400, 401, 403, 404).
- The clients only check that required boxes are filled before sending. Then they show the message the API sent back.
- No try/catch around normal API code. Catch an error only where you can do something about it. In Android network code you must catch, so the app does not crash and can show the message.
- Do not null check everything. Check where null can really happen, for example a lookup by id that finds nothing.

## Comments

The document says: "Code that does not include the following will not be marked": a comment header block on each .cs file, inline comments at the beginning of each method, and unique screenshots of the application. So these are not optional. Every .cs file gets the header. Every method gets a comment at its beginning, in C#, Java, Kotlin and JavaScript alike, including private methods, controller actions, constructors and one-line methods. Write them in this plain style.

File header:

```csharp
// File: ReservationService.cs
// Purpose: rules for creating, updating and cancelling reservations
// Author: member name from the decisions section
```

Method comment, one line, says what the method does or which rule it applies:

```csharp
// cancels a reservation, needs at least 12 hours notice
```

Rules for comments:

- Use `//` comments. No `/// <summary>` blocks, no `/** */` blocks, no `#region`, no banner lines made of `=` or `-`.
- One line is enough. Say what or why, not a story.
- Start with a verb in lower case: "gets", "checks", "saves".
- Never write "This method", "Here we", "Note that", "Ensure that", "Utilize", "Leverage", "Robust", "Seamless", "Comprehensive".
- Do not comment lines that explain themselves. No `// increment i`.
- Comment a business rule where it is enforced: `// must be within 7 days`.
- If you copy code from a tutorial or any other source, say so in a comment with the source. The document treats unreferenced copied code as plagiarism.
- No commented-out code, no TODO trails, no "removed", no notes about older versions of the code.
- The header block is required for .cs files only. For Java, Kotlin, JavaScript, TypeScript and XML files add a one-line comment at the top only when the file's job is not obvious from the name. The method comment rule still applies to those languages.

## Text shown to users

- Short and plain: "Reservation saved", "Could not cancel, less than 12 hours left".
- No exclamation marks, no "Oops", no "successfully", no "Please note", no "Something went wrong" when the API gave a real message.
- Sentence case for buttons, headings and labels: "Add station".
- Use the document's own words in labels: NIC, "Capacity (kW/h)", "Battery storage slots".

## Data

- Store dates in MongoDB as UTC. Convert only when showing them.
- Passwords are hashed with a well-known library and never stored as plain text. Never write your own hashing.
- The MongoDB connection string and the Google Maps API key stay out of git.
- Sample data is small, plain and believable: a few users, three or four stations, some slots, some reservations. No lorem ipsum.

## Backend: C# Web API, MongoDB, IIS

Checked against Microsoft and MongoDB documentation:

- Hosting on IIS needs the .NET Hosting Bundle on the server. It installs the .NET runtime and the ASP.NET Core Module. If the bundle was installed before IIS, run the installer again after IIS is in place.
- Use one .NET version for the whole API. The chosen version is in the decisions section, and the server needs the matching Hosting Bundle.
- Use the `MongoDB.Driver` NuGet package. Create one `MongoClient` and reuse it for the whole app (register it once as a singleton). Do not create a client per request.
- Use typed collections with plain model classes: `database.GetCollection<T>("Name")`. Use the async methods such as `InsertOneAsync`, `Find(...).ToListAsync()` and `ReplaceOneAsync`.
- The connection string goes in appsettings, not in code.

Rules for this project:

- Every rule from the business rules table is written once, in a service, and nowhere else. Controllers read the request, call the service and return the result.
- Every endpoint that changes data checks the role of the caller in the API.
- Allow the web app's origin with CORS if the web app runs on a different address from the API.
- Return plain objects, not envelopes.

Style example. The route and names are only an example, the real ones come from PROJECT_SCOPE.md.

```csharp
// File: ReservationService.cs
// Purpose: rules for creating, updating and cancelling reservations
// Author: member name from the decisions section

public class ReservationService
{
    private readonly IMongoCollection<EnergyReservation> reservations;

    public ReservationService(IMongoDatabase db)
    {
        reservations = db.GetCollection<EnergyReservation>("EnergyReservation");
    }

    // cancels a reservation, needs at least 12 hours notice
    // returns an error message, or null when it worked
    public async Task<string?> Cancel(string id)
    {
        var reservation = await reservations.Find(r => r.Id == id).FirstOrDefaultAsync();
        if (reservation == null) return "Reservation not found";

        if (reservation.StartTime - DateTime.UtcNow < TimeSpan.FromHours(12))
            return "Reservations can only be cancelled 12 hours before the start time";

        reservation.Status = "cancelled";
        await reservations.ReplaceOneAsync(r => r.Id == id, reservation);
        return null;
    }
}
```

```csharp
// cancels a reservation
[HttpPost("{id}/cancel")]
public async Task<IActionResult> Cancel(string id)
{
    var error = await service.Cancel(id);
    if (error != null) return BadRequest(error);
    return Ok();
}
```

## Web application

- The UI framework is chosen in the decisions section. The document allows Bootstrap 5, Tailwind CSS or React.js. Do not mix them, and do not add another UI library.
- UI only. No business rules. The web app talks only to the Web API, through one helper file. No direct database access.
- Hide menu items the user's role cannot use, but only as a convenience. The API is what blocks them.
- Look: plain admin style. A navbar, a page title, a table or a form, buttons. Use the framework's default components and colours.
- Do not add gradients, glass effects, shadows on everything, animated transitions, hero banners, marketing text, icon packs, emojis, illustrations, skeleton loaders, toast libraries or a dark mode switch.
- Every page uses the same layout, the same navbar and the same button styles. Primary button for save, danger button for cancel and deactivate, and ask to confirm before those two.
- Lists are tables. Add and edit use a form on the page or a simple modal. Show the API error text above the form.
- Responsive comes from the framework's grid. Write your own media queries only when something breaks.
- The home page is short: a title and links that fit the role. No marketing copy.
- If the choice is React: function components, `useState`, `useEffect` and `fetch`. No Redux, no React Query, no form libraries. Keep state inside the page. Use a small context only for the logged in user, and only if you need it. Make a custom hook only when the same code appears in three places.

Style example for the API helper:

```js
// calls the api and throws the server message if it fails
async function callApi(path, method = "GET", body = null) {
  const res = await fetch(API_URL + path, {
    method: method,
    headers: { "Content-Type": "application/json" },
    body: body ? JSON.stringify(body) : null,
  });
  const text = await res.text();
  if (!res.ok) throw new Error(text);
  return text ? JSON.parse(text) : null;
}
```

## Android application

- Pure native Android. No cross-platform framework. Any library beyond the Android SDK and AndroidX must be written in the decisions section before you add it. The Google Maps SDK is required by the document, so it is allowed.
- The language (Java or Kotlin) is in the decisions section. Use only that one.
- SQLite through `SQLiteOpenHelper`. It stores login details and reference data only. No business logic in the database code.
- Never do network calls on the main thread. Use a plain `Thread` or an `ExecutorService`, then `runOnUiThread` to update the screen.
- One activity per screen, XML layouts, standard widgets, `RecyclerView` for lists. Text goes in `strings.xml`.
- Maps: `SupportMapFragment` or `MapView` with an `OnMapReadyCallback`. The Maps API key goes in a local secrets file that is not committed. Google recommends `secrets.properties` and warns against putting the key in version control.
- Ask for the camera permission before scanning a QR code. Ask for the location permission only if the map really uses the phone's location.
- After create, update or cancel of a reservation, open a summary screen. The marking scheme asks for it.
- The QR content and its verification are decided in the API. The app draws the QR and scans it, and shows the result the API sends back.
- Look: stock Android widgets, even padding, no custom animations, no emojis, no decorative icons.

Style example (Java shown, use the same shape in Kotlin):

```java
// loads the reservations of the logged in prosumer
private void loadReservations() {
    new Thread(() -> {
        try {
            String json = api.get("/reservations/mine");
            List<Reservation> list = Reservation.listFromJson(json);
            runOnUiThread(() -> adapter.setItems(list));
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }).start();
}
```

## Git and writing

- Commit messages are short, lower case, present tense, one change each: `add cancel reservation endpoint`. No prefixes, no emojis, no long body.
- Each member commits with their own account, in small steps, so the history shows who built what.
- Screenshots go in the report and must be unique. Do not generate, fake or reuse them. If a task needs one, tell the user which screen to capture.
- README and other docs are plain prose in the team's voice. Sentence case headings. No bold labels on list items, no badges, no emojis, no feature lists that read like an advert, no "This project aims to". Say what it is, how to run it, how to host it on IIS, the video link and who did what.
- Chat replies while developing are short and direct. No "Certainly", no "Great question", no recap of what you just did unless asked, no closing offer of more help.

Writing habits to avoid in comments, docs and replies:

- "Not X but Y" sentences, and "It is not just X, it is Y".
- A one-line paragraph that repeats the paragraph before it.
- "Let's dive in", "Here's the thing", "Here is a".
- Three parallel items in a row when two would do.
- Inflated words: robust, seamless, comprehensive, crucial, pivotal, leverage, utilize, streamline, ensure.
- "Serves as", "stands as", "features", "boasts". Use "is" and "has".
- Title Case Headings.

## Before you finish

Check each point:

- [ ] The task maps to requirement IDs and nothing outside them was added.
- [ ] Business rules are in the API only.
- [ ] Every .cs file has the header block. Count the files, do not assume.
- [ ] Every method in every language has a one-line comment at its start, including private ones and constructors.
- [ ] Any screenshot used is real, taken from the running app, and not repeated.
- [ ] Names are plain and match the Words table.
- [ ] No emojis, no long dashes, no curly quotes, no arrows.
- [ ] No unused files, no commented-out code, no leftover placeholders.
- [ ] You said what you ran and what you did not run.

Scan for banned characters from the project root. It prints nothing when the files are clean.

```
LC_ALL=C.UTF-8 grep -rnP "[\x{2013}\x{2014}\x{2018}\x{2019}\x{201C}\x{201D}\x{2026}\x{2192}\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]" --include=*.cs --include=*.java --include=*.kt --include=*.xml --include=*.js --include=*.jsx --include=*.ts --include=*.tsx --include=*.html --include=*.css --include=*.md --include=*.json .
```
