On the performance of the Historical Data CSV Fetcher
-----------------------------------------------------
The size of our data for one currency pair (e.g. EURCHF) is about 75M records or 3.2GB (1 record is about 43 Bytes when saved in an SQLite DB). It takes an average of 10us per record to put it into the DB on my system, i.e. about 750s = 12.5min for one currency pair. Let's make that 15min to account for uncertainty in estimations. That's about 4.3 MB/s of reading, parsing and writing to the DB. So for four currency pairs it will take about 1h to load them into a persistor with the current system.

When using the fetcher as an actor and reading data directly from CSV for show or simulation purposes, it will start sending the first quotes as soon as they have been read. The fetcher keeps reading from disk lazily (i.e. only when needed) and only loads into memory what's necessary.

To optimize performance in a distributed system one should probably use a DB as basis (not CSV, which needs to be parsed first). Also, one could distribute DBs for different currency pairs on different nodes, depending on the configuration and the network traffic this could be beneficial.
