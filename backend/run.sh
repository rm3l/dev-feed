#!/usr/bin/env bash
set -eux

docker container rm -f dev-feed-backend >/dev/null 2>&1 || true
docker container run -d --rm --name dev-feed-backend -p 8080:8080 -p 8081:8081 -t org.rm3l/dev-feed-backend:0.8.0
docker container logs -f dev-feed-backend
