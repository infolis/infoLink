# -*- coding: utf-8 -*-

import os
import subprocess
from optparse import OptionParser
import ntpath
import codecs
import logging

logging.basicConfig(level=logging.DEBUG)
logging.log(1, 'foo')
def _log(lvl, msg):
    colors = [ 34, 33, 32, 31, '31;1' ]
    if os.name == 'posix':
        msg = "\x1B[%sm%s\x1B[0m" % (colors[lvl], msg)
    logging.log(lvl*10, msg)

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
parser.add_option("-n", "--npConstraint", action="store_true", dest="np", help="if set, use NP constraint")
parser.add_option("-u", "--ucConstraint", action="store_true", dest="uc", help="if set, use upper-case constraint")
parser.add_option("-m", "--idMapPath", dest="idMapPath", help="use csv file ID_MAP_PATH to retrieve ID of documents in corpus", metavar="ID_MAP_PATH")
parser.add_option("-g", "--german", action="store_true", dest="german", help="if set, use language german, use english else", metavar="LANG_GERMAN")
parser.add_option("-f", "--frequency", dest="frequency", help="use frequency-based measure for pattern validation with the specified threshold", metavar="FREQUENCY_THRESHOLD")
parser.add_option("-r", "--reliability", dest="reliability", help="use reliability-based measure for pattern validation with the specified threshold", metavar = "RELIABILITY_THRESHOLD")
parser.add_option("-N", "--maxN", dest="maxN", help="set the maximum number of iterations (default: 4)", metavar = "MAX_ITERATIONS")
parser.add_option("-F", "--frequencyStrategy", dest="strategy", help="set the strategy for processing new contexts within frequeny-based framework. Allowed values: \"mergeCurrent\", \"mergeNew\", \"mergeAll\", \"separate\" (default)", metavar = "FREQUENCY_STRATEGY")
parser.add_option("-C", "--javaClassPath", dest="classpath", help="Set the java classpath", metavar="CLASSPATH")

options, args = parser.parse_args()

infoLinkClassPath = options.classpath
_log(1, "Python class path: %s" % infoLinkClassPath)

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

def getRunCommand(scriptName, args):
    """
    Build an array of command line components for consumption by subprocess.Popen
    """
    ret = []
    scriptDir = os.path.abspath('build/install/infoLink/bin')
    if os.name == 'posix':
        ret.append("bash")
        ret.append("%s/%s" % (scriptDir, scriptName))
    else:
        ret.append("cmd")
        ret.append("%s/%s.bat" % (scriptDir, scriptName))
    for arg in args:
        ret.append(arg)
    _log(1, "Calling '%s'" % ' '.join(ret))
    return ret

#1) Preprocessing

#if patterns are to be learned, bibliographies must be removed
#if bibliographies are to be removed, page-wise bib extraction is needed
#without learning, do not extract bibliographies or split documents into pages
if options.extract:
    flag_pagewise = (options.learnpath != None)

    # build TextExtraction command
    textExtractionArgs = ["-i", options.corpus, "-o", options.extract]
    if flag_pagewise:
        textExtractionArgs.append("-p")
    textExtractionCmd = getRunCommand('TextExtractor', textExtractionArgs)
    p = subprocess.Popen(textExtractionCmd, cwd=infoLinkClassPath)
    p.wait()

    #remove last file separator
    if options.extract.endswith("\\") or options.extract.endswith("/"):
        options.extract = options.extract[:-1]
    if not os.path.exists(options.extract):
        os.makedirs(options.extract)
    if not os.path.exists(options.extract + "_clean"):
        os.makedirs(options.extract + "_clean")

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
indexingCmd = getRunCommand("Indexer", [corpusPath, options.index])
_log(2, "Creating Lucene Index for %s in %s with cmd\n '%s'" % (corpusPath, options.index, indexingCmd))

p = subprocess.Popen(indexingCmd, cwd=infoLinkClassPath)
p.wait()

# 2) InfoLink reference extraction (pattern-based or term-search-based)

#construct option string from options and corresponding values to pass over to learner
learnerOptions = []
learnerOptionNameDict = { "outpath" : "-o", "index" : "-i", "patterns" : "-p", "terms" : "-t", "seed" : "-s", "learnpath" : "-l", "reliability" : "-r", "frequency" : "-f", "np": "-n", "maxN" : "-N", "strategy" : "-F"}
optionsDict = vars(options) # optparse why u no dict
for scriptOption, learnerOption in learnerOptionNameDict.items():
    if scriptOption in optionsDict and optionsDict[scriptOption] is not None:
        learnerOptions.append(learnerOption)
        learnerOptions.append(optionsDict[scriptOption])
learnerOptions.append("-c")
learnerOptions.append(corpusPath)
if options.uc:
    learnerOptions.append("-u")
if options.np:
    learnerOptions.append("-n")
if options.german:
    learnerOptions.append("-g")


#reference extraction - learn new patterns or use existing patterns
learnerCmd = getRunCommand("Learner", learnerOptions)
p = subprocess.Popen(learnerCmd, cwd=infoLinkClassPath)
p.wait()


# 3) Post-processing: complete output files
patOut = "\" \""
termsOut = "\" \""
if options.patterns or options.learnpath:
    patOut = os.path.join(options.outpath, "contexts.xml")
    _log(2, "Completing infoLink output files %s" %patOut)
if options.terms:
    termsOut = os.path.join(options.outpath, "contexts.xml")
    _log(2, "Completing infoLink output files %s" %termsOut)

# 4) InfoLink reference matching
#TODO: QUERYCACHE AND EXTERNALURLS AS PARAMETERS OR IN INI FILE OR WHATEVER
_log(2, "Matching references and dataset records...")
matching_prefix = "\" \"" #supply prefix to process subcorpora inside of corpusPath having the specified prefix
matching_index = "\" \"" #use options.index here to include snippets of found dataset names that have been found using term-search
queryCache = os.path.join(options.outpath, "queryCache.csv")
#queryCache = "\" \""
externalURLs = os.path.join(options.outpath, "externalStudiesIndex.txt")
matchingOptions = [corpusPath, matching_prefix, patOut, termsOut, options.outpath, matching_index, options.idMapPath, queryCache, externalURLs]
matchingCmd = getRunCommand("ContextMiner", matchingOptions)
p = subprocess.Popen(matchingCmd, cwd=infoLinkClassPath)
p.wait()

# 5) Filter links
#TODO: IN PARAMETER OUTPUT FILE INCLUDE MATCHING PARAMETERS!!!
_log(2, "Filtering links (accept only datasets with corresponding years / numbers ...)")
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
        _log(2, "Wrote %s\n" %linkfile_out)
    except IOError as ioe:
        _log(3, ioe)
        print ioe

filter(linkfile_patterns)
_log(2, "\n%s\n" %linkfile_patterns.replace(".csv", "_unknownURN.csv"))
filter(linkfile_patterns.replace(".csv", "_unknownURN.csv"))
filter(linkfile_terms)
filter(linkfile_terms.replace(".csv", "_unknownURN.csv"))

# 6) Post-processing: create JSON file for visualization in LinkViz
_log(2, "Creating JSON output files...")
import Json

def convertToJson(filename):
    jsonOut_pat = filename.replace(".csv", ".json")
    with open(jsonOut_pat, "w") as f:
        f.write(str(Json.convertToJson(filename)))
    _log(2, "Wrote %s." %jsonOut_pat)

if patOut != "\" \"":
    try:
        convertToJson(linkfile_patterns_filtered)
    except IOError as ioe:
        _log(3, ioe)
        print ioe

    try:
        convertToJson(linkfile_patterns_filtered.replace(".csv", "_unknownURN.csv"))
    except IOError as ioe:
        _log(3, ioe)
        print ioe

if termsOut != "\" \"":
    try:
        convertToJson(linkfile_terms_filtered)
    except IOError as ioe:
        _log(3, ioe)
        print ioe

    try:
        convertToJson(linkfile_terms_filtered.replace(".csv", "_unknownURN.csv"))
    except IOError as ioe:
        _log(3, ioe)
        print ioe
