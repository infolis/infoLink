import os
import re
import codecs
import sys

delimiter = "|"
enumMarkers = [u",", u";", u"/", u"\\", u"&", u"und", u"and"]
#enumMarkers = [u",", u";", u"/", u"\\"]
##enumMarkers = [u",", u";", u"/", u"\\", " ", "\t"]
periodEnumMarkers = u"[-,;/\\\\]|(bis)|(to)|(till)|(until)"
#periodEnumMarkers = u"[-,;/\\\\]"
periodPat = "(\d\d(\d\d)?)\s*(-|–)+\s*(\d\d(\d\d)?)"

logfile = "logFile.txt"

#TODO: optionally search for "reference date" and "publication year" fields (primary for matching term search references...)
#TODO: restructure: 1. use semi-structured representation of reference strings and information from the repositories for matching, not only strings

def filter_bestHit(filename):
    #filter: only one matching per study name
    bestMatches = getBestMatches(filename)
    with codecs.open(filename, "r", "utf-8-sig") as f:
        matchList = f.readlines()
    bestMatching = [replaceWithBestMatch(match, bestMatches) for match in matchList]
    return set(bestMatching)

def filter_bestHits(filename):
    """Filter all entries with matching date specifications. Matching: exact match or 
    target specification is covered (entirely or partially) by source specification 
    or source specification is covered (entirely or partially) by target specification. 
    Remove all entries with non-matching targets.
    """
    #filter: all highest-ranking matchings per study name
    #now: filter: all matchings per study name
    bestMatches = getBestMatches_all(filename)
    with codecs.open(filename, "r", "utf-8-sig") as f:
        matchList = f.readlines()
    bestMatching_all = [replaceWithBestMatch_all(match, bestMatches) for match in matchList]
    return set(bestMatching_all)
    
def replaceWithBestMatch(match, bestMatches):
    try:
        pubTitle,pubId,pubType,pubIdType,matchedStudyName,foundStudyName,studyId,studyType,studyIdType,confidence,method,snippet,linkType = match.split(delimiter)
        bestMatchName, bestMatchId = bestMatches.get(foundStudyName)
        if bestMatchName == "":
            #no match was found to be valid - remove entry 
            return ""
        return delimiter.join([pubTitle,pubId,pubType,pubIdType,bestMatchName,foundStudyName,bestMatchId,studyType,studyIdType,confidence,method,snippet,linkType])
    except ValueError as ve:
        #happens when there aren't enough values to unpack - happens e.g. when newlines are present in the link file
        #or when format is wrong
        print ve
        #sys.exit(0)
        return ""

def replaceWithBestMatch_all(match, bestMatches):
    """Remove non-matching candidates for entry and return a list of matching candidates."""
    newMatches = []
    try:
        pubTitle,pubId,pubType,pubIdType,matchedStudyName,foundStudyName,studyId,studyType,studyIdType,confidence,method,snippet,linkType = match.split(delimiter)
        bestMatchings = bestMatches.get(foundStudyName)
        for matching in bestMatchings:
            bestMatchName, bestMatchId = matching
            if bestMatchName == "":
                with open(logfile, "a") as f:
                    f.write("Link rejected: %s." %match.encode("utf-8-sig"))
                return ""
            if (matchedStudyName == bestMatchName and studyId == bestMatchId):
                return delimiter.join([pubTitle,pubId,pubType,pubIdType,bestMatchName,foundStudyName,bestMatchId,studyType,studyIdType,confidence,method,snippet,linkType])
        with open(logfile, "a") as f:
            f.write("Link rejected: %s." %match.encode("utf-8-sig"))
    except ValueError as ve:
        #happens when there aren't enough values to unpack - happens e.g. when newlines are present in the link file
        print ve
        print "Found here: %s" %match.encode(sys.stdout.encoding, errors = "replace")
        #sys.exit(1)
    except TypeError as te:
        #happens since codes package is used 
        print te
    return ""
        
def getBestMatches(filename):
    refAndCandidates = getCandidates(filename)
    bestMatches = {}
    for studyRef in refAndCandidates.keys():
        candidates = refAndCandidates.get(studyRef)
        bestMatches[studyRef] = getBestMatch(studyRef, candidates)
    return bestMatches

def getBestMatches_all(filename):
    """Return all entries with matching sources and targets for all entries in filename."""
    refAndCandidates = getCandidates(filename)
    bestMatches = {}
    for studyRef in refAndCandidates.keys():
        candidates = refAndCandidates.get(studyRef)
        #bestMatches[studyRef] = getBestMatch_all(studyRef, candidates)
        bestMatches[studyRef] = getAllMatches(studyRef, candidates)
        if bestMatches.get(studyRef) == []:
            with open(logfile, "a") as f:
                f.write(( u"Couldn't find any match for " + studyRef + "\n").encode(sys.stdout.encoding, errors = "replace") )
    return bestMatches

def getBestMatch(studyRef, candidates):
    #best match: exact year match + shortest candidate (use string distance additionally?)
    maxScore = [-1000, "", ""]
    for candidate in candidates:
        candName, candId = candidate
        candScore = getScore(candName, studyRef)
        if candScore > maxScore[0]:
            maxScore = [candScore, candName, candId]
        elif candScore == maxScore[0]:
            moreGeneral = getMoreGeneral(maxScore[1], maxScore[2], candName, candId)
            maxScore = [candScore, moreGeneral[0], moreGeneral[1]]
    return maxScore[1], maxScore[2]


def getBestMatch_all(studyRef, candidates):
    #best matches: like getBestMatch, but return all best candidates instead of choosing one
    maxScore = [-1000, [("", "")]]
    for candidate in candidates:
        candName, candId = candidate
        candScore = getScore(candName, studyRef)
        if candScore > maxScore[0]:
            maxScore = [candScore, [(candName, candId)]]
        elif candScore == maxScore[0]:
            maxScore[1].append((candName, candId))
    #return getMostGeneral(maxScore[1])
    return maxScore[1]
	
def getAllMatches(studyRef, candidates):
    """Return a list of all matching candidates for a studyRef."""
    #simply return all matches... 
    matchScore = [-1000, [("", "")]]
    for candidate in candidates:
        candName, candId = candidate
        candScore = getScore(candName, studyRef)
        if candScore > 0:
            matchScore[1].append((candName, candId))
    return matchScore[1][1:]

def getMoreGeneral(name1, id1, name2, id2):
    #return more general study name or first study name if both are general
    #choose integrated dataset if country variations are in candidates
    #if non of them appear to be more general, choose candidate with shorter name (assumed to be more general)
    if containsGeneralMarker(name1):
        return name1, id1
    elif containsGeneralMarker(name2):
        return name2, id2
    else:
        if len(name1) <= len(name2):
            return name1, id1
        else:
            return name2, id2

def getMostGeneral(matchList):
    #return more general study name or first study name if both are general
    #choose integrated dataset if country variations are in candidates
    #if non of them appear to be more general, return both
    mostGeneralCandidates = []
    for candidate in matchList:
        if containsGeneralMarker(candidate[0]):
            mostGeneralCandidates.append(candidate)
    if mostGeneralCandidates:
        return mostGeneralCandidates
    else:
        return matchList
    

def containsGeneralMarker(name):
    return ((name.lower().count("integrated") > 0) or (name.lower().count("integriert") > 0))

    
def exactNumberMatch(year1, string):
    """Return true if specified number/year matches the number/year in the specified string, return false otherwise."""
    #nach jahreszahl kein "-", "/" usw. falls danach ein character kommt
    #vor jahreszahl kein "-", "/" usw. falls davor eine ziffer kommt, whitespace nur nach zahl erforderlich
    allowedPrefix = "((.*[^.\\d\\s,\\\\/-]+\\s*)|(.*?\\d+\\s+)|(.*[^\\d\\s]\\s*[-.,\\\\/]+\\s*))"
    allowedSuffix = "((\\s*[^/\\d\\s\\\\-].*)|(\\s+\\d+.*)|(\\s*[^\\d\\s]\\s*[-.,\\\\/]+.*)|(\\Z))"
    searchPat = re.compile(allowedPrefix + re.escape(year1) + allowedSuffix)
    return re.match(searchPat, string)
    
def partialNumberMatch1(year1, string):
    #todo: "." sollte eher nicht davor stehen dürfen, zumindest nicht mit zahl davor
    #todo: entweder prefix mit range davor oder range im suffix - eher nicht beides -> verodern für return...
    allowedPrefix1 = "(.*[-.,\\\\/]+\\s*)"
    allowedSuffix1 = "((\\s*\\D+.*)|(\\Z))"
    allowedPrefix2 = "(.*\\D+\\s*)"
    allowedSuffix2 = "((\\s*[-.,\\\\/]+.*)|(\\Z))"
    searchPat1 = re.compile(allowedPrefix1 + re.escape(year1) + allowedSuffix1)
    searchPat2 = re.compile(allowedPrefix2 + re.escape(year1) + allowedSuffix2)
    #yearStudyRef included in candidate year, e.g. 1999 in 1999/2000
    #nach jahreszahl darf "-", "/" usw. stehen wenn danach ein character kommt
    if (re.match(searchPat1, string) or re.match(searchPat2, string)):
        return True
    
    try:
        yearsCandidate = []
        yearCandidate = extractYear(string)
        for enumMarker in enumMarkers:
            if yearCandidate.count(enumMarker) > 0:
                yearsCandidate.extend(yearCandidate.split(enumMarker))
       
        #years may also be enumerated using whitespace
        try:
            spaceEnums = re.findall("\d+\s+\d+", yearCandidate)
            for se in spaceEnums:
                #years.append(se.split(" "))
                #split using the whitespace found between the numbers (" " or "\t" etc.)
                yearsCandidate.extend(se.split(re.search("\d+(\s+)\d+", se).group(1)))
        except TypeError as te:
            # yearCandidate == []
            pass
    
        for yearMention in yearsCandidate:
            #TODO: for mentions like 2000/01: only +"20" applicable, not +"19"...
            if yearMention.strip() == year1.strip() or "19" + yearMention.strip() == year1.strip() or "20" + yearMention.strip() == year1.strip():
                return True
        return False
    except AttributeError as ae:
        return False
    
def partialNumberMatch1Short(year1, string):
    #yearStudyRef included in candidate year, e.g. 1999 in 1999/2000, if "19" or "20" is added before yearStudyRef
    #mixed short and long versions...
    #separate for different scores
    allowedPrefix1 = "(.*[-.,\\\\/]+\\s*)"
    allowedSuffix1 = "((\\D*\\s*.*)|(\\Z))"
    allowedPrefix2 = "(.*\\D*\\s*)"
    allowedSuffix2 = "((\\s*[-.,\\\\/]+.*)|(\\Z))"
    searchPat1 = re.compile(allowedPrefix1 + "((.*?19)|(.*?20))" + re.escape(year1) + allowedSuffix1)
    searchPat2 = re.compile(allowedPrefix2 + "((.*?19)|(.*?20))" + re.escape(year1) + allowedSuffix2)
    #yearStudyRef included in candidate year, e.g. 1999 in 1999/2000
    #nach jahreszahl darf "-", "/" usw. stehen wenn danach ein character kommt
    return (re.match(searchPat1, string) or re.match(searchPat2, string))
    
def partialNumberMatch2(year1, string):
    #yearStudyRef includes candidate year, e.g. map 1998-2000 to 1999 dataset
    try:
        yearRefFrom = re.search(periodPat, year1).group(1)
        yearRefTo = re.search(periodPat, year1).group(4)
        years = re.findall("\\d+", string)
        for year in years:
            if ((float(yearRefFrom) <= float(year)) and (float(yearRefTo) >= float(year))):
                return True
        return False
    except AttributeError as ae:    #candidate year is not a period
        return False

def exactNumberMatchShort(year1, string):
    #exact version match if "19" of "20" is added before yearStudyRef
    allowedPrefix = "((.*[^.\\d\\s,\\\\/-]+\\s*)|(.*?\\d+\\s+)|(.*[^\\d\\s]\\s*[-.,\\\\/]+\\s*))"
    allowedSuffix = "((\\s*[^/\\d\\s\\\\-].*)|(\\s+\\d+.*)|(\\s*[^\\d\\s]\\s*[-.,\\\\/]+.*)|(\\Z))"
    searchPat = re.compile(allowedPrefix + "((.*?19)|(.*?20))" + re.escape(year1) + allowedSuffix)
    return re.match(searchPat, string)
    
def exactPeriodMatch(year1, string):
    #periods match when removing whitespace
    allowedPrefix = "((.*[^.\\d\\s,\\\\/-]+\\s*)|(.*?\\d+\\s+)|(.*[^\\d\\s]\\s*[-.,\\\\/]+\\s*))"
    allowedSuffix = "((\\s*[^/\\d\\s\\\\-].*)|(\\s+\\d+.*)|(\\s*[^\\d\\s]\\s*[-.,\\\\/]+.*)|(\\Z))"
    year1 = re.sub("\s*" + periodEnumMarkers + "+\s*", u"-", year1)
    string = re.sub("\s*-\s*", u"-", string)
    #1999-2000 should also match 1999/2000
    year2 = re.sub("\s*-\s*", u"/", year1)
    string2 = re.sub("\s*/\s*", u"/", string)
    #1999-2000 should also match 1999\2000
    year3 = re.sub("\s*-\s*", ur"\\\\", year1)
    string3 = re.sub(r"\s*\\\s*", ur"\\", string)
    #1999-2000 should also match 1999,2000
    year4 = re.sub("\s*-\s*", u",", year1)
    string4 = re.sub("\s*,\s*", u",", string)
    #1999-2000 should also match 1999;2000
    year5 = re.sub("\s*-\s*", u";", year1)
    string5 = re.sub("\s*;\s*", u";", string)
    searchPat = re.compile(allowedPrefix + re.escape(year1) + allowedSuffix)
    searchPat2 = re.compile(allowedPrefix + re.escape(year2) + allowedSuffix)
    searchPat3 = re.compile(allowedPrefix + re.escape(year3) + allowedSuffix)
    searchPat4 = re.compile(allowedPrefix + re.escape(year4) + allowedSuffix)
    searchPat5 = re.compile(allowedPrefix + re.escape(year5) + allowedSuffix)
    return (re.match(searchPat, string) or re.match(searchPat2, string2) or re.match(searchPat3, string3) or re.match(searchPat4, string4) or re.match(searchPat5, string5))
    
def periodPartlyCovered(year1a, year1b, year2a, year2b):
    """Check whether period a entirely or partly covers period b. Period a: year1a - year1b; period b: year2a - year2b. 
    4 cases may occur:
    1. period b is entirely covered: e.g. 1991-1999 in 1990-2000
    2. period b is partly covered 1): e.g. 1980-1999 in 1990-2000
    3. period b is partly covered 2): e.g. 1991-2013 in 1990-2000
    4. period a is entirely covered: e.g. 1980-2013 in 1990-2000"""
    case1 = ((year2a >= year1a) and (year2b <= year1b))
    case2 = ((year2a < year1a) and (year2b <= year1b) and (year2b >= year1a))
    case3 = ((year2a >= year1a) and (year2b > year1b) and (year2a <= year1b))
    case4 = ((year2a <= year1a) and (year2b >= year1b))
    return (case1 or case2 or case3 or case4)

def getScore(string1, string2):
    """Compute a score for the similarity of two strings (i.e. for the 
    similarity of a target snippet and a candidate for matching). 
    ..."""
    #compute score for similarity of strings
    #first score: year similarity (exact year match?)
    #second score: string similarity (levenshtein distance)
    #TODO: rather use token-based similarity, e.g. cosine, or combine both
    string2 = re.sub("-", "-", string2)
    string1 = re.sub("-", "-", string1)
    
    yearScore = -2000
    yearStudyRef = extractYear(string2)
	#handle enumeration of numbers / years / versions
    years = []
    
    for enumMarker in enumMarkers:
        if yearStudyRef.count(enumMarker) > 0:
            years.extend(yearStudyRef.split(enumMarker))
    #years may also be enumerated using whitespace
    #spaceEnums = re.findall("(\d+)\s+(\d+)", yearStudyRef)
    try:
        spaceEnums = re.findall("\d+\s+\d+", yearStudyRef)
        for se in spaceEnums:
            #years.append(se.split(" "))
            #split using the whitespace found between the numbers (" " or "\t" etc.)
            years.extend(se.split(re.search("\d+(\s+)\d+", se).group(1)))
    except TypeError as te:
        #yearStudyRef == []
        pass
    years.append(yearStudyRef)

    if not yearStudyRef: #no number found in string2
        yearScore = 0.1
        
    elif not extractYear(string1): #no number found in string1
        yearScore = 0.2
        
    else:
    
        for yearStudyRef in years:
        #every number specification in years is used for finding a match
        #if any match has a score greater than 0, the candidate is accepted
        #note: the score returned is the score of the last match, not the highest score for any of the number specifications
        #to return highest possible value, simply collect all values in list and return highest value
            if exactNumberMatch(yearStudyRef, string1):
                yearScore = 1
                #print "Score 1.0: Mapped " + yearStudyRef + " to " + string1    
            elif partialNumberMatch1(yearStudyRef, string1):
                yearScore = 0.5
                #print "Score 0.5 (partial 1): Mapped " + yearStudyRef + " to " + string1
            elif len(yearStudyRef) == 2:
                if exactNumberMatchShort(yearStudyRef, string1): 
                    yearScore = 0.9
                    #print "Score 0.9: Mapped " + yearStudyRef + " to " + string1
                elif partialNumberMatch1Short(yearStudyRef, string1):
                    yearScore = 0.5
                    #print "Score 0.5 (partial 1 short): Mapped " + yearStudyRef + " to " + string1            
            if re.search(periodPat, yearStudyRef):
                #check: exact periods may be covered by exactNumberMatch
                #if yearScore > 0:
                 #   print "Score: %f" %yearScore
                  #  print years
                #year in yearStudyRef is a period rather than a year
                if exactPeriodMatch(yearStudyRef, string1):
                    yearScore = 1
                    #print "Score 1.0: Mapped " + yearStudyRef + " to " + string1
                else:
                    #no direct match of year specifications
                    yearStudyRefFrom = re.search(periodPat, yearStudyRef).group(1)
                    yearStudyRefTo = re.search(periodPat, yearStudyRef).group(4)
                
                    if len(yearStudyRefFrom) == 2:
                        yearStudyRefFrom1 = "19" + yearStudyRefFrom
                        yearStudyRefFrom2 = "20" + yearStudyRefFrom
                    else:
                        yearStudyRefFrom1 = yearStudyRefFrom
                        yearStudyRefFrom2 = yearStudyRefFrom
                    if len(yearStudyRefTo) == 2:
                        if ((len(yearStudyRefFrom) == 4 and yearStudyRefFrom.startswith("19")) or len(yearStudyRefFrom) == 2):
                            if float("19" + yearStudyRefTo) < yearStudyRefFrom1:
                                yearStudyRefTo1 = "20" + yearStudyRefTo
                            else:
                                yearStudyRefTo1 = "19" + yearStudyRefTo
                        else:
                            yearStudyRefTo1 = "20" + yearStudyRefTo
                        if ((len(yearStudyRefFrom) == 4 and yearStudyRefFrom.startswith("20")) or len(yearStudyRefFrom) == 2):
                            yearStudyRefTo2 = "20" + yearStudyRefTo
                        else:
                            if float("19" + yearStudyRefTo) < yearStudyRefFrom1:
                                yearStudyRefTo2 = "20" + yearStudyRefTo
                            else:
                                yearStudyRefTo2 = "19" + yearStudyRefTo
                    else:
                        yearStudyRefTo1 = yearStudyRefTo
                        yearStudyRefTo2 = yearStudyRefTo
                    #periods match when removing whitespace and adding "19" or "20" before
                    if exactPeriodMatch(yearStudyRefFrom1+"-"+yearStudyRefTo1, string1) or exactPeriodMatch(yearStudyRefFrom2+"-"+yearStudyRefTo2, string1) :
                        yearScore = 0.9
                        #print "Score 0.9: Mapped " + yearStudyRef + " to " + string1         
                    else:
                        if re.search(periodPat, string1):
                            #candidate year is period itself
                            yearCandiFrom = re.search(periodPat, string1).group(1)
                            yearCandiTo = re.search(periodPat, string1).group(4)
                            
                            if len(yearCandiFrom) == 2:
                                yearCandiFrom1 = "19" + yearCandiFrom
                                yearCandiFrom2 = "20" + yearCandiFrom
                            else:
                                yearCandiFrom1 = yearCandiFrom
                                yearCandiFrom2 = yearCandiFrom
                            if len(yearCandiTo) == 2:
                                if ((len(yearCandiFrom) == 4 and yearCandiFrom.startswith("19")) or len(yearCandiFrom) == 2):
                                    if float("19" + yearCandiTo) < yearCandiFrom1:
                                        yearCandiTo1 = "20" + yearCandiTo
                                    else:
                                        yearCandiTo1 = "19" + yearCandiTo
                                else:
                                    yearCandiTo1 = "20" + yearCandiTo
                                if ((len(yearCandiFrom) == 4 and yearCandiFrom.startswith("20")) or len(yearCandiFrom) == 2):
                                    yearCandiTo2 = "20" + yearCandiTo
                                else:
                                    if float("19" + yearCandiTo) < yearCandiFrom1:
                                        yearCandiTo2 = "20" + yearCandiTo1
                                    else:
                                        yearCandiTo2 = "19" + yearCandiTo
                            else:
                                yearCandiTo1 = yearCandiTo
                                yearCandiTo2 = yearCandiTo
                        
                            #merged partly and entirely covered, receive same score
                            #if needed, split periodPartlyCovered into 2 methods: partlyCovered and entirelyCovered
                            if (periodPartlyCovered(float(yearStudyRefFrom1), float(yearStudyRefTo1), float(yearCandiFrom1), float(yearCandiTo1)) or periodPartlyCovered(float(yearStudyRefFrom2), float(yearStudyRefTo2), float(yearCandiFrom1), float(yearCandiTo1)) or 
                            periodPartlyCovered(float(yearStudyRefFrom1), float(yearStudyRefTo1), float(yearCandiFrom2), float(yearCandiTo2)) or periodPartlyCovered(float(yearStudyRefFrom2), float(yearStudyRefTo2), float(yearCandiFrom2), float(yearCandiTo2))):
                                yearScore = 0.3
                                """
                                print "Study ref1: %s - %s" %(yearStudyRefFrom1, yearStudyRefTo1)
                                print "Study ref2: %s - %s" %(yearStudyRefFrom2, yearStudyRefTo2)
                                print "Candi ref1: %s - %s" %(yearCandiFrom1, yearCandiTo1)
                                print "Candi ref2: %s - %s" %(yearCandiFrom2, yearCandiTo2)
                                print "first part: " + str(periodPartlyCovered(float(yearStudyRefFrom1), float(yearStudyRefTo1), float(yearCandiFrom1), float(yearCandiTo1)))
                                print "second part: " + str(periodPartlyCovered(float(yearStudyRefFrom2), float(yearStudyRefTo2), float(yearCandiFrom1), float(yearCandiTo1)))
                                print "third part: " + str(periodPartlyCovered(float(yearStudyRefFrom1), float(yearStudyRefTo1), float(yearCandiFrom2), float(yearCandiTo2)))
                                print "fourth part: " + str(periodPartlyCovered(float(yearStudyRefFrom2), float(yearStudyRefTo2), float(yearCandiFrom2), float(yearCandiTo2)))
                                """
                                #print u"Score 0.3: Mapped " + yearStudyRef + u" (= " + yearStudyRefFrom1 + u" + " + yearStudyRefTo1 + u") " + u" to " + string1 
                        elif partialNumberMatch2(yearStudyRefFrom1+u"-"+yearStudyRefTo1, string1) or partialNumberMatch2(yearStudyRefFrom2+"u-"+yearStudyRefTo2, string1):
                            yearScore = 0.2
                            #print "Score 0.2: Mapped " + yearStudyRef + " to " + string1
                            #year of candidate study is covered by period of yearStudyRef
                            #e.g. soep 2000-2004 is referenced, soep 2000 is the candidate
            #use below code to search for "partial datasets"
            #-> e.g. soep 2000 is referenced, soep 2000-2004 is the candidate
            elif re.search(periodPat, string1):
                #candidate year is a period
                yearCandiFrom = re.search(periodPat, string1).group(1)
                yearCandiTo = re.search(periodPat, string1).group(4)
                try:
                    if (float(yearStudyRef) >= float(yearCandiFrom)) and (float(yearStudyRef) <= float(yearCandiTo)):
                        #yearStudyRef lies in between the candidate's period
                        yearScore = 0.5
                        #print "Score 0.5: Mapped " + yearStudyRef + " to " + string1 + "(= " + yearCandiFrom + " - " + yearCandiTo + ")"
                    elif (float("19" + yearStudyRef) >= float(yearCandiFrom)) and (float("19" + yearStudyRef) <= float(yearCandiTo)) or (float("20" + yearStudyRef) >= float(yearCandiFrom)) and (float("20" + yearStudyRef) <= float(yearCandiTo)):
                        yearScore = 0.15
                except ValueError as ve:
                    #print "Could not convert value to float - ignoring."
                    #print ve
                    pass
                    #with open(logfile, "a") as f:
                    #    f.write(( u"Couldn't find any match for " + string1 + "( tried converting to float at end)\n").encode(sys.stdout.encoding, errors = "replace") )
                    #print "Could not find any match for " + yearStudyRef.encode("utf-8-sig")
            
            #else:
            #    yearScore = -2000
            #stringScore = levenshtein(string1, string2)
    
    stringScore = 0
    return yearScore - stringScore

def unescapeString(string):
    return string.decode("string_escape")	

def extractYear(string):
    """Extract the year specification inside of string.
    ..."""
    try:
        return re.search("\d+.*\d+", string).group().replace(u"\u2013", u"-") #workaround...
    except AttributeError:
        try:
            return re.search("\d", string).group().replace(u"\u2013", u"-") #workaround...
        except AttributeError: #no number found in string
            return []

def levenshtein(seq1, seq2):
    """Return the Levenshtein string distance for seq1 and seq2."""
    oneago = None
    thisrow = range(1, len(seq2) + 1) + [0]
    for x in xrange(len(seq1)):
        twoago, oneago, thisrow = oneago, thisrow, [0] * len(seq2) + [x + 1]
        for y in xrange(len(seq2)):
            delcost = oneago[y] + 1
            addcost = thisrow[y - 1] + 1
            subcost = oneago[y - 1] + (seq1[x] != seq2[y])
            thisrow[y] = min(delcost, addcost, subcost)
    return thisrow[len(seq2) - 1]


def getCandidates(filename):
    """Read all entries in filename and return a dictionary having reference study names 
    (found text snippets assumed to be study names and to be matched to records) as keys 
    and a list of candidates to be matching records for the key as values.
    """
    with codecs.open(filename, "r", "utf-8-sig") as f:
        matchList = f.readlines()
    refAndCandidates = {}
    for match in matchList:
        try:
            pubTitle,pubId,pubType,pubIdType,matchedStudyName,foundStudyName,studyId,studyType,studyIdType,confidence,method,snippet,linkType = match.split(delimiter)
            candidates = refAndCandidates.setdefault(foundStudyName, set())
            candidates.add((matchedStudyName, studyId))
            refAndCandidates[foundStudyName] = candidates
        except ValueError as ve:
            #happens when there aren't enough values to unpack - happens e.g. when newlines are present in the link file
            print ve
            print "Found here: %s" %match.encode(sys.stdout.encoding, errors = "replace")
            print filename
            continue
    return refAndCandidates


if __name__=="__main__":

    candidate0 = "Studierendensurvey 2000/01"
    refString0 = "Studierendensurvey 2001"
    candidate = "German Social Survey (ALLBUS) Cumulative File, 1980, 1982, 1984, 1986"
    candidate2 = "German Social Survey (ALLBUS) Cumulative File, 1980-1992"
    candidate3 = "German Social Survey (ALLBUS) Cumulative File, 1980, 1992"
    candidate4 = "German Social Survey (ALLBUS) Cumulative File, 1980, 1996"
    candidate5 = "German Social Survey (ALLBUS) Cumulative File, 1980 - 1996"
    candidate6 = "German Social Survey (ALLBUS) Cumulative File, 1980 1996"
    candidate7 = "German Social Survey (ALLBUS) Cumulative File, 1980-1990"
    
    refString = "ALLBUS 1996/08"
    refString2 = "ALLBUS 1982"
    refString3 = "ALLBUS 1982   -   1983"
    refString4 = "ALLBUS 85/01"

    print "Correct value: > 0 ", getScore(candidate0, refString0)
    print "Correct value: < 0 ", getScore(candidate, refString)
    print "Correct value: < 0 ", getScore(candidate2, refString)
    print "Correct value: < 0 ", getScore(candidate3, refString2)
    print "Correct value: > 0 ", getScore(candidate4, refString)
    print "Correct value: > 0 ", getScore(candidate5, refString2)
    print "Correct value: > 0 ", getScore(candidate5, refString3)
    print "Correct value: < 0 ", getScore(candidate4, refString3)
    print "Correct value: > 0 ", getScore(candidate5, refString)
    print "Correct value: < 0 ", getScore(candidate6, refString2)
    print "Correct value: > 0 ", getScore(candidate6, refString)
    print "Correct value: > 0 ", getScore(candidate7, refString4)
    