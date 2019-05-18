#!/usr/bin/env bash
set -eux

docker container rm -f dev-feed-backend >/dev/null 2>&1 || true
docker container run -d --rm --name dev-feed-backend -p 8080:8080 -t org.rm3l/dev-feed-backend:0.1.0-SNAPSHOT
docker container logs -f dev-feed-backend
