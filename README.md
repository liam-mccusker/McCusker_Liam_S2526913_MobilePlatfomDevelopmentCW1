# Scottish Road Situations – MPD Coursework 1

Liam McCusker – S2526913 – Glasgow Caledonian University

Android app (Java, MVVM) showing Traffic Scotland unplanned events and current
roadworks from the DATEX II feeds: list with filters, details, road and distance
search, colour-coded delays, map, and offline support.

## Running the project
The API keys are not stored in this repository. To run with live data and maps,
add these lines to `local.properties` (no quotes, no spaces):

    MAPS_API_KEY=your-google-maps-key
    TS_CLIENT_ID=your-traffic-scotland-client-id
    TS_CLIENT_KEY=your-traffic-scotland-client-key

Without them the project still builds and runs: `local.defaults.properties`
supplies placeholder (non-working) values so the build succeeds. The app then
shows "No Traffic Scotland API credentials in this build" and the map is blank.
The submitted .apk was built with working keys, so it shows live data and maps.

## Icons
Google Material Icons, added through Android Studio's Vector Asset tool.
Licence: Apache License 2.0.