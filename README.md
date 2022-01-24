# Dev Feed

[![Backend Build Workflow](https://github.com/rm3l/dev-feed/workflows/Backend%20Build%20and%20Publish%20Docker%20Image/badge.svg)](https://github.com/rm3l/dev-feed/actions?query=workflow%3A%22Backend+Build+and+Publish+Docker+Image%22)
[![Mobile Build Workflow](https://github.com/rm3l/dev-feed/workflows/Mobile%20Build/badge.svg)](https://github.com/rm3l/dev-feed/actions?query=workflow%3A%22Mobile+Build%22)

[![Heroku](https://img.shields.io/badge/heroku-deployed%20on%20free%20dyno-blue.svg)](https://dev-feed-api.herokuapp.com/graphiql)

[![Docker Stars](https://img.shields.io/docker/stars/rm3l/dev-feed-api.svg)](https://hub.docker.com/r/rm3l/dev-feed-api)
[![Docker Pulls](https://img.shields.io/docker/pulls/rm3l/dev-feed-api.svg)](https://hub.docker.com/r/rm3l/dev-feed-api)

[![License](https://img.shields.io/badge/license-MIT-green.svg?style=flat)](https://github.com/rm3l/dev-feed/blob/master/LICENSE)

Dev Feed is a Flutter-based mobile application allowing to keep up with top engineering content from
companies all over the world.
It stemmed from my own needs to not only follow a curated list of tech-related blogs, but also
play a little bit with the excellent [Flutter](https://flutter.dev/) SDK.

<a href="https://play.google.com/store/apps/details?id=org.rm3l.awesome_dev"><img src="https://github.com/rm3l/dev-feed/raw/master/mobile/deployment/screenshots/android/google-play-badge.png" width="50%"/></a>

![demo](https://raw.githubusercontent.com/rm3l/dev-feed/master/mobile/deployment/screenshots/android/latest_news_w400.png)

## Tech Stack

The tech stack is rather simple:
* Backend
  * A [Spring Boot](https://spring.io/projects/spring-boot) application written in [Kotlin](https://kotlinlang.org/), which contains scheduled crawlers in charge of analyzing certain remote websites for articles and feeding a local database. It then aggregates and exposes such data over a [GraphQL](https://graphql.org/) API, with the ability to search for articles, or by tags, or to read past articles. This Backend application is published to the [Docker Hub](https://hub.docker.com/r/rm3l/dev-feed), and continuously deployed to [Heroku](https://dev-feed-api.herokuapp.com/graphiql) as well. At the moment, articles are fetched from the list below, but additional sources may be added later on:
    * [DiscoverDev.io](https://www.discoverdev.io/)
    * [EngineeringBlogs.xyz](https://engineeringblogs.xyz/)
    * my own blog, located at [rm3l.org](https://rm3l.org)
* Mobile
  * A cross-platform mobile UI application written in [Dart](https://dart.dev/), using the [Flutter](https://flutter.dev/) SDK. Please note that there is no sync'ing mechanism, and all search/favorite articles are stored on the local device. This is an enhancement that might be implemented later on.

### Building and running

#### Using the Backend GraphQL API

##### Docker

A Docker repository with the GraphQL API Server can be found here: https://hub.docker.com/r/rm3l/dev-feed-api

To fetch the docker image, run:

```bash
docker image pull rm3l/dev-feed-api
```

To run the server with the default options and expose it on port 8080:

```bash
docker container run --rm -p 8080:8080 rm3l/dev-feed-api
```

You can then access the GraphiQL browser by heading to http://localhost:8080/graphiql

##### Kubernetes

The Backend API is also published to [my Helm Charts repository](https://helm-charts.rm3l.org/), so as to be deployable to a Kubernetes Cluster using [Helm](https://helm.sh/).

It is listed on Artifact Hub : https://artifacthub.io/packages/helm/rm3l/dev-feed

```bash
$ helm repo add rm3l https://helm-charts.rm3l.org
$ helm install my-dev-feed rm3l/dev-feed
```

See https://artifacthub.io/packages/helm/rm3l/dev-feed or https://github.com/rm3l/helm-charts/blob/main/charts/dev-feed/README.md for
all customizable values.

You can then access the GraphiQL browser by heading to http://localhost:8080/graphiql

##### Manual mode

1. Build the Backend

```sh
./backend/gradlew -p ./backend build --stacktrace
```

2. Run the Backend GraphQL API

```sh
java -jar backend/api/build/libs/dev-feed-api-1.12.1.jar
```

You can then access the GraphiQL browser by heading to http://localhost:8080/graphiql

#### Using the Mobile application

1. Install Flutter by following the instructions on the [official website](https://flutter.dev/docs/get-started/install)

2. Prepare the configuration environment

Skip this to use the default Heroku Backend. Otherwise, if you have a custom Backend (either local or remote), you need to create a specific environment file (say `my_personal_backend.dart`) in the `mobile/lib/environments` folder, e.g.:

```dart
import 'package:dev_feed/env.dart';

void main() => MyPersonalBackend();

class MyPersonalBackend extends Env {
  final String baseUrl = 'https://my-dev-feed-backend-api.example.org';
}
```

3. Build the mobile apps

First `cd` to the `mobile` directory:

```sh
cd mobile
```

If you simply want to target the default Heroku backend, just run:

```sh
flutter build apk --debug
```

Otherwise, if you have a custom Backend (and its related Dart environment file) declared under `mobile/lib/environments/my_personal_backend.dart`, then run:

```sh
flutter build apk --debug -t lib/environments/my_personal_backend.dart
```

You will then find the mobile applications built under the respective platform folders. For example, the APK for Android can be found under `build/app/outputs/apk/debug/`.

4. Install and run the APK either in an emulator or in a real device

```sh
flutter install
```

Or:

```sh
adb install -r build/app/outputs/apk/debug/app-debug.apk
```

## Contribution Guidelines

Contributions and issue reporting are more than welcome. So to help out (e.g., with a new Article crawler plugin in the Backend), do feel free to fork this repo and open up a pull request.
I'll review and merge your changes as quickly as possible.

You can use [GitHub issues](https://github.com/rm3l/awesome-dev/issues) to report bugs.
However, please make sure your description is clear enough and has sufficient instructions to be able to reproduce the issue.

## Credits / Inspiration

* [EngineeringBlogs.xyz](https://engineeringblogs.xyz/)
* [DiscoverDev.io](https://www.discoverdev.io/)

## Developed by

* Armel Soro
  * [keybase.io/rm3l](https://keybase.io/rm3l)
  * [rm3l.org](https://rm3l.org) - &lt;apps+dev_feed@rm3l.org&gt; - [@rm3l](https://twitter.com/rm3l)
  * [paypal.me/rm3l](https://paypal.me/rm3l)
  * [coinbase.com/rm3l](https://www.coinbase.com/rm3l)

## License

    The MIT License (MIT)

    Copyright (c) 2019-2022 Armel Soro

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
