# 🌙 Nightout

**Find out what is happening tonight in any city. Discover events, bars, pubs and clubs that are open right now near you.**

Nightout is an application built with Spring Boot. It collects information about events and nightlife locations from public APIs. The data is stored in a PostgreSQL database with PostGIS. This makes it possible to search for places based on their location and find, for example, which bars are open within 1 km of your current location.

<!-- Add a screenshot: save it as docs/screenshot.png and remove the comment marks around the next line -->
<!-- ![Nightout screenshot](docs/screenshot.png) -->

## Features

- **Tonight only.** Search for a city and see which events are happening tonight. The events come from Ticketmaster, while information about bars, pubs and clubs comes from OpenStreetMap. Even after midnight, the application considers it the same night until 06:00.
- **Near me.** Use your location to find nearby places. The results are sorted by distance, using PostGIS to calculate which places are closest. This also works when places are located outside the city border.
- **Open now.**  The application uses opening hours from OpenStreetMap. These opening hours are converted into weekly time slots, which makes it possible to check whether a place is currently open. This also works for bars that close after midnight.
- **Plan my night.** Choose an event and find bars within walking distance that are still open when the event finishes. The application also provides a walking route.
- **Map and filters.** View events and places on a map using Leaflet. You can filter the results by place type, event genre or name. Photos are provided by Wikimedia Commons.

## Tech stack

| Area | Tools |
|---|---|
| Backend | Java 25, Spring Boot 4 (Web MVC, Data JPA, Validation, Cache, Scheduling) |
| Database | PostgreSQL 17 + PostGIS, Flyway migrations, Hibernate in `validate` mode |
| Data sources | Ticketmaster Discovery API, OpenStreetMap Overpass API, Nominatim geocoding |
| Opening hours | [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser) plus our own conversion to weekly slots |
| Testing | JUnit 5, AssertJ, Testcontainers (real PostGIS in Docker) |
| Frontend | Plain HTML/JavaScript, Leaflet, opening_hours.js |
| Dev setup | Docker Compose, started automatically by Spring Boot |

## How it works

The application gets information from three external sources. Ticketmaster provides event information, Overpass provides information about bars and other nightlife places, and Nominatim is used to find cities. The collected information is processed and saved in the database

**Data ingestion**
- First, the application uses Nominatim to find the location of a city. It then gets events from Ticketmaster and nightlife places from Overpass.
- Overpass servers can sometimes be slow or overloaded. To avoid waiting too long, the application sends the same request to multiple Overpass servers, with a small delay between them. It uses the first response that arrives.
- To prevent duplicate data, venues and events are checked using their source IDs (osm_id and external_id). These IDs are also protected by unique constraints in the database. Ticketmaster venues are also compared with existing venues using their names.
- Loading bars and other venues happens in the background. This means users can already see the events for tonight while the application is still collecting the venue information.
- A scheduled task runs every night to remove events that have already passed.

**Geospatial search**
- The database contains a venue.location column with the type geography(Point, 4326). PostgreSQL automatically creates this value from the latitude and longitude using a generated column. A GiST index is used to make location searches faster.
- When searching for nearby places, the application uses ST_DWithin to find venues within a certain distance. It then uses the <-> operator to sort them by distance. This allows the database to do the calculations instead of Java.

**Opening hours**
- The opening hours of each venue are first read from the opening_hours field. They are then converted into separate database records in venue_opening_period(day_of_week, opens_minute, closes_minute).
- Special attention is given to places that close after midnight. For example, a bar that is open on Friday from 22:00 until 03:00 is stored as (5, 1320, 1620). The opening period stays connected to Friday, even though the bar closes on Saturday.
- When checking if a venue is open, the application checks both today's opening hours and the previous day's hours if they continue past midnight.
- Some opening hours cannot be converted correctly, for example, opening times based on sunset or descriptions written as free text. Instead of making assumptions, the application stores these hours as unknown.
- Currently, the application can understand 649 out of 652 venues that have opening hours. This means that about 99.5% of the opening hours are successfully processed.

## API
- The application provides several REST API endpoints that can be used to retrieve information.

| Endpoint | Description |
|---|---|
| `GET /api/nightout?city=Amsterdam` | Returns information about a city, its events and venues for tonight. If the city has not been imported before, it is imported automatically. |
| `GET /api/venues/nearby?lat=52.3676&lng=4.9041&radius=1000` |Finds venues within the given radius in metres, with the closest places shown first. The maximum radius is 10 km. |
| `GET /api/venues/nearby?...&open=true` |Only returns venues that are currently open.|
| `GET /api/venues?city=Berlin&type=CLUB` | Returns venues in a city, with an optional filter for venue type.|
| `GET /api/venues/{id}` | Returns information about one specific venue. |
| `GET /api/events/tonight?city=Berlin` | Returns all events happening tonight in a city. |
| `GET /api/events?venueId=1` | Returns events taking place at a specific venue. |
| `GET /api/cities` | Returns all cities that have been imported. |

Example response from `/api/venues/nearby` (one item, some fields left out):

```json
[
  {
    "id": 412,
    "name": "Example Pub",
    "type": "Pub",
    "street": "Example Street",
    "cityName": "Amsterdam",
    "latitude": 52.3700,
    "longitude": 4.9010,
    "openingHours": "Mo-Th 16:00-01:00; Fr-Sa 16:00-03:00",
    "openNow": true,
    "openUntil": "01:00",
    "distanceMeters": 270
  }
]
```

## Running it locally

**You need:** JDK 25 and Docker Desktop (running). Maven is included through the wrapper.

```bash
git clone https://github.com/Maiaiacob/nightout.git
cd nightout

# Optional: events need a free Ticketmaster API key (https://developer.ticketmaster.com)
export TICKETMASTER_API_KEY=your-key        # Windows PowerShell: $env:TICKETMASTER_API_KEY="your-key"

./mvnw spring-boot:run                      # Windows: .\mvnw spring-boot:run
```

Then open http://localhost:8080.

- Spring Boot automatically starts the PostGIS database using compose.yaml. Flyway then creates the required database tables.
- During the first start, a few cities are loaded in the background. The Overpass servers can sometimes be slow, so it may take a few minutes before all bars and other venues appear.
- The database is available on port 5433. You can connect to it using a database client with the username and password nightout.

## Tests

```bash
./mvnw test
```

The repository and integration tests start a real PostGIS database with Testcontainers, so Docker has to be running. They cover:

- **Radius search:** only venues inside the radius are returned, nearest first, and the result limit is respected.
- **Open now:** a bar open Friday 20:00–03:00 is open at 01:00 on Saturday and closed at 03:00, and venues with unknown hours are never reported as open.
- **Opening-hours conversion:** closing after midnight, `24/7`, `off`, later rules overriding earlier ones, day ranges across the weekend (`Fr-Mo`), public-holiday rules, and unsupported formats.
- **Distance and time helpers**, and that the Flyway schema matches the JPA entities.

## Project structure

```
src/main/java/com/nightout/
├── client/        API clients: Ticketmaster, Overpass (with mirror fallback), Nominatim
├── config/        Configuration properties, clock
├── controller/    REST endpoints
├── dto/ mapper/   API response objects and entity → DTO mapping
├── hours/         Opening hours: parsing, weekly slots, open-now lookups
├── model/         JPA entities: City, Venue, Event, OpeningPeriod
├── repository/    Spring Data repositories, including the PostGIS native queries
├── service/       Import pipeline, search, background loading, scheduled cleanup
└── util/          Night window ("tonight" until 06:00), distance helper
src/main/resources/
├── db/migration/  Flyway migrations (V1 schema, V2 opening hours)
└── static/        Web page (index.html)
```

## Roadmap
There are still some improvements that could be added to the application in the future:
- Add GitHub Actions to automatically run the tests after every push.
- Deploy the application online using a hosted PostgreSQL database with PostGIS.
- Use the correct time zone for each city when checking opening hours. Currently, the server's time zone is used.
- Improve the ranking of venues by considering distance, opening hours, preferred venue types and the time of night.
- Improve venue matching between different data sources by comparing names and distances using pg_trgm.

## Data and credits
The project uses data from several external sources:

- Venue and map data © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors, available under the ODbL.
- Event data from the [Ticketmaster Discovery API](https://developer.ticketmaster.com/).
- Photos from Wikimedia Commons.
