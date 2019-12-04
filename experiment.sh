#!/usr/bin/env bash

#export ALFINE_HOME=`pwd`
#export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 # Edit depending on OS.

if [[ ! $JAVA_HOME ]]; then
    echo Please point JAVA_HOME to a java 8 distribution.
    exit 1
fi
if [[ ! $ALFINE_HOME ]]; then
    echo Please set ALFINE_HOME to the top-level directory of Alfine.
    exit 1
fi

declare -a projects=("extendj-bms" "jacop-bms")
declare -a types=("rename" "inline-method" "extract-method" "inline-constant" "extract-constant")

# All variable dependencies here.
variableArchives="extendj-8.1.2 jacop-4.6.0"

experimentDir=experiment
sharedAssetsDir=$experimentDir/assets

for project in "${projects[@]}"
do
    projectAssetsDir=$sharedAssetsDir/$project

    exportSrcDir=$projectAssetsDir/src
    exportLibDir=$projectAssetsDir/lib

    mkdir -p $exportSrcDir
    mkdir -p $exportLibDir

    java -jar alfine.jar \
         --command export \
         --src      $exportSrcDir \
         --lib      $exportLibDir \
         --project  $project \
         --variable $variablesArchives
done

experimentOutputDir=$experimentDir/output
processDir=$experimentDir/process

# Refactor projects.

function refactor_ten_times {

    procDir=$experimentDir/p$processid

    for i in {0..9}
    do
        workspaceDir=$procDir/workspace
        workspaceAssetsDir=$workspaceDir/assets

        rm    -rf $workspaceDir
        mkdir -p  $workspaceAssetsDir
        cp    -r  $sharedAssetsDir/$project $workspaceAssetsDir

        dataDir=$experimentOutputDir/$project/$type/d$1$i
        echo "dataDir = $dataDir"
    done
}

function mkprocess {

    processid=$1
    project=$2
    type=$3

    procDir=$experimentDir/p$processid

    mkdir -p $procDir

    for i in {0..9}
    do
        refactorArgs=""

        case $type in
            "rename")
                refactorArgs="--limit 10 --type rename --length 49"
                ;;
            "inline-method")
                refactorArgs=""
                ;;
            "inline-constant")
                refactorArgs=""
                ;;
            "extract-method")
                refactorArgs=""
                ;;
            "extract-constant")
                refactorArgs=""
                ;;
        esac

        refactor_ten_times $1 $2 $3 $i $refactorArgs &
    done
}

processID=0

for project in "${projects[@]}"
do
    for type in "${types[@]}"
    do
        # Create a subprocess for each (project, type)-pair.
        mkprocess $((processID++)) $project $type &
    done
done
