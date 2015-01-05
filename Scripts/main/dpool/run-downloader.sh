#!/bin/bash

killall -9 java

nohup java -server -Xms128m -Xmx256m -jar Downloader.jar 2>&1 >/dev/null &

