#!/usr/bin/env bash
set -eux

docker container rm -f awesome_dev_backend >/dev/null 2>&1 || true
docker container run -d --rm --name awesome_dev_backend -p 8080:8080 -t org.rm3l/awesome_dev_backend:0.1.0-SNAPSHOT
docker container logs -f awesome_dev_backend