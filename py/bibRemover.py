
 
import re
import os
import shutil

def getDocumentName(filename):
	"""Restore the original document name from the files representing single pages of a document."""
	return re.sub("_\d+_clean.txt", "", filename)
	
def getPageNumber(filename):
	"""Extract the page number from a file representing a single page of a document."""
	return int(re.search("_(\d*)_clean.txt", filename).group(1))
	
def sortPages(pageList, discontinuous = False):
    """Sort list of pages by page number.
    There may be missing pages, e.g. if removed by bibRemover first, thus sortedPageList does not need to have equal length as pageList."""
    if not discontinuous:
        sortedPageList = list(pageList)
    else:
        sortedPageList = [ ]
        #max length of documents: 1500 pages
        for i in range(1500):
            sortedPageList.append( "" )
    for page in pageList:
        try:
            sortedPageList[page.number-1] = page
        except IndexError as ie:
            print page.number
            print page.filename
            raise(IndexError)
        
    return sortedPageList

class Document:
	"""A text document consisting of multiple pages."""
	
	def __init__(self, name, parent):
		"""Initialize instance with the base name of the document and the name of the directory to be searched for pages of the document."""
		self.name = name
		self.parent = parent
		self.pages = self.getPages()
	
	def getPages(self):
		"""Return a list of all pages belonging to the document.
		For this, search for all files in parent that contain the document name."""
		pageList = set()
		for filename in os.listdir(self.parent):
			if (self.name in filename) and (filename.endswith(".txt")):
				pageList.add(self.Page(self, os.path.join(self.parent, filename)))
		return sortPages(pageList)
 
	class Page:
		"""Text document representing a single page of a document."""
		
		def __init__(self, document, filename):
			"""Initialize instance with a pointer to the document containing the page and the filename of the page."""
			self.document = document #pointer to the document containing the page
			self.filename = filename
			self.number = getPageNumber(filename)
			self.text = self.getText()
			
		def getText(self):
			"""Read the contents of the text file."""
			with open(self.filename, "r") as f:
				return f.read()
			
		def hasBibNumberRatio(self):
			"""Compute the ratio of numbers on page: a high number of numbers is 
			assumed to be typical for bibliographies as they contain many years, page numbers
			and dates."""
			numNumbers = float(len(re.findall("\d+", self.text)))
			numChars = len(self.text)
			#penalty for decimal numbers
			numDecimals = float(len(re.findall("\d+\.\d+", self.text)))
			return ((numNumbers / numChars) >= 0.01) and ((numNumbers / numChars) <= 0.1) and ((numDecimals / numChars) <= 0.004)
			
		def hasBibNumberRatio_c(self):
			"""Compute the ratio of numbers on page: a high number of numbers is 
			assumed to be typical for bibliographies as they contain many years, page numbers
			and dates.
			Cue words indicating the beginning of a reference section descreas the required ratio.
			"""
			numNumbers = float(len(re.findall("\d+", self.text)))
			numChars = len(self.text)
			#penalty for decimal numbers
			numDecimals = float(len(re.findall("\d+\.\d+", self.text)))
			#pages containing on of the cue words are accepted at lower thresholds (are preferred)
			if (self.cueWordContained() and ((numNumbers / numChars) >= 0.005) and ((numNumbers / numChars) <= 0.1) and ((numDecimals / numChars) <= 0.004)):
				return True
			elif ((numNumbers / numChars) >= 0.01) and ((numNumbers / numChars) <= 0.1): 
				return True
			else:
				return False
				
		def hasBibNumberRatio_d(self, checkStartPage = False):
			"""Compute the ratio of numbers on page: a high number of numbers is 
			assumed to be typical for bibliographies as they contain many years, page numbers
			and dates.
			Cue words indicating the beginning of a reference section decrease the required ratio.
			"""
			numNumbers = float(len(re.findall("\d+", self.text)))
			numChars = len(self.text)
			#penalty for decimal numbers
			numDecimals = float(len(re.findall("\d+\.\d+", self.text)))
			if checkStartPage:
				if (self.cueWordContained() and ((numNumbers / numChars) >= 0.005) and ((numNumbers / numChars) <= 0.1) and ((numDecimals / numChars) <= 0.004)):
					return True
				elif ((numNumbers / numChars) >= 0.01) and ((numNumbers / numChars) <= 0.1) and ((numDecimals / numChars) <= 0.004) and self.atEnd():
					return True
				else:
					return False
			else:
				return ((numNumbers / numChars) >= 0.008) and ((numNumbers / numChars) <= 0.1) and ((numDecimals / numChars) <= 0.004)

			
		def atEnd(self):
			"""Check whether page is at the end of the document or followed only by other bib pages."""
			if (self.number == len(self.document.pages)):
				return True
			else:
				#check whether next page is at the end and is a bib page
				#note: page1 is at pageList[0] -> next page is at pageList[self.number]
				nextPage = self.document.pages[self.number]
				return (nextPage.hasBibNumberRatio() and nextPage.atEnd())
			
		def cueWordContained(self):
			"""Check if any of the cue words is present in the text, return True if so, False if none of the cue words is found. 
			Search is case-sensitive.
			"""
			for cue in self.cueWords():
				if re.search(cue, self.text):
					return True
			return False
			
		def cueWords(self):
			"""Return a list of cue words indicating the beginning of a bibliography section."""
			return set(["Literatur", "Literaturverzeichnis", "Literaturliste", "Bibliographie", "Bibliografie", "Quellen", "Quellenangaben", "Quellenverzeichnis", "Literaturangaben", "Bibliography", "References", "list of literature", "List of Literature", "List of literature", "list of references", "List of References", "List of references", "reference list", "Reference List", "Reference list"])
			
		def isBibliography(self):
			"""Compute whether the page is likely to be a bibliography page or not. 
			For this, check whether the page is at the end of the document or only followed by other bib pages and if the ratio 
			of numbers relative to the ratio of characters is typical for a bib page."""
			if self.hasBibNumberRatio():
				if self.atEnd():
					return True
			return False
			
		def isBibliography_b(self):
		
			if not self.hasBibNumberRatio():
				return False
			else:
				return self.atEnd_b()
				
		#set another set of bibRatio values
		#accept pages with lower values only if cue word is present
		def isBibliography_c(self):
			if self.hasBibNumberRatio_c():
				if self.atEnd_b():
					return True
			return False
				
		def isBibliography_d(self):
			return self.hasBibNumberRatio_d()

		def startsBibliography_d(self):
			return self.hasBibNumberRatio_d(True)
			
		#inBibSection is true because this function is not called unless the current page has bibRatio
		#bibliography does not have to be at the end of the document (appendix may follow)
		#but there must not be more than one bibliography section - bib section must not be followed by another bib section
		#a bib section must be continuous (not be interrupted by a non-bib-section)
		def atEnd_b(self, inBibSection = True): 
			bibRatio = self.hasBibNumberRatio()
			if (self.number == len(self.document.pages)):
				if (bibRatio and inBibSection) or (not bibRatio and not inBibSection):
					return True
				return False
			else:
				nextPage = self.document.pages[self.number]
				if not bibRatio and inBibSection:
					inBibSection = False
					return nextPage.atEnd_b(inBibSection)
				
				elif not bibRatio and not inBibSection:
					return nextPage.atEnd_b(inBibSection)
					
				elif bibRatio and inBibSection:
					return nextPage.atEnd_b(inBibSection)
				
				elif bibRatio and not inBibSection:
					return False

					
				
			
		
def getBibliographies(path):
	documentNames = set()
	bibPages = []
	for filename in os.listdir(path):
		documentNames.add(getDocumentName(filename))

	for docName in documentNames:
		for page in Document(docName, path).pages:
			if page.isBibliography_c():
				bibPages.append(page)
	return bibPages
	
	
def getBibliographies_d(path):
    documentNames = set()
    bibPages = []
    for filename in os.listdir(path):
        documentNames.add(getDocumentName(filename))
        print "added documents for filename \"%s\" to list." %filename

    for docName in documentNames:
        bibSecStarted = False
        for page in Document(docName, path).pages:
            if page == "":
                continue

            if not bibSecStarted:
                #first page of bib has not been found
                if page.startsBibliography_d() and page.atEnd_b():
                    bibPages.append(page)
                    bibSecStarted = True
                    #found first bib page
                    #set flag to apply bonus to all following pages

            else:
                if page.isBibliography_d():
                    #first bib page was previously found and this page belongs to it
                    bibPages.append(page)
                else:
                    #bibliography was found before, this page ends it (e.g. appendix)
                    break
    return bibPages


def getBibPages(path, outFile):
    bibpages = []
    with open(outFile, "w") as f:
        for page in getBibliographies_d(path):
            f.write(page.filename + "\n")
            bibpages.append(page.filename)
            #moveBibPage(page, path)
            #print "Moved %s to bibPages" %page
    print "Wrote %s." %outFile
    return bibpages
    
def combinePages(path, suspectedBibs, pathOut):
    """Merge all pages of a document and save to pathOut."""
    documentNames = set()
    #find all distinct documents with all corresponding pages
    for filename in os.listdir(path):
        documentNames.add(getDocumentName(filename))
        print "added documents for filename \"%s\" to list." %filename
    #merge the text of all pages
    for docName in documentNames:
        content = ""
        for page in Document(docName, path).pages:
            #ignore content of bib pages
            if page.filename not in suspectedBibs:
                content += page.text
        #write text to file
        with open(os.path.join(pathOut, docName +".txt"), "w") as f:
            f.write(content)
            print "Wrote %s." %os.path.join(pathOut, docName +".txt")
    
def makeBiblessDocs(path, outFile, pathBiblessDocs):
    bibpages = getBibPages(path, outFile)
    combinePages(path, bibpages, pathBiblessDocs)