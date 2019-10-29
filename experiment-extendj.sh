#!/bin/env sh

export ALFINE_HOME=`pwd`
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk

# Note: The identifier length was chosen arbitrarily. In my limited experience most identifiers
#       are less than 49 characters (this could be less true about generated code). We could
#       collect the identifier length distribution for extendj and jacop to try to motivate a
#       particular length ...

./experiment/scripts/experiment-prepare.sh --project extendj-bms --variable extendj-8.1.2
./experiment/scripts/experiment-run.sh --limit 10 --type rename --length 49

# TODO: Because the test suite is unreliable in the sense it may hang when test
#       compilation fails we must use the `timeout` command from `coreutils`
#       to terminate the test suite after about 2(?) minutes.
#
# timeout 2 java -jar alfine.sh --command build --project extendj-bms --test-on-import
#
