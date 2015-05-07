#!/bin/bash

# EPFL Bigdata Course 2015, remote actor starter/stopper script V.1

# Start and stop the remote actor system on our worker nodes.
# The script logs into the workers via ssh, it assumes your
# public key is in ~/.ssh/authorized_key on every worker.

if [ $# -ne "1" ]; then
	echo "Usage: ./workerctl.sh <start | stop | status | update-restart>"
	exit
fi

# Which workers to control
DOMAINNAME=".cloudapp.net"
HOSTS=(ts-1-021qv44y ts-2 ts-3 ts-4 ts-5 ts-6 ts-7 ts-8)
USERS=(merlin dennis ts3 jakub ts-5 jakob ts-7 ts-8)
PORTS=(22 22 22 22 58575 22 65530 52640)

# Which git branch to run on workers
BRANCH="master"

# Which class to run on workers
CLASS="ch.epfl.ts.remoting.RemotingWorker"

# Which logfile to write to on workers
LOGFILE_ON_WORKER="~/RemotingWorker.log"

bold=$(tput bold) ; normal=$(tput sgr0) # Text formatting

# Commands
# The start command does:
#	1. "ssh -f -n <server> <command>" connects to that server and executes the specified command
#	2. "sh -c <command2>"" executes command2 in a shell environment
#	3. "nohup <command3>" makes sure that command3 is not killed when its parent is killed
#		(in our case: sh will be killed as soon as ssh has sent its command and disconnects,
#		but nohup assures this doesn't happen to sbt)
#	4. "sbt 'project ts' 'runMain ch.epfl.ts.remoting.RemotingWorker'" runs our remote actor system
#	5. " > ~/RemoteActor.log 2>&1" redirects all sbt output to ~/RemoteActor.log
START_CMD="sh -c \"\cd ~/TradingSimulation; \
			nohup sbt 'project ts' 'runMain $CLASS' \
			> $LOGFILE_ON_WORKER 2>&1 &\""

STOP_CMD="kill \$(ps -ef | grep '$CLASS' | grep -v grep | awk '{print \$2}')"

STATUS_CMD="ps -ef | grep '$CLASS' | grep -v grep"

UPDATE_CMD="sh -c 'cd ~/TradingSimulation; git fetch; git checkout origin/$BRANCH'"

for i in "${!HOSTS[@]}"; do
	HOST=${HOSTS[$i]}
	USER=${USERS[$i]}
	PORT=${PORTS[$i]}
	echo "${bold}Connecting to $USER@$HOST$DOMAINNAME port $PORT${normal}"

	case "$1" in
		start)
			ssh $USER@$HOST$DOMAINNAME -p $PORT "$START_CMD"
			echo "Remote actor started. Logs are at $HOST$DOMAINNAME:$LOGFILE_ON_WORKER"
			;;
		stop)
			ssh $USER@$HOST$DOMAINNAME -p $PORT "$STOP_CMD"
			echo "Killed remote actors."
			;;
		status)
			echo "${bold}ps -ef | grep '$CLASS'${normal} on $HOST$DOMAINNAME:"
			ssh $USER@$HOST$DOMAINNAME -p $PORT "$STATUS_CMD"
			;;
		update)
			echo "Updating from git branch $BRANCH"
			ssh $USER@$HOST$DOMAINNAME -p $PORT "$UPDATE_CMD"
			;;
		update-restart)
			echo "Updating and restarting..."
			ssh $USER@$HOST$DOMAINNAME -p $PORT "$UPDATE_CMD; $STOP_CMD; $START_CMD"
			;;
		*)
			echo "Unknown action $1"
			;;
	esac
done

