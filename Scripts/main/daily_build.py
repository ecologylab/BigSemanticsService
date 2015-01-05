#!/usr/bin/python

import exceptions
import shutil
import time
import datetime
from copy import copy
from os import listdir, remove
from os.path import dirname, join, expanduser

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

    self.bsjava_repo = join(self.bs_repo, "BigSemanticsJava")

    self.service_repo = join(self.bs_repo, "BigSemanticsService")
    self.service_proj = join(self.service_repo, "BigSemanticsService")
    self.service_build = join(self.service_proj, "build")
    self.dpool_proj = join(self.service_repo, "DownloaderPool")

    self.wrapper_repo = join(self.bs_repo, "BigSemanticsWrapperRepository")
    self.wrapper_proj = join(self.wrapper_repo, "BigSemanticsWrappers")
    self.onto_vis_dir = join(self.wrapper_proj, "OntoViz")

    self.bsjs_repo = join(self.bs_repo, "BigSemanticsJavaScript")

    # deployment related paths:
    self.jetty_dir = join(self.bs_base_dir, "jetty-dist")
    self.webapps_dir = join(self.jetty_dir, "webapps")
    self.downloader_dir = join(self.bs_base_dir, "downloader")
    self.archive_dir = join(self.bs_base_dir, "archives")
    self.prod_webapps_dir = config["prod_webapps_dir"]
    self.prod_downloader_dir = config["prod_downloader_dir"]
    self.prod_static_dir = config["prod_static_dir"]

    # data:
    self.example_table_script = config["example_table_script"]
    self.example_table_data_file = config["example_table_data_file"]
    self.max_archives = config["max_archives"]
    self.prod_host = config["prod_host"]
    self.prod_user = config["prod_user"]
    self.prod_login_id = join(self.home_dir, config["prod_login_id"])

  def git_update_to_latest(self, git_dir):
    # clean local changes
    check_call(["git", "checkout", "--", "*"], wd=git_dir)
    check_call(["git", "clean", "-f"], wd=git_dir)
    check_call(["git", "clean", "-f", "-d"], wd=git_dir)
    # pull down latest code
    check_call(["git", "pull"], wd=git_dir)
    # update submodules
    check_call(["git", "submodule", "foreach", "git checkout -- *"], wd=git_dir)
    check_call(["git", "submodule", "foreach", "git clean -f"], wd=git_dir)
    check_call(["git", "submodule", "foreach", "git clean -f -d"], wd=git_dir)
    check_call(["git", "submodule", "update"], wd=git_dir)

  def update_projs(self):
    self.git_update_to_latest(self.bs_repo)

  def compile_projs(self):
    # compile service
    check_call(["ant", "clean"], wd=self.service_proj)
    check_call(["ant", "main"], wd=self.service_proj)
    shutil.copy2(join(self.service_build, "BigSemanticsService.war"),
                 self.webapps_dir)
    # compile dpool service and downloader
    check_call(["ant", "clean"], wd=self.dpool_proj)
    check_call(["ant", "main"], wd=self.dpool_proj)
    shutil.copy2(join(self.dpool_proj, "build", "DownloaderPool.war"),
                 self.webapps_dir)
    shutil.copy2(join(self.dpool_proj, "build", "Downloader.jar"),
                 self.downloader_dir)

  def start_local_service(self):
    fork(["killall", "java"], wd=self.jetty_dir)
    time.sleep(3)
    fork(["nohup", "java", "-server", "-jar", "start.jar"], wd=self.jetty_dir)
    time.sleep(30)
    fork(["nohup", "java", "-server", "-Xms128m", "-Xmx256m", "-jar",
          "Downloader.jar"], wd=self.downloader_dir)
    time.sleep(5)

  def test_local_service_and_release(self):
    local_tester_config = copy(bs_service_tester.tester_config)
    local_tester_config["service_host"] = "localhost:8080"
    service_tester = bs_service_tester.ServiceTester(local_tester_config)
    (code, fatal, non_fatal) = service_tester.test_service()
    if code != 200 or len(fatal) > 0 or len(non_fatal) > 0:
      raise exceptions.RuntimeError(
          "Build broken:\n  code: {}\n  fatal: {}\n  non_fatal: {}".format(
              code, fatal, non_fatal))
    else:
      self.archive_bins()
      self.release_to_prod()
      print "archived and released.\n"

  def archive(self, file_dir, file_name):
    f = join(file_dir, file_name)
    tag = datetime.datetime.now().strftime(".%Y%m%d%H")
    dest_file = join(self.archive_dir, file_name + tag)
    shutil.copyfile(f, dest_file)
    files = listdir(self.archive_dir)
    archives = [f for f in files if f.startswith(file_name + ".")]
    if len(archives) > self.max_archives:
      archives = sorted(archives)
      remove(join(self.archive_dir, archives[0]))

  def archive_bins(self):
    self.archive(self.webapps_dir, "BigSemanticsService.war")
    self.archive(self.webapps_dir, "DownloaderPool.war")
    self.archive(self.downloader_dir, "Downloader.jar")

  def release_file_to_prod(self, file_name, local_dir, remote_dir):
    dest_spec = "{0}@{1}:{2}".format(self.prod_user,
                                     self.prod_host,
                                     remote_dir)
    cmds = ["scp", "-i", self.prod_login_id, file_name, dest_spec]
    check_call(cmds, wd = local_dir)

  def release_to_prod(self):
    # copy the war files
    self.release_file_to_prod("BigSemanticsService.war",
                              self.webapps_dir,
                              self.prod_webapps_dir)
    self.release_file_to_prod("DownloaderPool.war",
                              self.webapps_dir,
                              self.prod_webapps_dir)
    self.release_file_to_prod("Downloader.jar",
                              self.downloader_dir,
                              self.prod_downloader_dir)

    # copy the generated visualization file
    self.release_file_to_prod("mmd_repo.json",
                              self.onto_vis_dir,
                              self.prod_static_dir)

    # generate and copy the example table data file
    cmds = ["python", self.example_table_script,
            "--out", self.example_table_data_file]
    check_call(cmds, wd = self.onto_vis_dir)
    self.release_file_to_prod(self.example_table_data_file,
                              self.onto_vis_dir,
                              self.prod_static_dir)



if __name__ == "__main__":
  builder = ServiceBuilder()
  try:
    builder.update_projs()
    builder.compile_projs()
    builder.start_local_service()
    builder.test_local_service_and_release()
    print "everything done."
  except Exception as e:
    import sys
    sys.stderr.write("dev build failed! see email notification.")
    send_bot_email_to_maintainers("Dev build failed.", str(e))

