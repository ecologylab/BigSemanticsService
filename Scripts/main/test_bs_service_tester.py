#!/usr/bin/python

import unittest
from bs_service_tester import ServiceTester

class TestServiceTester(unittest.TestCase):
  def test_normal_state(self):
    tests = [
      {
        "doc": "http://example.com/",
        "include": "example include",
      },
    ]
    resp = (200, "This is an example include!", None)
    srv_tester = ServiceTester({"service_host": "localhost", "tests": tests})
    srv_tester.access_and_download = lambda url: resp
    (code, fatal, non_fatal) = srv_tester.test_service()
    self.assertEqual(200, code)
    self.assertEqual(0, len(fatal))
    self.assertEqual(0, len(non_fatal))

  def test_connection_failure(self):
    tests = [
      {
        "doc": "http://example.com/",
        "include": "example include",
      },
    ]
    resp = (-1, None, None)
    srv_tester = ServiceTester({"service_host": "localhost", "tests": tests})
    srv_tester.access_and_download = lambda url: resp
    (code, fatal, non_fatal) = srv_tester.test_service()
    self.assertTrue(code < 0)
    self.assertTrue(fatal.find("Access error") >= 0)
    self.assertTrue(fatal.find("example.com") >= 0)

  def test_service_failure(self):
    tests = [
      {
        "doc": "http://example.com/",
        "include": "example include",
      },
    ]
    resp = (500, "Service unavailable.", None)
    srv_tester = ServiceTester({"service_host": "localhost", "tests": tests})
    srv_tester.access_and_download = lambda url: resp
    (code, fatal, non_fatal) = srv_tester.test_service()
    self.assertNotEqual(200, code)
    self.assertTrue(fatal.find("Service error") >= 0)
    self.assertTrue(fatal.find("example.com") >= 0)

  def test_include_error(self):
    url1 = "http://example.com"
    url2 = "http://example2.com"
    url3 = "http://example3.com"
    tests = [
      {
        "doc": url1,
        "include": "example include 1",
      },
      {
        "doc": url2,
        "include": "example include 2",
      },
      {
        "doc": url3,
        "include": "example include 3",
      },
    ]
    resp1 = (200, "This is an example incl", None)
    resp2 = (200, "This is an example include 2.", None)
    resp3 = (200, "something irrelevant", None)
    resps = { url1: resp1, url2: resp2, url3: resp3 }
    srv_tester = ServiceTester({"service_host": "localhost", "tests": tests})
    srv_tester.get_test_url = lambda mmd, doc, path: doc
    srv_tester.access_and_download = lambda url: resps[url]
    (code, fatal, non_fatal) = srv_tester.test_service()
    self.assertEqual(0, len(fatal))
    self.assertTrue(non_fatal.find("Expected content not found") >= 0)
    self.assertTrue(non_fatal.find(url1) >= 0)
    self.assertTrue(non_fatal.find(url2) < 0)
    self.assertTrue(non_fatal.find(url3) >= 0)



if __name__ == '__main__':
  unittest.main()

