## Project Description
This is an application to track the offsets for all the consumer groups in a Kafka cluster. This app will collect offset information for both a regular Kafka consumer and also Storm-Kafka Spout. The app provides a rest endpoint that can be called on-demand to get this information. You can also configure the app to push the offset metrics at a regular interval to a StatsD server, so that you can use a metric collection system like InfluxDB/Grafana to collect the information from StatsD and monitor. 
 
## Prerequisites
Java 1.7
Maven 3

## Installation (install commands)
git clone https://github.com/Symantec/kafka-monitoring-tool.git

cd kafka-monitoring-tool

mvn package

You will have the kafka-monitoring-tool-0.0.1.jar created in the “target” directory. 

## Running project
java -jar target/kafka-monitoring-tool-0.0.1.jar server

By default the application assumes the zookeeper is running localhost on port 2181. If you need to provide a zookeeper host, pass it as a jvm parameter like this:

java -Ddw.zookeeperUrls=ZKHOST:ZKPORT,ZKHOST:ZKPORT,ZKHOST:PORT -jar kafka-monitoring-tool-0.0.1.jar server

Once the server is up, run the following command from localhost to get the information in a json format.
curl -X GET http://localhost:8080/kafka/offset

## Configuration (config commands)
The application can also be passed a yml file as a configuration file, instead of passing the zookeeper urls on the command line. Default.yml file is available in the project. The way you start the project with yml file is like this:

java -jar kafka-monitoring-tool-0.0.1.jar server default.yml

### API parameters
There are few Query Params you can pass to the API to get specific results. Examples are:

If you would like the output in a HTML format instead of Json format, try this:
http://localhost:8080/kafka/offset?outputType=html

If you would like to get only the offsets from a regular consumers, try with the consumerType=regular parameter, example:
http://localhost:8080/kafka/offset?consumerType=regular

If you would only like the storm spout consumers, try this:
http://localhost:8080/kafka/offset?consumerType=spout



### PushToStatsD

If you would like to push the stats to a StatsD server, you can configure the service to do so. You just need to enable the the pushToStatsD property. By default the application assumes the statsD is listening on the localhost at port 8125. 

java  -Ddw.zookeeperUrls=ZKHOST:ZKPORT,ZKHOST:ZKPORT,ZKHOST:PORT -Ddw.pushToStatsD=true -jar kafka-monitoring-0.1.0.jar server

By default the metrics are collected and pushed every 60 seconds. If you want to change the frequency, set this property:
java  -Ddw.zookeeperUrls=ZKHOST:ZKPORT,ZKHOST:ZKPORT,ZKHOST:PORT -Ddw.pushToStatsD=true —Ddw.refreshSeconds=60 -jar kafka-monitoring-tool-0.0.1.jar server

To push metrics to statsd server listening on a different host on a different port, set the following parameters:
java  -Ddw.zookeeperUrls=ZKHOST:ZKPORT,ZKHOST:ZKPORT,ZKHOST:PORT -Ddw.pushToStatsD=true —Ddw.refreshSeconds=60 -Ddw.statsDHost=locahost —Ddw.statsDPort=8125 -Ddw.statsDPrefix=kafka-monitor -jar kafka-monitoring-tool-0.0.1.jar server

You can also configure all these properties in a yml config file and pass in the config file to the app, like
java -jar kafka-monitoring-tool-0.0.1.jar server default.yml
