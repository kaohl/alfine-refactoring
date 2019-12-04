#!/usr/bin/env bash

declare -a projects=("extendj-bms" "jacop-bms")
declare -a types=("rename" "inline-method")

# Alfine export-command argument. (Specify all variable projects.)
variableProjects="extendj-8.1.2 jacop-4.6.0"

# Assume `basedir` exists and contains our refactoring framework.

basedir=experiment
framework=$basedir/eclipse/eclipse
sharedAssetsDir=$basedir/assets
dataRootDir=$basedir/data

rm    -rf $dataRootDir
mkdir -p  $dataRootDir

rm    -rf $sharedAssetsDir
mkdir -p  $sharedAssetsDir

for project in "${projects[@]}"; do

    # Export benchmark-project assets.

    projectSrcDir=$sharedAssetsDir/$project/src
    projectLibDir=$sharedAssetsDir/$project/lib

    mkdir -p $projectSrcDir
    mkdir -p $projectLibDir

    java -jar alfine.jar\
         --command  export\
         --project  $project
         --src      $projectSrcDir\
         --lib      $projectLibDir\
         --variable $variableProjects
done

function refactor {
    procID=$1
    project=$2
    type=$3
    n=$4

    args="--limit 10"

    case $type in
        "rename")
            args="$args --type rename --length 49"
        ;;
        "inline-method")
            args="$args --type inline-method"
        ;;
        *)
            args=""
        ;;
    esac

    #
    # TODO: call refactoring framework with refactoring arguments.
    # See previous experiment script.
    #
}

# Start one refactoring process per processing unit.

total=100            # Number of refactorings per project per type.
n=`nproc`            # Number of available processing units.
m=$(( $total / $n )) # Number of refactorings per process.
n_times_m=$(( $n * $m )) # Actual number of refactorings per project per type.

if [[ ! $n_times_m == $total ]]; then
    echo "Total number of refactorings per type ($total) is not evenly"\
         "divisible by the number of available processing units ($n)."\
         "$n_times_m refactorings will be created."
fi

for project in "${projects[@]}"
do
    # This loop divides the work evenly on all available processing units.
    # Because all refactorings of the same project and type takes approximately
    # the same time, all batches in the inner-most loop terminates approximately
    # at the same time.
    #
    # The call to `wait` will make the main process wait for all processes created
    # in the inner loop before running refactorings of the next (project, type)-pair.

    for type in "${types[@]}"; do

        # Create data output directory for project and type.
        mkdir -p $dataRootDir/$project/$type

        for i in $(seq $n); do
            (
                echo "starting project = $project, type = $type, batch = $i"

                procID=$i

                procDir=$baseDir/process-$procID
                workspaceDir=$procDir/workspace
                procAssetsDir=$workspaceDir/assets
                procOutputDir=$workspaceDir/output

                rm    -rf $procDir
                mkdir -p  $procAssetsDir

                # Copy assets from shared resources dir to fresh workspace folder.

                projectAssetsDir=$sharedAssetsDir/$project
                projectSrcDir=$projectAssetsDir/src
                projectLibDir=$projectAssetsDir/lib

                cp -r\
                   $projectSrcDir\
                   $projectLibDir\
                   $procAssetsDir

                for j in $(seq $m); do
                    refactor $procID $project $type $(( "$m * ( $i - 1 ) + $j" ))
                done
            ) &
        done
        time wait # Wait for (inner loop) all known process IDs.
    done
done
