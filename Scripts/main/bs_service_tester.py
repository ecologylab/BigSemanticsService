#!/usr/bin/python

import urllib2
import urllib
from simple_config import load_config

tester_config = load_config("tester.conf")

class ServiceTester:
  def __init__(self, config=None):
    if config is None:
      config = tester_config
    self.config = config
    self.timeout_seconds = config["timeout_seconds"]
    self.tests = config["tests"]

  def test_service(self):
    code = -1
    fatal = ""
    non_fatal = ""

    for t in self.tests:
      mmd = t.get("mmd")
      doc = t.get("doc")
      path = t.get("path")
      include = t.get("include")

      url = self.get_test_url(mmd, doc, path)

      (code, content, error) = self.access_and_download(url)
      if code < 0:
        error_msg = "(no error message available)"
        if error is not None:
          error = str(error)
        fatal = "Access error when trying {}:\n{}".format(url, error)
      elif code != 200:
        fatal = "Service error when trying {}, HTTP code: {}".format(url, code)
      else:
        if content.find(include) < 0:
          non_fatal +=\
            "Expected content not found for {}: {}\n".format(url, include)

    return (code, fatal, non_fatal)

  def get_test_url(self, mmd, doc, path):
    base_url = "http://" + self.config["service_host"]
    if mmd is not None:
      return base_url + "/BigSemanticsService/mmd.xml?name=" + mmd
    elif doc is not None:
      params = urllib.urlencode({"reload": "true", "url": doc})
      return base_url + "/BigSemanticsService/metadata.xml?" + params
    elif path is not None:
      return base_url + path
    else:
      return None

  def access_and_download(self, url):
    f = None
    code = -1
    content = None
    error = None
    try:
      f = urllib2.urlopen(url, None, self.timeout_seconds)
      code = f.getcode()
      content = u'\n'.join(f.readlines())
    except Exception as e:
      code = -1
      error = e
    finally:
      if f is not None:
        f.close()
    return (code, content, error)

