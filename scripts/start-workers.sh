#!/bin/bash

# EPFL Bigdata Course 2015, remote actor starter/stopper script V.1

# Start and stop the remote actor system on our worker nodes

if [ $# -ne "1" ]; then
	echo "Usage: ./start-workers.sh <start | stop | status>"
	exit
fi

DOMAINNAME=".cloudapp.net"
HOSTS=(ts-1-021qv44y ts-2) #ts-3 ts-4 ts-5 ts-6 ts-7 ts-8)
USERS=(merlin dennis) #ts3 jakub ts-5 jakob ts-7 ts-8)
PORTS=(22 22) # 22 22 22 22 65530 52640)


for i in "${!HOSTS[@]}"; do
	HOST=${HOSTS[$i]}
	USER=${USERS[$i]}
	PORT=${PORTS[$i]}
	echo "Connecting to $USER@$HOST$DOMAINNAME port $PORT"

	case "$1" in
		start)
			# The following long line does:
			#	1. "ssh -f -n <server> <command>" connects to that server and executes the specified command
			#	2. "sh -c <command2>"" executes command2 in a shell environment
			#	3. "nohup <command3>" makes sure that command3 is not killed when its parent is killed 
			#		(in our case: sh will be killed as soon as ssh has sent its command and disconnects, but nohup assures this doesn't happen to sbt)
			#	4. "sbt 'project ts' 'runMain ch.epfl.ts.remoting.RemotingActorExample'" runs our remote actor system
			#	5. " > ~/RemoteActor.log 2>&1" redirects all sbt output to ~/RemoteActor.log
			ssh -n -f $USER@$HOST$DOMAINNAME -p $PORT "sh -c \"\cd ~/TradingSimulation; nohup sbt 'project ts' 'runMain ch.epfl.ts.remoting.RemotingActorExample' > ~/RemotingActor.log 2>&1 &\""
			;;
		stop)
			ssh -n -f $USER@$HOST$DOMAINNAME -p $PORT "kill $(ps -ef | grep '[R]emotingActor' | awk '{print $2}')"
			;;
		status)
			bold=$(tput bold) ; normal=$(tput sgr0) # Text formatting
			echo "${bold}ps -ef | grep '[R]emotingActor'${normal} on $HOST$DOMAINNAME:"
			ssh $USER@$HOST$DOMAINNAME -p $PORT "ps -ef | grep '[R]emotingActor'"
			;;
		*)
			echo "Unknown action $1"
			;;
	esac
done
	