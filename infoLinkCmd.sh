#!/bin/bash

# -e = extract text from pdf documents and save to specified location
# -c = location of input corpus (if in pdf format, use -e option)
# -l = learn extraction patterns from corpus and save training data to this directory
# -o = output path
# -s = seed dataset names (at the moment, do not specify seeds consisting of multiple words)
# -i = path of the lucene index (will be created if neccessary)
# -m = path of the map file listing document filenames and corresponding IDs
# -u = use uppercase_constraint (dataset titles are required to have at least one uppercase character)
# -p = use the patterns in this file for reference extraction
# -t = apply term search for dataset names listed in this file
# -f = use frequency-based measure for pattern validity assessment
# -r = use reliability-based measure for pattern validity assessment with specified threshold

PYTHON_SRC="src/main/python"
DIR_NAME=${PWD##*/} 
INSTALL_DIR="build/install/$DIR_NAME"

# extract and clean text from pdf documents, remove bibliographies and learn and apply patterns, use uppercase_constraint
# use ALLBUS, Allbus, Eurobarometer, ISSP and NHANES as seeds
# apply reliability-based pattern validity assessment with threshold of 0.7
python $PYTHON_SRC/infoLink.py \
    -C "build/classes/main/:$INSTALL_DIR/lib/*" \
    -e "../data/test/small_txt" \
    -c "../data/test/small" \
    -l "../data/test/train_small" \
    -o "../data/test/output_small" \
    -s "ALLBUS--@--Allbus--@--Eurobarometer--@--ISSP--@--NHANES" \
    -i "../data/test/Index_small" \
    -m "../data/test/urnDict.csv" \
    -r "0.7" \
    -u \

