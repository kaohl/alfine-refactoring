#!/bin/env bash

# Description
#   This script prepares the `experiment` folder for experiments by
#   exporting specified project source code into `experiment/assets`.

# Assume we are running in `ALFINE_HOME` directory.

if [[ ! -d experiment ]]; then
    echo error: no such directory \`experiment\`
    echo create a directory called \`experiment\`\; \
         add the refactoring framework eclipse \
         product to it and try again.
    exit 1
else
    # Clean previous setup (if any).
    rm -rf experiment/assets
fi

mkdir -p experiment/assets/lib

exportSrcDir=experiment/assets
exportLibDir=experiment/assets/lib

java -jar alfine.jar --command export --src $exportSrcDir --lib $exportLibDir $@
