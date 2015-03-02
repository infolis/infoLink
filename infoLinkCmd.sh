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
# first argument: classpath for InfoLink java classes (change if desired)

PYTHON_SRC="src/main/python"

# extract and clean text from pdf documents, remove bibliographies and learn and apply patterns, use uppercase_constraint
# use ALLBUS, Allbus, Eurobarometer and ISSP as seeds
python $PYTHON_SRC/infoLink.py \
    -C "build/classes/main/" \
    -e "../data/test/small_txt" \
    -c "../data/test/small" \
    -l "../data/test/train_small/" \
    -o "../data/test/output_small" \
    -s "\"ALLBUS Allbus Eurobarometer ISSP\"" \
    -i "../data/test/Index_small" \
    -m "../data/test/urnDict.csv" \
    -u \
    .

# learn patterns and apply patterns without text extraction. Use uppercase_constraint
# python.exe ../py/infoLink.py "." -c "../data/test/small_txt" -l "../data/test/train_small/" -s "\"ALLBUS Allbus Eurobarometer ISSP\"" -i "../data/test/Index_small" -o "../data/test/output_small" -m "../data/test/urnDict.csv" -u

# apply existing patterns and term search for known dataset names with prior text extraction. Use uppercase_constraint
# python.exe ../py/infoLink.py "." -p "../data/test/patterns.csv" -o "../data/test/output_small" -e "../data/test/small_txt" -c "../data/test/small" -i "../data/test/Index_small" -t "../data/test/known_datasets.csv" -u
