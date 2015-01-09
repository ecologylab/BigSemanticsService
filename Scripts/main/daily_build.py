#!/usr/bin/python

import exceptions
import shutil
import time
import datetime
from copy import copy
from os import listdir, remove
from os.path import dirname, join, expanduser, isfile

from simple_config import load_config
from bot_email import send_bot_email_to_maintainers
from forker import fork, call, check_call
import bs_service_tester

builder_config = load_config("builder.conf")

class ServiceBuilder:
  def __init__(self, config=None):
    if config is None:
      config = builder_config
    self.config = config

    # code paths:
    self.home_dir = expanduser('~')
    self.code_dir = join(self.home_dir, config["code_dir"])
    self.bs_base_dir = join(self.home_dir, config["bs_base_dir"])
    
    # bs code paths:
    self.bs_repo = join(self.code_dir, "BigSemantics")
    self.service_proj = join(self.bs_repo, "BigSemanticsService", "BigSemanticsService")
    self.service_build = join(self.service_proj, "build")
    self.onto_vis_dir = join(self.bs_repo, "BigSemanticsWrapperRepository",
                             "BigSemanticsWrappers", "OntoViz")
    self.bsjs_repo = join(self.bs_repo, "BigSemanticsJavaScript")

    # deployment related paths:
    self.deploy_dir = join(self.bs_base_dir, "deploy")
    self.static_dir = join(self.deploy_dir, "static")
    self.archive_dir = join(self.bs_base_dir, "archives")
    self.prod_deploy_dir = config["prod_deploy_dir"]
    self.prod_static_dir = config["prod_static_dir"]

    # data:
    self.example_table_script = config["example_table_script"]
    self.example_table_data_file = config["example_table_data_file"]
    self.max_archives = config["max_archives"]
    self.prod_host = config["prod_host"]
    self.prod_user = config["prod_user"]
    self.prod_login_id = join(self.home_dir, config["prod_login_id"])

  def git_update_to_latest(self, git_dir):
    # update submodules, if any
    if isfile(join(git_dir, ".gitmodules")):
      check_call(["git", "submodule", "foreach", "git", "checkout", "--", "*"], wd=git_dir)
      check_call(["git", "submodule", "foreach", "git", "clean", "-f"], wd=git_dir)
      check_call(["git", "submodule", "foreach", "git", "clean", "-f", "-d"], wd=git_dir)
      check_call(["git", "submodule", "foreach", "git", "fetch"], wd=git_dir)
    # clean local changes
    check_call(["git", "checkout", "--", "*"], wd=git_dir)
    check_call(["git", "clean", "-f"], wd=git_dir)
    check_call(["git", "clean", "-f", "-d"], wd=git_dir)
    # pull down latest code
    check_call(["git", "pull"], wd=git_dir)
    check_call(["git", "submodule", "update"], wd=git_dir)

  def copy_file(self, fname, src_dir, dest_dir,
                new_fname = None,
                remote_host = None,
                remote_user = None,
                remote_login = None):
      f0 = join(src_dir, fname)
      f1 = join(dest_dir, fname if new_fname is None else new_fname)
      if remote_host is None and remote_user is None and remote_login is None:
        shutil.copyfile(f0, f1)
        print "copied {0} to {1}".format(f0, f1)
      else:
        dest_spec = "{0}@{1}:{2}".format(remote_user, remote_host, f1)
        cmds = ["scp", "-i", remote_login, f0, dest_spec]
        check_call(cmds, wd = dest_dir)

  def archive(self, file_dir, file_name):
    f = join(file_dir, file_name)
    tag = datetime.datetime.now().strftime(".%Y%m%d%H")
    archive_name = file_name + tag
    print f, archive_name
    self.copy_file(file_name, file_dir, self.archive_dir, archive_name)

    # remove old archives:
    files = listdir(self.archive_dir)
    archives = [f for f in files if f.startswith(file_name + ".")]
    if len(archives) > self.max_archives:
      archives = sorted(archives)
      remove(join(self.archive_dir, archives[0]))

  def update_projs(self):
    self.git_update_to_latest(self.bs_repo)

  def compile_projs(self):
    # compile service
    call(["find", ".", "-name", "build", "-exec", "rm", "-rf", "{}", ";"],
         wd=self.bs_repo)
    check_call(["ant", "clean"], wd=self.service_proj)
    check_call(["ant", "main"], wd=self.service_proj)
    cmds = ["python", self.example_table_script,
            "--out", self.example_table_data_file]
    check_call(cmds, wd = self.onto_vis_dir)

  def release_service(self, deploy_dir, host = None, user = None, login = None):
    self.copy_file("BigSemanticsService.jar", self.service_build, deploy_dir,
                   None, host, user, login)
    static_dir = join(deploy_dir, "static")
    self.copy_file("mmd_repo.json", self.onto_vis_dir, static_dir,
                   None, host, user, login)
    self.copy_file(self.example_table_data_file, self.onto_vis_dir, static_dir,
                   None, host, user, login)

  def start_local_service(self):
    self.release_service(self.deploy_dir)

    fork(["killall", "java"], wd=self.deploy_dir)
    time.sleep(5)
    fork(["nohup", "java", "-server", "-jar", "BigSemanticsService.jar"],
          wd=self.deploy_dir)
    time.sleep(5)

  def archive_local_service(self):
    self.archive(self.service_build, "BigSemanticsService.jar")

  def test_local_service_and_archive(self):
    local_tester_config = copy(bs_service_tester.tester_config)
    local_tester_config["service_host"] = "localhost:8080"
    service_tester = bs_service_tester.ServiceTester(local_tester_config)
    (code, fatal, non_fatal) = service_tester.test_service()
    if code != 200 or len(fatal) > 0 or len(non_fatal) > 0:
      raise exceptions.RuntimeError(
          "Build broken:\n  code: {}\n  fatal: {}\n  non_fatal: {}".format(
              code, fatal, non_fatal))
    else:
      self.archive_local_service()
      print "tested and archived.\n"

  def release_to_prod(self):
    self.release_service(self.prod_deploy_dir,
                         self.prod_host, self.prod_user, self.prod_login_id)



if __name__ == "__main__":
  builder = ServiceBuilder()
  try:
    builder.update_projs()
    builder.compile_projs()
    builder.start_local_service()
    builder.test_local_service_and_archive()
    builder.release_to_prod()
    print "everything done."
  except Exception as e:
    msg = str(e)
    print "Error:", msg
    import sys
    sys.stderr.write("dev build failed! see email notification.")
    send_bot_email_to_maintainers("Dev build failed.", msg)

