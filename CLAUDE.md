# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Dev Feed is a Flutter-based mobile application for keeping up with top engineering content from companies worldwide. The project is a monorepo containing:

- **Backend**: Spring Boot application (Kotlin) with scheduled crawlers that analyze remote websites for articles, storing them in a local database and exposing data via GraphQL API
- **Mobile**: Cross-platform Flutter application (Dart) that consumes the GraphQL API

The backend crawlers fetch articles from:
- DiscoverDev.io
- EngineeringBlogs.xyz
- rm3l.org

## Architecture

### Backend (Spring Boot + Kotlin)

The backend is a multi-module Gradle project located in `backend/`:

- **api** (`dev-feed-api`): GraphQL API server, main entry point
- **common** (`dev-feed-common`): Shared utilities and code
- **persistence** (`dev-feed-persistence`): Database layer and models
- **article-parser** (`dev-feed-article-parser`): Article parsing logic
- **screenshot** (`dev-feed-screenshot`): Screenshot generation functionality
- **crawlers**: Plugin-based crawler system
  - `common`: Shared crawler infrastructure
  - `cli`: Command-line interface for crawlers
  - `discoverdev_io`: DiscoverDev.io crawler
  - `engineeringblogs_xyz`: EngineeringBlogs.xyz crawler
  - `rm3l_org`: rm3l.org blog crawler

Each crawler is independently deployable as a Docker container. The API and all crawlers are published to Docker Hub under `rm3l/dev-feed-*`.

### Mobile (Flutter)

The mobile app is in `mobile/` with this structure:

- **lib/api**: GraphQL API client (articles, tags)
- **lib/config**: App configuration (routes, application setup)
- **lib/environments**: Environment-specific configurations
  - `render.dart`: Default production backend (https://dev-feed-api.onrender.com)
  - `development.dart`: Local development backend
  - `prod.dart`: Production configuration
- **lib/ui**: UI screens (latest news, archives, favorites, search, tags, about)
- **lib/main.dart**: App entry point

The app uses an environment system where different backends can be targeted at build time by specifying `-t lib/environments/<env>.dart`.

## Build Commands

### Backend

All backend commands should be run from the repository root:

```bash
# Build entire backend
./backend/gradlew -p ./backend build --stacktrace

# Build specific module (e.g., API)
./backend/gradlew -p ./backend :dev-feed-api:build

# Run tests
./backend/gradlew -p ./backend test

# Run the API server locally
java -jar backend/api/build/libs/dev-feed-api-*.jar

# Build Docker image for API (using Jib)
./backend/gradlew -p ./backend :dev-feed-api:jib

# Build Docker image for specific crawler
./backend/gradlew -p ./backend :crawlers:dev-feed-crawler-discoverdev_io:jib
```

The GraphiQL browser is accessible at http://localhost:8080/graphiql when running locally.

### Mobile

All mobile commands should be run from the `mobile/` directory:

```bash
# Get dependencies
flutter pub get

# Run tests
flutter test

# Build debug APK (targets default Render backend)
flutter build apk --debug

# Build debug APK with custom backend
flutter build apk --debug -t lib/environments/development.dart

# Build release APK
flutter build apk --release

# Build release App Bundle for Google Play
flutter build appbundle --release

# Install on connected device/emulator
flutter install

# Clean build artifacts
flutter clean
```

## Important Configuration Details

### Mobile Package Name

The Android package name is `lemrapp.dev_feed` (configured in `mobile/android/app/build.gradle`).

### Version Management

Versions are automatically derived from git tags using the format:
- Version code: Calculated from `MAJOR.MINOR.PATCH` + build number
- Version name: `<tag>-<commits-since-tag>/<git-hash>-<platform>`

Example: `2.1.1-230/ee0fa7d-android`

To tag a new version:
```bash
git tag <version>
git push origin <version>
```

### Mobile Environments

The mobile app supports multiple backend environments. To add a new environment:

1. Create a file in `mobile/lib/environments/<name>.dart`
2. Extend the `Env` class and set `baseUrl`
3. Build with `-t lib/environments/<name>.dart`

### Android Signing

Release builds require a keystore at `~/.droid/awesome_dev.keystore.properties` with:
```properties
keystore=<path-to-keystore>
keyPassword=<password>
```

In CI/DEBUG mode, it falls back to the debug keystore.

## Docker Deployment

Backend components are published to Docker Hub:

```bash
# Pull and run the API
docker pull rm3l/dev-feed-api
docker run --rm -p 8080:8080 rm3l/dev-feed-api

# Pull specific crawler
docker pull rm3l/dev-feed-crawler-discoverdev_io
```

## Flutter Version

This project uses Flutter 2.2.2 (stable channel). The codebase is written without sound null safety.

## GraphQL API

The backend exposes a GraphQL API with queries for:
- Searching articles
- Filtering by tags
- Reading past articles
- Browsing archives

Access the GraphiQL interface at `/graphiql` to explore the schema.
