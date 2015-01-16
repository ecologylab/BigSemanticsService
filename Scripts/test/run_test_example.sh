#!/bin/bash
#Author Zach
#This is an example of how to run a jmeter test with out using the GUI
./jmeter -n -t ./test_plan_file.jmx -l ./results_file.jtl 
#The -n specifies non-GUI mode

#If the test needs to be shutdown gracefully run the shutdown.sh script, if it needs to be immediately shutdown run stoptest.sh

#Here's a link that gives some more information about starting jmeter in non-GUI mode 
# http://blazemeter.com/blog/dear-abby-blazemeter-how-do-i-run-jmeter-non-gui-mode
