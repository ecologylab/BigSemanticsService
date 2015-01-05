#!/usr/bin/python

from downloaders import downloader_config, Downloaders
from bot_email import send_bot_email_to_maintainers

nodes = downloader_config["downloaders"]
ds = Downloaders()
i = 0
code_out_errs = ds.do("ps -C java -o command --no-headers")
for (code, out, err) in code_out_errs:
  node = nodes[i]
  if code == 0 and out.startswith("java ") and out.find("Downloader.jar") >= 0:
    print "Running: " + node.host + ": " + out
  else:
    print "Not running: " + node.host
    run_on_host = lambda h: h == node.host
    (ncode, nout, nerr) = ds.run_downloaders(run_on_host)[0]
    if ncode != 0:
      print "Sending failure notification for " + node.host
      send_bot_email_to_maintainers(
        "Failed to relaunch downloader: " + node.host,
        "Output:\n" + nout + "\nError:\n" + nerr)
  i += 1

