from __future__ import division
from difflib import SequenceMatcher
import json
import copy
import string
import sys
import urllib
import unicodedata

outfile = open('status.txt', 'w+')

def prettyPrint(jsonObj):
    print json.dumps(jsonObj, sort_keys=True, indent=4, separators=(',', ': '))

def linePrint(str, url):
    print str + " " * (50 - len(str)) + url
    outfile.write(str + " " * (50 - len(str)) + url + '\n')

#if run time is an issue some of these checks can be commented out
def sanitizedStrCheck(str1, str2, location):
    if (isinstance(str1, str) and isinstance(str2, str)) or (isinstance(str1, unicode) and isinstance(str2, unicode)):
        m = SequenceMatcher(None, str1, str2)
        rat = m.ratio()
        str1 = str1.replace("\n", " ")
        str2 = str2.replace("\n", " ")
        if ''.join(filter(lambda c: c in string.printable, str1)) == ''.join(filter(lambda c: c in string.printable, str2)):
            return True
        elif str1.strip() == str2.strip():
            return True
        elif unicodedata.normalize('NFKD', str1).encode('ascii','ignore') == unicodedata.normalize('NFKD', str2).encode('ascii','ignore'):
            return True
        elif str1.replace(" ", "") == str2.replace(" ", ""):
            return True
        elif rat > .8 and not str1.__contains__('http://'):
            #print rat
            #print "SERVER:"
            #print repr(str1)
            #print "CLIENT:"
            #print repr(str2)
            return True
        elif str1.__contains__('http://') and str2.__contains__('http://') and urllib.quote_plus(str2.replace(" ", "%20")) == urllib.quote_plus(str1):
            #print str1
            #print str2
            #print urllib.quote_plus(str1)[:150]
            #print urllib.quote_plus(str2.replace(" ", "%20"))[:150]
            return True
    return False


nestedMissing = {}

def nestedCheck(serv, client, parentLocation):
    global nestedCoCounter
    nestedMatchCount = 0

    if serv == client:
        return True
    if isinstance(serv, dict) and isinstance(client, dict):
        for k in serv:
            if k == "meta_metadata_name":
                nestedMatchCount += 1
            elif k in client and (serv[k] == client[k] or nestedCheck(serv[k], client[k], "")):
                nestedMatchCount += 1
            elif k not in client:
                nestedMissing[k + " not in composite of"] = parentLocation
        if len(serv.keys()) == nestedMatchCount:
            return True
    elif isinstance(serv, list) and isinstance(client, list):
        listItemMatchCount = 0
        for i in range(0, len(serv)):
            nestedMatchCount = 0
            if isinstance(serv[i], dict):
                for k in serv[i]:
                    if k == "download_status":
                        nestedMatchCount += 1
                    elif k == "meta_metadata_name":
                        nestedMatchCount += 1
                    elif i < len(client) and k in client[i]:
                        if sanitizedStrCheck(serv[i][k], client[i][k], "hodor") or nestedCheck(serv[i][k], client[i][k], parentLocation):
                            nestedMatchCount += 1
                    elif i < len(client):
                        #if k == "price":
                        #    prettyPrint(serv[i])
                        #    prettyPrint(client[i])
                        nestedMissing[k + " not in collection of"] = parentLocation
                if len(serv[i].keys()) <= nestedMatchCount:
                    listItemMatchCount += 1

        if listItemMatchCount == len(serv):
            return True
        #to account for minor mismatches, such as character encodings, if more than half match we call it good
        elif listItemMatchCount/len(serv) > .5:
            return True
    return False

fileS = open('jsonServiceResponse.txt', 'r')
fileC = open('jsonClientSide.txt', 'r')

servList = []
clientList = []

firstLine = True

for line in fileS:
    if len(line)>1:
        servList.append(json.loads(line))

firstLine = True

for line in fileC:
    if len(line)>1:
        clientList.append(json.loads(line))

#sort the collections so we can iterate and compare faster
print "Sorting Server Collections"
for metadataServ in servList:
    curKey = metadataServ.keys()[0]
    for k in metadataServ[curKey]:
        if isinstance(metadataServ[curKey][k], list):
            metadataServ[curKey][k].sort()

print "Sorting Client Collections"
for metadataCLient in clientList:
    curKey = metadataCLient.keys()[0]
    for k in metadataCLient[curKey]:
        if isinstance(metadataCLient[curKey][k], list):
            metadataCLient[curKey][k].sort()

totalCount = 0
totalFields = 0

servList2 = copy.deepcopy(servList)

#remove metametadata name, log record, and additional locations since we can't expect the plugin to get those
for x in range(0, len(servList2)):
    curKey = servList2[x].keys()[0]
    for k in servList2[x][curKey]:
        if k == "meta_metadata_name":
            servList[x][curKey].pop(k, None)
        elif k == "log_record":
            servList[x][curKey].pop(k, None)
        elif k == "additional_locations":
            servList[x][curKey].pop(k, None)
        for k3 in servList2[x][curKey][k]:
            if k3 == "meta_metadata_name":
                servList[x][curKey][k].pop(k3, None)
        if isinstance(servList2[x][curKey][k], list):
            for y in range(0, len(servList2[x][curKey][k])):
                for k2 in servList2[x][curKey][k][y]:
                    if k2 == "meta_metadata_name":
                        servList[x][curKey][k][y].pop(k2, None)

servList2 = copy.deepcopy(servList)
clientList2 = copy.deepcopy(clientList)

#don't print mm_name
print "About to delete mm_name"
for x in range(0, len(clientList)):
    curKey = clientList[x].keys()[0]
    for k in clientList[x][curKey]:
        if k == "mm_name":
            clientList2[x][curKey].pop(k, None)
        if not isinstance(clientList[x][curKey][k], dict):
            for y in range(0, len(clientList[x][curKey][k])):
                for k2 in clientList[x][curKey][k][y]:
                    if k2 == "mm_name":
                        clientList2[x][curKey][k][y].pop(k2, None)
        if isinstance(clientList[x][curKey][k], dict):
            for y in clientList[x][curKey][k]:
                if y == "mm_name":
                    clientList2[x][curKey][k].pop(y, None)
#don't print equal fields. leave location since it is used for matching
print "About to delete equal fields"
for x in range(0, len(clientList)):
    curKey = clientList[x].keys()[0]
    for y in range(0, len(servList)):
        for k in clientList[x][curKey]:
            if curKey in servList[y]:
                for k in servList[y][curKey]:
                    if k == 'location':
                        continue
                    #if the service has a location in a favicon or home_page that is simply the same as document location don't count it.
                    if k == 'favicon' or k == 'home_page' and 'location' in servList[y][curKey][k] and 'location' in servList[y][curKey] and servList[y][curKey][k]['location'] == servList[y][curKey]['location']:
                        #prettyPrint(servList[y][curKey][k])
                        servList2[y][curKey].pop(k, None)
                        #print "POPPING"
                    if k in clientList2[x][curKey] and k in servList2[y][curKey]:
                        if clientList2[x][curKey][k] == servList2[y][curKey][k] or nestedCheck(servList2[y][curKey][k], clientList2[x][curKey][k], servList[y][curKey]['location']) or sanitizedStrCheck(servList2[y][curKey][k], clientList2[x][curKey][k], servList[y][curKey]['location']):
                            # "deleting " + str(k) + " ============================================= from " + curKey
                            servList2[y][curKey].pop(k, None)
                            clientList2[x][curKey].pop(k, None)



#pretty print some so we know what doesn't match
i = 0
for metadataServ in servList2:
    break
    for metadataClient in clientList2:
        curKey = metadataServ.keys()[0]
        if curKey in metadataClient.keys():
            if metadataServ[curKey]['location'] == metadataClient[curKey]['location'] and metadataClient[curKey]['location'] == "http://www.amazon.com/Twilight-Saga-Breaking-Two-Disc-Special/dp/B002BWP49C":
                print "SERVER"
                prettyPrint(metadataServ)
                print "CLIENT"
                prettyPrint(metadataClient)
    i+=1

nestedCounter = 0
mismatch = False

mmdStatusObjects = []

print "\n===================Cases where server gets a field that client doesn't have at all\n"
outfile.write("\n===================Cases where server gets a field that client doesn't have at all\n\n")
for metadataServ in servList:
    for metadataClient in clientList:
        curKey = metadataServ.keys()[0]
        if curKey in metadataClient.keys() and metadataServ[curKey]['location'] == metadataClient[curKey]['location']:
            matchCount = 0
            curFields = len(metadataServ[curKey])
            mismatch = False
            for k in metadataServ[curKey]:
                if k == 'favicon' or k == 'home_page' and 'location' in metadataServ[curKey][k] and 'location' in metadataServ[curKey] and metadataServ[curKey][k]['location'] == metadataServ[curKey]['location']:
                    matchCount += 1
                elif k in metadataClient[curKey]:
                    if metadataServ[curKey][k] == metadataClient[curKey][k]:
                        matchCount += 1
                    #check composites and collections
                    elif isinstance(metadataServ[curKey][k], dict) or isinstance(metadataServ[curKey][k], list):
                        if nestedCheck(metadataServ[curKey][k], metadataClient[curKey][k], metadataServ[curKey]['location']):
                            matchCount += 1
                        else:
                            nestedCounter += 1
                    elif sanitizedStrCheck(metadataServ[curKey][k], metadataClient[curKey][k], metadataServ[curKey]['location']):
                        matchCount += 1
                else:
                    linePrint(k + " not in " + curKey, metadataServ[curKey]['location'])
            #print str(matchCount) + " out of " + str(curFields)+ " for " + str(curKey)
            mmdStatusObjects.append({'matches': matchCount, 'possible': curFields, 'misses': curFields-matchCount, 'type': curKey, 'url': metadataServ[curKey]['location']})
            totalCount += matchCount
            totalFields += curFields

print " "
outfile.write('\n')
for k in nestedMissing:
    linePrint(k, nestedMissing[k])

print "\n===================Status of each document with mismatches\n"
outfile.write("\n===================Status of each document with mismatches\n\n")
newlist = sorted(mmdStatusObjects, key=lambda k: k['misses'], reverse=True)
for object in newlist:
    if object['misses'] > 0:
        linePrint(str(object['matches']) + " out of " + str(object['possible']) + " for " + str(object['type']), str(object['url']))

documentMatches = 0
for object in newlist:
    if object['misses'] == 0:
        documentMatches+=1

print str(documentMatches) + " out of " + str(len(newlist)) + " total documents matched\n "
outfile.write("\n" + str(documentMatches) + " out of " + str(len(newlist)) + " total documents matched\n")

print str(totalCount) + " out of " + str(totalFields)+ " total fields matched"
outfile.write(str(totalCount) + " out of " + str(totalFields)+ " total fields matched")
#print str(nestedCounter) + " were composite/collection mismatches"

fileS.close()
fileC.close()