#!/bin/bash

source ~/.shcolor.sh 2>/dev/null || source <(curl -s https://raw.githubusercontent.com/kba/shcolor/master/shcolor.sh|tee ~/.shcolor.sh)

TREETAGGER_DIR=$PWD/tree-tagger
CORE_TGZ="tree-tagger-linux-3.2.tar.gz"
SCRIPTS_TGZ="tagger-scripts.tar.gz"

echo "`C 2`Downloading Tree Tagger if necessary"

if [[ -e "$TREETAGGER_DIR" ]];then
    echo "`C 1`Directory '`C 3`$TREETAGGER_DIR`C 1`' already exists"
else
    echo "`C 4`Creating directory '`C 3`'$TREETAGGER_DIR'`C 1`'"
    mkdir -p "$TREETAGGER_DIR"
    cd "$TREETAGGER_DIR"
    echo "`C 4`Downloading and extracting `C 3`'$CORE_TGZ'`C 9`"
    curl "http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/$CORE_TGZ"|tar vxz
    echo "`C 4`Downloading and extracting `C 3`'$SCRIPTS_TGZ'`C 9`"
    curl "http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/$SCRIPTS_TGZ"|tar vxz
fi
