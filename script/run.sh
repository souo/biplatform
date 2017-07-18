#!/bin/bash
BASEDIR="$(cd "`dirname "$0"`"/..; pwd)"

LOGFILE=${LOGFILE:-"${BASEDIR}/biplatform.log"}
PIGFILE=${PIDFILE:-"${BASEDIR}/biplatform.pid"}

nohup /bin/bash ${BASEDIR}/bin/local-main 1>>${LOGFILE} 2>&1 &