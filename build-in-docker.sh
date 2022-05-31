#!/bin/bash
docker build -t git-mkver .
docker run \
    --rm \
    -v $(pwd):/workspace \
    -w /workspace \
    -it git-mkver
