sudo apt-get update
sudo apt-get install git wget unzip default-jdk
git clone https://github.com/merlinND/TradingSimulation.git
wget https://dl.bintray.com/sbt/native-packages/sbt/0.13.8/sbt-0.13.8.zip
unzip sbt-0.13.8.zip
rm sbt-0.13.8.zip
sudo ln -s ~/sbt/bin/sbt /usr/bin/sbt
cd TradingSimulation/

sbt compile
sbt test
