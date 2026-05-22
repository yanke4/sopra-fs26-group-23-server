<p align="center">
  <img src="./docs/logo.png" alt="Game logo" width="100%" />
</p>

# SoPra FS26 – Group 23 Server

<!-- Status badges -->

![Build](https://img.shields.io/github/actions/workflow/status/yanke4/sopra-fs26-group-23-server/dockerize.yml?branch=main&label=build)
![Deploy](https://img.shields.io/github/actions/workflow/status/yanke4/sopra-fs26-group-23-server/main.yml?branch=main&label=deploy)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4-6DB33F?logo=springboot)
![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk)
![Gradle](https://img.shields.io/badge/Gradle-9-02303A?logo=gradle)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-ready-4169E1?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker)

## Introduction

This project is the **backend** of a web-based, multiplayer strategy game inspired by classic territorial conquest games such as _Risk_. It manages user accounts, lobbies, the authoritative game state of every match, and pushes real-time updates to the connected clients while they reinforce, attack and try to conquer the whole map of Europe.

The motivation behind the project is to deliver a smooth, real-time multiplayer experience in the browser. The server's job is to be the source of truth: it validates every action, enforces the game rules, resolves dice rolls and broadcasts the resulting state so that all players see the same board at the same time.

---

## Technologies Used

- **Spring Boot 4** + **Java 17** + **Gradle** (wrapper)
- **Spring Data JPA** with **H2** in dev and **PostgreSQL** in production
- **Spring WebSocket** (STOMP) and **Pusher** for real-time game updates
- **MapStruct** for DTO mapping
- **JUnit 5** + **Spring Boot Test** for unit and integration tests, **JaCoCo** for coverage
- **Docker** for packaging, **Google Cloud Run** for deployment, **SonarCloud** for static analysis
- **Nix (flake.nix)** for a reproducible dev environment

---

## High-Level Components

The server is organised in the classic Spring layered style. Each layer has a clear responsibility and they communicate through well-defined interfaces.

1. **REST Controllers** – [`controller/`](./src/main/java/ch/uzh/ifi/hase/soprafs26/controller)
   The HTTP entry points (`UserController`, `LobbyController`, `GameController`, `TurnController`, `PlayerController`, `ChatController`). They validate the request, delegate to the service layer and return DTOs — they hold no game logic of their own.

2. **Game & Turn Services** – [`service/GameService.java`](./src/main/java/ch/uzh/ifi/hase/soprafs26/service/GameService.java) and [`service/TurnService.java`](./src/main/java/ch/uzh/ifi/hase/soprafs26/service/TurnService.java)
   The heart of the application. They own the authoritative game state: starting a match, advancing the turn phases (reinforce → attack → fortify), resolving dice rolls, and detecting victory. Every client action goes through them.

3. **Lobby & User Services** – [`service/LobbyService.java`](./src/main/java/ch/uzh/ifi/hase/soprafs26/service/LobbyService.java) and [`service/UserService.java`](./src/main/java/ch/uzh/ifi/hase/soprafs26/service/UserService.java)
   Handle the pre-game flow: account creation and authentication, lobby creation/join by code, ready state, and the hand-off from lobby to game.

4. **Map Domain** – [`service/MapService.java`](./src/main/java/ch/uzh/ifi/hase/soprafs26/service/MapService.java), [`RegionService`](./src/main/java/ch/uzh/ifi/hase/soprafs26/service/RegionService.java), [`FieldService`](./src/main/java/ch/uzh/ifi/hase/soprafs26/service/FieldService.java)
   Encapsulate everything about the board: which fields exist, which regions they belong to, and which fields are adjacent. This is what the game logic queries to decide whether an attack or a fortification is legal.

5. **Persistence Layer** – [`entity/`](./src/main/java/ch/uzh/ifi/hase/soprafs26/entity) and [`repository/`](./src/main/java/ch/uzh/ifi/hase/soprafs26/repository)
   JPA entities (`User`, `Lobby`, `Game`, `Player`, `Field`, `Region`, `Map`, `UserStats`) and their Spring Data repositories. H2 is used in the `dev` profile, PostgreSQL in production.

6. **Real-time Layer** – `WebSocket` (STOMP) configuration and `Pusher` integration
   Used by the services to broadcast game-state changes (turn advanced, attack resolved, territory captured, chat message) to every client subscribed to the match.

**How they are correlated:** the _Controllers_ receive an HTTP call from the client, validate it and delegate to the _Services_. The _Game/Turn Services_ mutate the authoritative game state through the _Persistence Layer_, consult the _Map Domain_ for legality checks, and finally use the _Real-time Layer_ to broadcast the change to every connected player.

---

## Launch & Deployment

### Prerequisites

- MacOS, Linux or Windows + WSL2
- `git`, `curl`
- **Java 17** (or use the bundled Nix flake to get it automatically)
- Optional: Docker (only needed to run the production image locally)

### First-time setup

Clone the repo. The bundled [`flake.nix`](./flake.nix) provides Java 17 and Gradle in a reproducible shell:

```bash
git clone https://github.com/yanke4/sopra-fs26-group-23-server
cd sopra-fs26-group-23-server
nix develop   # optional, drops you into a shell with the right JDK
```

If you don't use Nix, make sure your `JAVA_HOME` points to a JDK 17.

### Running locally

The Gradle wrapper handles everything — no separate Gradle install is needed.

| Goal                                                        | Command                      |
| ----------------------------------------------------------- | ---------------------------- |
| Start the dev server (`http://localhost:8080`, H2 database) | `./gradlew bootRun`          |
| Build a production JAR                                      | `./gradlew build`            |
| Run all tests                                               | `./gradlew test`             |
| Generate the JaCoCo coverage report                         | `./gradlew jacocoTestReport` |
| Run the SonarCloud analysis (requires `SONAR_TOKEN`)        | `./gradlew sonar`            |

The dev profile uses an in-memory H2 database, so the server starts with an empty schema every time. You can browse it at `http://localhost:8080/h2-console`.

### Tests

Tests live under [`src/test/java`](./src/test/java) and cover the service, controller, repository and DTO mapping layers. They run with JUnit 5 through `./gradlew test`.

### External dependencies

- **PostgreSQL** – used in production through `spring.profiles.active=production`. The connection is configured by the environment variables `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`.
- **Pusher** – used for real-time updates. Configured through `pusher.appId`, `pusher.key`, `pusher.secret`, `pusher.cluster` in `application.properties`.

### Releases (Docker & Cloud Run)

Every push to `main` triggers two pipelines:

- [`dockerize.yml`](./.github/workflows/dockerize.yml) builds the image and pushes it to Docker Hub.
- [`main.yml`](./.github/workflows/main.yml) runs the test suite + SonarCloud analysis, then builds the image and deploys it to **Google Cloud Run** (region `europe-west1`).

To run a release locally:

```bash
docker pull <dockerhub_username>/<dockerhub_repo_name>
docker run -p 8080:8080 <dockerhub_username>/<dockerhub_repo_name>
```

The required GitHub Secrets are `dockerhub_username`, `dockerhub_password` (a PAT) and `dockerhub_repo_name` for the Docker pipeline, plus `GCP_SERVICE_CREDENTIALS`, `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` and `SONAR_TOKEN` for the Cloud Run deploy.

---

## Roadmap

The features below are good entry points for new contributors:

1. **Additional maps** – the game currently ships with the Europe map only. Adding a new map (e.g. World, Americas, Asia) means defining a new dataset of fields and adjacencies and exposing it through [`MapService`](./src/main/java/ch/uzh/ifi/hase/soprafs26/service/MapService.java) so the client can offer it as a lobby option.
2. **Ranking & matchmaking system** – persistent player ranks, an ELO-style ladder backed by [`UserStats`](./src/main/java/ch/uzh/ifi/hase/soprafs26/entity/UserStats.java), and a leaderboard that goes beyond the per-game one.
3. **Single-player mode with AI bots** – allow a player to start a game against AI opponents with configurable difficulty. The bot would drive the game through the same [`TurnService`](./src/main/java/ch/uzh/ifi/hase/soprafs26/service/TurnService.java) API as a human, unlocking tutorials, offline play and a great way to onboard new users.

---

## Authors and Acknowledgment

Built by **Group 23** of the _Software Engineering Lab (SoPra FS26)_ course at the **University of Zurich**.

| Name          | GitHub                                           |
| ------------- | ------------------------------------------------ |
| Daniele Zaka  | [@devnida](https://github.com/devnida)           |
| Linus Frick   | [@Linu5Frick](https://github.com/Linu5Frick)     |
| Matteo Lozano | [@MatteoLozano](https://github.com/MatteoLozano) |
| Kevin Yan     | [@yanke4](https://github.com/yanke4)             |

Thanks to the HASEL group and the SoPra teaching team for the project template, infrastructure and feedback throughout the semester.

---

## License

This project is released under the **MIT License**.

```
MIT License

Copyright (c) 2026 Daniele Zaka, Linus Frick, Matteo Lozano, Kevin Yan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
