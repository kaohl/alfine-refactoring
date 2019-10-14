#!/bin/env bash

# Assume we are running in `ALFINE_HOME` directory.

# Example of intended use:
# $> ./experiment-prepare.sh --project jacop-bms --variable jacop-4.6.0
# $> ./experiment-run.sh --limit 10 --type inline-method --length 24

# Run until we got 100 versions or until one does not produce any output.

for i in {0..99}
do
    # Refactor assets.

    rm    -rf experiment/workspace
    mkdir -p  experiment/workspace/assets
    cp    -r  experiment/assets/* experiment/workspace/assets

    # Note: User must set options `limit`, `type`, and type-related options.

    cmd="../eclipse/eclipse -data . --src assets --lib assets/lib --out output --seed $((RANDOM)) $@"

    cd experiment/workspace

    eval $cmd > output.log

    cd -

    if [ "(ls -A experiment/workspace/output)" ]; then
        echo "No output generated after refactoring."
        echo "Failed to refactor project with i=$i."
        break
    fi

    # Save output.

    mkdir -p experiment/output/"d$i"

    echo "$cmd" > experiment/output/d$i/cmd.txt

    cp experiment/workspace/output/*   experiment/output/"d$i"
    cp experiment/workspace/output.log experiment/output/"d$i"

done

