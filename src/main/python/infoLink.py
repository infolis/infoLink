# -*- coding: utf-8 -*-

import os
import subprocess
from optparse import OptionParser
import ntpath
import codecs

def path_leaf(path):
    """Return filename regardless of file separator used."""
    head, tail = ntpath.split(path)
    return tail or ntpath.basename(head)

parser = OptionParser()
parser.add_option("-c", "--corpus", dest="corpus", help="extract references from documents in CORPUS_PATH", metavar="CORPUS_PATH")
parser.add_option("-o", "--outpath", dest="outpath", help="write InfoLink output to OUTPUT_PATH", metavar="OUTPUT_PATH")
parser.add_option("-p", "--patterns", dest="patterns", help="use patterns in PATTERNS_PATH for InfoLink reference extraction", metavar="PATTERNS_PATH")
parser.add_option("-l", "--learnpath", dest="learnpath", help="learn extraction patterns from corpus and save training data to this directory", metavar="TRAIN_PATH")
parser.add_option("-e", "--extract", dest="extract", help="extract text from CORPUS and store in TXT_PATH", metavar="TXT_PATH")
parser.add_option("-i", "--index", dest="index", help="use / generate this Lucene Index for documents in corpus", metavar="INDEX")
parser.add_option("-s", "--seed", dest="seed", help="learn extraction patterns using this seed", metavar = "SEED")
parser.add_option("-t", "--terms", dest="terms", help="apply term search for dataset names listed in this file", metavar = "TERMS_FILENAME")
parser.add_option("-n", "--npConstraint", dest="np", help="if set, use NP constraint with the specified tree tagger arguments TAGGER_ARGS", metavar="TAGGER_ARGS")
parser.add_option("-u", "--ucConstraint", action="store_true", dest="uc", help="if set, use upper-case constraint")
parser.add_option("-m", "--idMapPath", dest="idMapPath", help="use csv file ID_MAP_PATH to retrieve ID of documents in corpus", metavar="ID_MAP_PATH")
parser.add_option("-f", "--frequency", dest="frequency", help="use frequency-based measure for pattern validation with the specified threshold", metavar="FREQUENCY_THRESHOLD")
parser.add_option("-r", "--reliability", dest="reliability", help="use reliability-based measure for pattern validation with the specified threshold", metavar = "RELIABILITY_THRESHOLD")
parser.add_option("-N", "--maxN", dest="maxN", help="set the maximum number of iterations (default: 4)", metavar = "MAX_ITERATIONS")
parser.add_option("-C", "--javaClassPath", dest="classpath", help="Set the java classpath", metavar="CLASSPATH")

options, args = parser.parse_args()

classpath = "."
if options.classpath:
    classpath = options.classpath

#make absolute paths before passing arguments to Java modules
if options.corpus: #necessary because None in options.corpus will default in current absolute path...
    options.corpus = os.path.abspath(options.corpus)
if options.outpath:
    options.outpath = os.path.abspath(options.outpath)
if options.patterns:
    options.patterns = os.path.abspath(options.patterns)
if options.learnpath:
    options.learnpath = os.path.abspath(options.learnpath)
if options.extract:
    options.extract = os.path.abspath(options.extract)
if options.index:
    options.index = os.path.abspath(options.index)
if options.terms:
    options.terms = os.path.abspath(options.terms)

#1) Preprocessing

#if patterns are to be learned, bibliographies must be removed
#if bibliographies are to be removed, page-wise bib extraction is needed
#without learning, do not extract bibliographies or split documents into pages
if options.extract:
    flag_pagewise = (options.learnpath != None)
    if flag_pagewise:
	textExtractionCmd = ["java", "-classpath", classpath, "preprocessing.TextExtractor", "-i", options.corpus, "-o", options.extract, "-p"]
    else:
        textExtractionCmd = ["java", "-classpath", classpath, "preprocessing.TextExtractor", "-i", options.corpus, "-o", options.extract]
    #remove last file separator
    if options.extract.endswith("\\") or options.extract.endswith("/"):
        options.extract = options.extract[:-1]
    if not os.path.exists(options.extract):
        os.makedirs(options.extract)
    if not os.path.exists(options.extract + "_clean"):
        os.makedirs(options.extract + "_clean")
    textCleaningCmd = ["java", "-classpath", classpath, "preprocessing.Cleaner", options.extract, options.extract + "_clean"]
    print "Calling %s..." %textExtractionCmd
    p = subprocess.Popen(textExtractionCmd)
    p.wait()
    p = subprocess.Popen(textCleaningCmd)
    p.wait()
    if options.learnpath:
        import bibRemover
        #remove bibliographies and make documents whole again (merge pages to documents)
        cleanTextPagesPath = options.extract + "_clean"
        head, tail = os.path.split(cleanTextPagesPath)
        suspectedBibsFile = os.path.abspath(os.path.join(cleanTextPagesPath, "..", tail + "_suspectedBibs.csv"))
        biblessDocsPath = os.path.abspath(os.path.join(cleanTextPagesPath, "..", tail + "_biblessDocs"))
        if not os.path.exists(biblessDocsPath):
            os.makedirs(biblessDocsPath)
        bibRemover.makeBiblessDocs(cleanTextPagesPath, suspectedBibsFile, biblessDocsPath)

        corpusPath = biblessDocsPath
    else:
        corpusPath = cleanTextPagesPath

else:
    corpusPath = options.corpus

#create Lucene Index
print "Creating Lucene Index for %s in %s" %(corpusPath, options.index)
indexingCmd = ["java", "-classpath", classpath, "luceneIndexing.Indexer", corpusPath, options.index]
print "Calling\n%s" %indexingCmd
p = subprocess.Popen(indexingCmd)
p.wait()

# 2) InfoLink reference extraction (pattern-based or term-search-based)

flags = []
if options.uc:
    flags.append("-u")

#construct option string from options and corresponding values to pass over to learner
optionStr = []
optionDict = vars(options)
learnerOptionNameDict = { "outpath" : "-o", "index" : "-i", "patterns" : "-p", "terms" : "-t", "seed" : "-s", "learnpath" : "-l", "reliability" : "-r", "frequency" : "-f", "np": "-n", "maxN" : "-N"}
for item in optionDict.items():
    if item[0] == "uc":
        pass
    elif item[1]:
        try:
            optionStr.append(learnerOptionNameDict.get(item[0], ""))
            optionStr.append(item[1])
        except TypeError as te:
            #option is not related to learner and thus not included in learnerOptionNameDict (e.g. -e)
            pass
optionStr.append("-c")
optionStr.append(corpusPath)
optionStr.extend(flags)

#reference extraction - learn new patterns or use existing patterns
learnerCmd = ["java", "-Xmx1g", "-Xms1g", "-classpath", classpath, "patternLearner.Learner"]
learnerCmd.extend(optionStr)
print "Calling\n%s" %learnerCmd
p = subprocess.Popen(learnerCmd)
p.wait()


# 3) Post-processing: complete output files
patOut = "\" \""
termsOut = "\" \""
if options.patterns or options.learnpath:
    patOut = os.path.join(options.outpath, "contexts.xml")
    print "Completing infoLink output files %s" %patOut
if options.terms:
    termsOut = os.path.join(options.outpath, "contexts.xml")
    print "Completing infoLink output files %s" %termsOut

# 4) InfoLink reference matching
#TODO: QUERYCACHE AND EXTERNALURLS AS PARAMETERS OR IN INI FILE OR WHATEVER
print "Matching references and dataset records..."
matching_prefix = "\" \"" #supply prefix to process subcorpora inside of corpusPath having the specified prefix
matching_index = "\" \"" #use options.index here to include snippets of found dataset names that have been found using term-search
queryCache = os.path.join(options.outpath, "queryCache.csv")
#queryCache = "\" \""
externalURLs = os.path.join(options.outpath, "externalStudiesIndex.txt")
matchingOptions = [corpusPath, matching_prefix, patOut, termsOut, options.outpath, matching_index, options.idMapPath, queryCache, externalURLs]
matchingCmd = ["java", "-classpath", classpath, "patternLearner.ContextMiner"]
matchingCmd.extend(matchingOptions)
print "Calling\n%s" %matchingCmd
os.system(" ".join(matchingCmd))

# 5) Filter links
#TODO: IN PARAMETER OUTPUT FILE INCLUDE MATCHING PARAMETERS!!!
print "Filtering links (accept only datasets with corresponding years / numbers ...)"
linkfile_patterns = os.path.join(options.outpath, "links_doi_patterns_unfiltered.csv")
linkfile_terms = os.path.join(options.outpath, "links_doi_terms_unfiltered.csv")
linkfile_patterns_filtered = os.path.join(options.outpath, "links_doi_patterns.csv")
linkfile_terms_filtered = os.path.join(options.outpath, "links_doi_terms.csv")

from Filter import filter_bestHits

def filter(linkfile_in):
    linkfile_out = linkfile_in.replace("_unfiltered.csv", ".csv")
    linkfile_out = linkfile_in.replace("_unfiltered_unknownURN.csv", "_unknownURN.csv")
    try:
        with codecs.open(linkfile_out, "w", "utf-8-sig") as f:
            for line in filter_bestHits(linkfile_in):
                f.write(line)
        print "Wrote %s\n" %linkfile_out
    except IOError as ioe:
        print ioe

filter(linkfile_patterns)
print "\n%s\n" %linkfile_patterns.replace(".csv", "_unknownURN.csv")
filter(linkfile_patterns.replace(".csv", "_unknownURN.csv"))
filter(linkfile_terms)
filter(linkfile_terms.replace(".csv", "_unknownURN.csv"))

# 6) Post-processing: create JSON file for visualization in LinkViz
print "Creating JSON output files..."
import Json

def convertToJson(filename):
    jsonOut_pat = filename.replace(".csv", ".json")
    with open(jsonOut_pat, "w") as f:
        f.write(str(Json.convertToJson(filename)))
    print "Wrote %s." %jsonOut_pat

if patOut != "\" \"":
    try:
        convertToJson(linkfile_patterns_filtered)
    except IOError as ioe:
        print ioe

    try:
        convertToJson(linkfile_patterns_filtered.replace(".csv", "_unknownURN.csv"))
    except IOError as ioe:
        print ioe

if termsOut != "\" \"":
    try:
        convertToJson(linkfile_terms_filtered)
    except IOError as ioe:
        print ioe

    try:
        convertToJson(linkfile_terms_filtered.replace(".csv", "_unknownURN.csv"))
    except IOError as ioe:
        print ioe
