#!/bin/sh
set -eu

set -a
. /home/ubuntu/rhythm-metadata-api/.env
set +a

exec /home/ubuntu/musicxml-ingest/venv/bin/python \
  /usr/local/libexec/sonorus-sync-update-cos.py "$@"
