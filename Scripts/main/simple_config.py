#!/usr/bin/python

import os.path

def load_config(config_file):
  bss_dir = os.getenv("BSS_DIR")
  if bss_dir is None:
    bss_dir = os.path.join(os.path.expanduser("~"), "bigsemantics-service")

  fpath = os.path.join(bss_dir, "config", config_file)
  config = {}
  execfile(fpath, config)
  return config

