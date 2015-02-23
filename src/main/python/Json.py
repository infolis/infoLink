# -*- coding: utf8 -*-
import json
import codecs

#group 0 is reserved for publications
colorList = ["dummy"]
colorSet = set()

def convertToJson(filename):
    """convert InFoLiS linkfile to json graph to be imported by linkViz..."""
    nodeSet = set()
    nodeList = []
    linkList = []
    linkSet = set()
    jsonGraph = {}
    studyRefDict = {}
    #construct studyRefDict (collect all different strings used to refer to a specific study id)
    with codecs.open(filename, "r", "latin-1") as f:
        for line in f:
            try:
                pubName, pubId, pubType, pubLinkType, studyName_dara, studyName, studyId, \
                studyType, studyLinkType, confidence, method, textsnippet, linktype = \
                line.split("|")
            except ValueError as ve:
                print ve
                print line.encode("latin-1")
                exit(1)
            studyNames = studyRefDict.setdefault(studyId, set())
            studyNames.add(studyName)
            studyRefDict[studyId] = studyNames

    #construct node list for studies and publications, link list and generate json graph
    with codecs.open(filename, "r", "latin-1") as f:
        for line in f:
            pubName, pubId, pubType, pubLinkType, studyName_dara, studyName, studyId, \
            studyType, studyLinkType, confidence, method, textsnippet, linktype = \
            line.split("|")
            if not pubName:
                pubName = pubId
            pubNode = {"id": pubId, "name": pubName, "type": pubType, "linktype": pubLinkType, "group": 0}
            #group study nodes by studyname and assign separate group for publications
            #group = determineGroup(studyName)
            group = determineGroup("|".join(studyRefDict.get(studyId)))
            #study ids are not unique in link list because one doi may be referened by different
            #reference strings (e.g. eurobarometer 1993 and eurobarometer 39a refer to
            #the same study)
            #in the json graph however, there is only one node per study id and its name consists
            #of all different reference strings found in the links
            #(use commented code when constructing one node for each reference name)
            #uniqueStudyId = studyId + " (" + "|".join(studyRefDict.get(studyId)) + ")"
            uniqueStudyId = studyId
            #(use commented code when constructing one node for each reference name)
            #studyNode = {"id": uniqueStudyId, "name": studyName, "type": studyType, "linktype": studyLinkType, "group": group}
            studyNode = {"id": uniqueStudyId, "name": "|".join(studyRefDict.get(studyId)), "type": studyType, "linktype": studyLinkType, "group": group}
            linkId = pubId + uniqueStudyId + studyName
            linkNode = {"source": pubId, "target": uniqueStudyId, "refName": studyName}
            #nodes may appear in multiple links - avoid duplicates
            if not pubId in nodeSet:
                nodeList.append(pubNode)
                nodeSet.add(pubId)
            if not uniqueStudyId in nodeSet:
                nodeList.append(studyNode)
                nodeSet.add(uniqueStudyId)
            #links may also have duplicates if there are same entries with different confidences and if they should all be exported
            if not linkId in linkSet:
                linkSet.add(linkId)
                linkList.append(linkNode)
    jsonGraph = {"links": linkList, "nodes": nodeList}
    return json.JSONEncoder().encode(jsonGraph)

#assign studies with different dois but the same reference name to one group
#nodes in one group will have the same color in the visualization
def determineGroup(nodeName):
    if not nodeName in colorSet:
        colorSet.add(nodeName)
        colorList.append(nodeName)
    return colorList.index(nodeName)

if __name__=="__main__":
    filename = "links_doi_patterns.csv"
    with codecs.open(filename.replace(".csv", ".json"), "w", "latin-1") as f:
        f.write(str(convertToJson(filename)))
