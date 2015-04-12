::!\bin\bash

:: -e = extract text from pdf documents and save to specified location
:: -c = location of input corpus (if in pdf format, use -e option)
:: -l = learn extraction patterns from corpus and save training data to this directory
:: -o = output path
:: -s = seed dataset names (use delimiter specified in Util.delimiter_internal to enumerate seeds)
:: -i = path of the lucene index (will be created if neccessary)
:: -m = path of the map file listing document filenames and corresponding IDs
:: -u = use uppercase_constraint (dataset titles are required to have at least one uppercase character)
:: -n = use NP constraint with the specified tree tagger arguments: tagging command and chunking command
:: -p = use the patterns in this file for reference extraction
:: -t = apply term search for dataset names listed in this file
:: -f = use frequency-based measure for pattern validity assessment with specified threshold
:: -r = use reliability-based measure for pattern validity assessment with specified threshold
:: -N = maximum number of iterations
:: -F = strategy for processing new contexts within frequeny-based framework: "mergeCurrent", "mergeNew", "mergeAll", "separate" (default)

::PYTHON_SRC="src\main\python"
::DIR_NAME=${PWD::::*\} 
::INSTALL_DIR="build\install\$DIR_NAME"
::TAGGING_CMD=".\tree-tagger\cmd\tree-tagger-german"
::CHUNKING_CMD=".\tree-tagger\cmd\tagger-chunker-german"

:: extract and clean text from pdf documents, remove bibliographies and learn and apply patterns
:: use ALLBUS, Eurobarometer, and NHANES as seeds (for frequency-based method: only ALLBUS)
:: apply reliability-based pattern validity assessment with threshold of 0.5: use  -r "0.5" \
:: apply frequency-based pattern validity assessment with threshold of 0.24 
:: use uppercase constraint
:: use NP constraint: add  -n "$TAGGING_CMD--@--$CHUNKING_CMD" \
:: restrict maximum number of iterations to 2

