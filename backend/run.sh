#!/usr/bin/env bash
set -eux

docker container rm -f dev-feed-api >/dev/null 2>&1 || true
docker container run -d --rm --name dev-feed-api -p 8080:8080 -p 8081:8081 -t org.rm3l/dev-feed-api:0.10.5
docker container logs -f dev-feed-api
