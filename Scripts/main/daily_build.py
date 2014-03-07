#!/usr/bin/python

import exceptions
import shutil
import time
import datetime
from copy import copy
from os import listdir, remove
from os.path import dirname, join

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
    # paths:
    self.code_dir = config["code_dir"]
    self.wrapper_repo = join(self.code_dir, "BigSemanticsWrapperRepository")
    self.wrapper_proj = join(self.wrapper_repo, "BigSemanticsWrappers")
    self.service_repo = join(self.code_dir, "BigSemanticsService")
    self.service_proj = join(self.service_repo, "BigSemanticsService")
    self.service_build = join(self.service_proj, "build")
    self.jetty_dir = config["jetty_dir"]
    self.webapps_dir = join(self.jetty_dir, "webapps")
    self.downloader_dir = config["downloader_dir"]
    self.war_archive_dir = config["war_archive_dir"]
    self.prod_login_id = config["prod_login_id"]
    self.prod_webapps_dir = config["prod_webapps_dir"]
    # data:
    self.max_war_archives = config["max_war_archives"]
    self.prod_user = config["prod_user"]
    self.prod_host = config["prod_host"]

  def pull_wrappers(self):
    # clean local wrapper changes
    check_call(["git", "checkout", "--", "*"], wd=self.wrapper_repo)
    check_call(["git", "clean", "-f"], wd=self.wrapper_repo)
    check_call(["git", "clean", "-f", "-d"], wd=self.wrapper_repo)
    # pull down latest wrappers
    check_call(["git", "pull"], wd=self.wrapper_repo)

  def compile_wrappers_to_jars(self):
    check_call(["ant", "clean"], wd=self.wrapper_proj)
    check_call(["ant"], wd=self.wrapper_proj)

  def build_service_war(self):
    check_call(["ant", "clean"], wd=self.service_build)
    check_call(["ant", "buildwar"], wd=self.service_build)
    shutil.copy2(join(self.service_build, "BigSemanticsService.war"),
                 self.webapps_dir)

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
      self.archive_war()
      self.release_to_prod()
      print "archived and released.\n"

  def archive_war(self):
    war_file = join(self.webapps_dir, "BigSemanticsService.war")
    tag = datetime.datetime.now().strftime(".%Y%m%d%H")
    dest_file = join(self.war_archive_dir, "BigSemanticsService.war" + tag)
    shutil.copyfile(war_file, dest_file)
    files = listdir(self.war_archive_dir)
    archives = [f for f in files if f.startswith("BigSemanticsService.war.")]
    if len(archives) > self.max_war_archives:
      archives = sorted(archives)
      remove(join(self.war_archive_dir, archives[0]))

  def release_to_prod(self):
    # copy the war file
    war_file = "BigSemanticsService.war"
    dest_dir = "{0}@{1}:{2}".format(self.prod_user,
                                    self.prod_host,
                                    self.prod_webapps_dir)
    cmds = ["scp", "-i", self.prod_login_id, war_file, dest_dir]
    check_call(cmds, wd = self.webapps_dir)
    # copy the generated visualization file
    onto_vis_dir = join(self.wrapper_proj, "OntoViz")
    onto_vis_data_file = "mmd_repo.json"
    dest_static_html_dir = join(self.prod_webapps_dir, "root")
    dest_dir = "{0}@{1}:{2}".format(self.prod_user,
                                    self.prod_host,
                                    dest_static_html_dir)
    cmds = ["scp", "-i", self.prod_login_id, onto_vis_data_file, dest_dir]
    check_call(cmds, wd = onto_vis_dir)
    # generate and copy the example table data file
    example_table_script = "generate_domain_example_table.py"
    example_table_data_file = "domain_type_examples.json"
    cmds = ["python", example_table_script, "--out", example_table_data_file]
    check_call(cmds, wd = onto_vis_dir)
    cmds = ["scp", "-i", self.prod_login_id, example_table_data_file, dest_dir]
    check_call(cmds, wd = onto_vis_dir)



if __name__ == "__main__":
  builder = ServiceBuilder()
  try:
    builder.pull_wrappers()
    builder.compile_wrappers_to_jars()
    builder.build_service_war()
    builder.start_local_service()
    builder.test_local_service_and_release()
    print "everything done."
  except Exception as e:
    import sys
    sys.stderr.write("dev build failed! see email notification.")
    send_bot_email_to_maintainers("Dev build failed.", str(e))

