#!/usr/bin/env bash
set -eux

docker image build -t org.rm3l/dev-feed-api:0.10.5 .
