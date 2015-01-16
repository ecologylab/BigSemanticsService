#!/bin/bash

# Set BSS_DIR to the service directory.
_BSS_DIR=${BSS_DIR:="$HOME/bigsemantics-service"}

echo "BigSemantics Service Dir: $_BSS_DIR"
cd "$_BSS_DIR"

if [ -d "$_BSS_DIR" ]; then
  if [ -d "$_BSS_DIR/bs-code" ]; then
    echo "Updating BigSemantics code ..."
    cd $_BSS_DIR/bs-code
    git submodule foreach git checkout -- "*"
    git submodule foreach git clean -f
    git submodule foreach git clean -f -d
    git checkout -- "*"
    git clean -f
    git clean -f -d
    git pull
  fi

  echo "Stopping running service if any ..."
  killall -9 java

  cd $_BSS_DIR/deploy
  # By default, the service runs on port 8080
  # You may want to map port 8080 to 80 in your system, e.g.:
  #   sudo iptables -t nat -I PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080
  echo "Starting BigSemantics service ..."
  nohup java -server -jar BigSemanticsService.jar 2>&1 > /dev/null &
  sleep 20
  echo "Done."
else
  echo "Service directory not found: $_BSS_DIR"
fi

