#!/bin/env sh

./experiment/scripts/experiment-prepare.sh --project extendj-bms --variable extendj-8.1.2
./experiment/scripts/experiment-run.sh --limit 10 --type rename

# TODO: Since the test suite is unreliable in the sense that the test suite may
#       hang when test compilation fails we must use the `timeout` command from
#       `coreutils` to terminate the test suite after about 2(?) minutes. 
#
# timeout 2 java -jar alfine.sh --command build --project extendj-bms --test-on-import
#
