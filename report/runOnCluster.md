## Running a trading simulator setup in the "cloud"

###Basic concepts
There is a master and several slave nodes. The master runs (_RemotingHostExample_) and the slaves must run (_RemotingActorExample_) which make them listen on TCP port 3333. The master can then sends jobs to the slaves.

###Setting up the slaves
The setup procedure for each VM consisted of:

1. Aquire your VM with SSH and full access
2. Run a setup script something like https://github.com/merlinND/TradingSimulation/blob/run-on-cluster/scripts/vm-prepare.sh .
3. Run `screen` and press enter to get a shell
4. Run the project as you would normally: `sbt "project ts" run` and select the _RemotingActorExample_ class in the component selection.
5. The VM is now ready to communicate with _RemotingHostExample_ which you can run from your machine or from another node which you dedicate to be the master.
6. Press _CTRL+A+D_ to detach from this screen (you can reattach by running `screen -D -R`(connects to the first best session) or `screen -r 34863.pts-0.ts-1` (if you have noted down the identifier of your session after having detached before).
7. You can now disconnect form this machine. The process will keep running.