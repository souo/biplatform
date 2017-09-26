#!/usr/bin/env bash

BASEDIR="$(cd "`dirname "$0"`"/..; pwd)"
log=${log:-"${BASEDIR}/biplatform.log"}
pid=${pid:-"${BASEDIR}/biplatform.pid"}

usage="Usage: service.sh (start|stop|status|restart)"
# if no args specified, show usage
if [ $# -lt 1 ]; then
  echo $usage
  exit 1
fi

start()
{
  if [ -f $pid ];then
     TARGET_ID="$(cat "$pid")"
      if [[ $(ps -p "$TARGET_ID" -o comm=) =~ "java" ]]; then
        echo "$command running as process $TARGET_ID.  Stop it first."
        exit 1
      fi
  fi

  echo "starting biplatform..."
  nohup -- /bin/bash ${BASEDIR}/bin/local-main 1>>${log} 2>&1 &
  newpid="$!"
  echo "$newpid" > "$pid"
  # Poll for up to 5 seconds for the java process to start
  for i in {1..10}
    do
      if [[ $(ps -p "$newpid" -o comm=) =~ "java" ]]; then
         break
      fi
      sleep 0.5
    done

    sleep 2
    # Check if the process has died; in that case we'll tail the log so the user can see
    if [[ ! $(ps -p "$newpid" -o comm=) =~ "java" ]]; then
      echo "failed to launch biplatform"
      tail -10 "$log" | sed 's/^/  /'
      echo "full log in $log"
    fi
}

stop()
{
  if [ -f $pid ]; then
    TARGET_ID="$(cat "$pid")"
    if [[ $(ps -p "$TARGET_ID" -o comm=) =~ "java" ]]; then
      echo "stopping biplatform..."
      kill "$TARGET_ID" && rm -f "$pid"
    else
      echo "no service to stop"
    fi
  else
    echo "no service to stop"
  fi
}

restart()
{
  stop
  start
}

option=$1

case $option in
  (start)
    start
    ;;

  (stop)
    stop
    ;;

  (status)
    if [ -f $pid ]; then
      TARGET_ID="$(cat "$pid")"
      if [[ $(ps -p "$TARGET_ID" -o comm=) =~ "java" ]]; then
        echo service is running as $TARGET_ID.
        exit 0
      else
        echo $pid file is present but service not running
        exit 1
      fi
    else
      echo service not running.
      exit 2
    fi
    ;;

  (restart)
    restart
    ;;

  (*)
    echo $usage
    exit 1
    ;;

esac