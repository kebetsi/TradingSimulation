#!/bin/bash

# EPFL Bigdata Course 2015, Tick data fetcher V.1

# Connects to a remote data server and fetches "tick-last/bid/ask-quotes 1s bar" data.
# It saves all the zip files into the directory data/ in the working directory,
# it unzips the data and deletes the used zip files.

# Usage: ./getData.sh <server>

SERVER=$1 # set server here to a constant if you use the script often

for j in `seq 2013 2014`; do # Years you want to fetch (min 2003)
for i in `seq 1 12`; do # Months you want to fetch (max in running year is running month)
for TYPE in  tick-last-quotes tick-ask-quotes tick-bid-quotes; do
	# helper variables
	YEAR=$j
	MONTH=`printf "%02d" $i`
	TK=`curl http://${SERVER}/download-free-forex-historical-data/?/ninjatrader/${TYPE}/eurchf/${YEAR}/${MONTH} --silent | perl -n -e '/id=\"tk\" value=\"([a-z0-9]+)\"/ && print "$1\n"' | head -n 1`;
	case $TYPE in 
		tick-last-quotes) TYPECODE="T_LAST";;
		tick-ask-quotes) TYPECODE="T_ASK";;
		tick-bid-quotes) TYPECODE="T_BID";;
		*) echo "What's that type!?"; exit 1;
	esac

	echo "DENNIS' TICK DATA FETCHER: fetching month $MONTH of year $YEAR"

	# compiling the curl command
	FILENAME="${YEAR}.${MONTH}.zip"
	ORIGIN="Origin: http://${SERVER}"
	REFERER="Referer: http://${SERVER}/download-free-forex-historical-data/?/ninjatrader/${TYPE}/eurchf/${YEAR}/${MONTH}"
	DATA="tk=${TK}&date=${YEAR}&datemonth=${YEAR}${MONTH}&platform=NT&timeframe=${TYPECODE}&fxpair=EURCHF"

	# getting and saving the data
	mkdir -p data
	CMD="curl 'http://${SERVER}/get.php' -o 'data/$FILENAME' -H '$ORIGIN' -H '$REFERER' --data '$DATA'"
	echo "$CMD"
	eval "$CMD"
	unzip data/$FILENAME -d data
	rm data/*.zip
done
done
done