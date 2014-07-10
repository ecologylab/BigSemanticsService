# Providing functionality of forking new processes.

import exceptions
import os
from subprocess import Popen, PIPE, STDOUT

DEVNULL = open(os.devnull, "w")

def fork(cmds, wd = None):
  wd_name = wd if wd is not None else os.getcwd()
  print wd_name + " (fork) $ " + " ".join(cmds)
  p = Popen(cmds, stderr=STDOUT, stdout=DEVNULL, cwd = wd)

def call(cmds, wd = None):
  wd_name = wd if wd is not None else os.getcwd()
  print wd_name + " (call) $ " + " ".join(cmds)
  p = Popen(cmds, stdout=PIPE, stderr=PIPE, cwd = wd)
  (out, err) = p.communicate()
  return (p.returncode, out, err)

def check_call(cmds, wd = None):
  (code, out, err) = call(cmds, wd)
  if code != 0:
    wd_name = wd if wd is not None else os.getcwd()
    raise exceptions.RuntimeError(
            "Failed to execute {}\n  in {}\n  OUT:\n{}\n  ERROR:\n{}\n".format(
              cmds, wd_name, out, err))

