#!python

import sys
import io
import re
import urllib
import urllib2
import urlparse
import lxml.etree

def get_outlinks(url):
  '''
  url: the url to the page to crawl
  '''
  result = []
  if url is None:
    return result

  html = None
  resp = None
  try:
    url = url.strip()
    resp = urllib2.urlopen(url)
    if resp.code == 200:
      html = resp.read()
  except (urllib2.URLError, Exception) as e:
    print "can't access {0}: {1}".format(url, e)
  finally:
    if resp is not None:
      resp.close()

  if html is None:
    return result
  html_parser = lxml.etree.HTMLParser()
  try:
    uhtml = html.decode('utf-8', 'ignore')
    tree = lxml.etree.parse(io.StringIO(uhtml), html_parser)
    anchors = tree.xpath('//a')
    for anchor in anchors:
      href = anchor.attrib.get('href', None)
      if href is not None:
        href = href.strip()
        dest = urlparse.urljoin(url, href)
        if dest.startswith('http://'):
          result.append(dest)
  except Exception as e:
    print "can't parse {0}: {1}".format(url, e)

  return result

def crawl(urls,
          max_to_handle,
          handle_url,
          crawl_test = None,
          handle_test = None):
  handled = []
  visited = set()
  i = 0
  p = 0
  while len(handled) < max_to_handle and i < len(urls):
    url = urls[i]
    if url not in visited and crawl_test(url):
      outlinks = get_outlinks(url)
      visited.add(url)
      urls.extend(outlinks)
    if handle_test(url) and url not in handled:
      handle_url(url, p + 1, max_to_handle)
      handled.append(url)
      p += 1
    i += 1
  return handled

def call_semantics_service(url, i, max_to_handle):
  service_pattern = "http://ecology-service.cse.tamu.edu/BigSemanticsService/metadata.xml?url={0}"
  qurl = urllib.quote(url)
  surl = service_pattern.format(qurl)
  resp = urllib2.urlopen(surl)
  content = resp.read()
  is_downloaded = content.find('DOWNLOAD_DONE') >= 0
  is_typed = content.find('</amazon_product>') >= 0
  if resp.code == 200 and is_downloaded and is_typed:
    print "[{0}/{1}] service called on {2}".format(
            i, max_to_handle, url)
  else:
    print "[{0}/{1}] error calling service: {2}: c={3}, d={4}, t={5}".format(
            i, max_to_handle, surl, resp.code, is_downloaded, is_typed)

def call_downloader_service(url, i, max_to_handle):
  agent = "Mozilla%2F5.0%20(Windows%20NT%206.2%3B%20Win64%3B%20x64)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F32.0.1667.0%20Safari%2F537.36"
  service_pattern = "http://ecology-service.cse.tamu.edu/DownloaderPool/page/download.xml?url={0}&agent={1}"
  qurl = urllib.quote(url)
  resp = urllib2.urlopen(service_pattern.format(qurl, agent))
  if resp.code == 200:
    print "[{0}/{1}] successful downloading invocation on {2}".format(
            i, max_to_handle, url)
  else:
    print "[{0}/{1}] downloading error code {2} for {3}".format(
            i, max_to_handle, resp.code, url)

if __name__ == '__main__':
  if len(sys.argv) < 3:
    print "usage: {0} <url_lst_file> <max_to_handle>".format(sys.argv[0])
    quit()

  f = open(sys.argv[1])
  urls = f.readlines()
  n = int(sys.argv[2])
  crawl_test = lambda(url): url.find('amazon.com') > 0;
  p_prod = r'^http://www.amazon.com/([^/]+/)?dp/[^/]+';
  handle_test = lambda(url): re.search(p_prod, url) is not None;
  handled = crawl(urls, n, call_semantics_service, crawl_test, handle_test);
  for url in handled:
    print url

