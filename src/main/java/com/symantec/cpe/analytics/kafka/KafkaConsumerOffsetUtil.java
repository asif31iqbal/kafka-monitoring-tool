package com.symantec.cpe.analytics.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.symantec.cpe.analytics.KafkaMonitorConfiguration;
import com.symantec.cpe.analytics.core.kafka.KafkaConsumerGroupMetadata;
import com.symantec.cpe.analytics.core.kafka.KafkaOffsetMonitor;
import com.symantec.cpe.analytics.core.kafka.KafkaSpoutMetadata;
import com.symantec.cpe.analytics.core.kafka.TopicPartitionLeader;
import com.symantec.cpe.analytics.core.managed.ZKClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.cluster.Broker;
import kafka.common.TopicAndPartition;
import kafka.javaapi.*;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.JavaConversions;

import java.util.*;
import java.util.concurrent.*;


public class KafkaConsumerOffsetUtil {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerOffsetUtil.class);

    private static final Map<String, SimpleConsumer> consumerMap = new HashMap<String, SimpleConsumer>();
    private StatsDClient statsd = null;
    private static final String clientName = "GetOffsetClient";
    private KafkaMonitorConfiguration kafkaConfiguration;
    private static KafkaConsumerOffsetUtil kafkaConsumerOffsetUtil = null;
    private ZKClient zkClient;
    private static ZkClient iotecZkClient;

    public static KafkaConsumerOffsetUtil getInstance(KafkaMonitorConfiguration kafkaConfiguration, ZKClient zkClient) {
        if(kafkaConsumerOffsetUtil == null) {
            kafkaConsumerOffsetUtil = new KafkaConsumerOffsetUtil(kafkaConfiguration, zkClient);
        }
        iotecZkClient = new ZkClient(kafkaConfiguration.getZookeeperUrls(), 5000, 5000, ZKStringSerializer$.MODULE$);
        return kafkaConsumerOffsetUtil;
    }

    private KafkaConsumerOffsetUtil(KafkaMonitorConfiguration kafkaConfiguration, ZKClient zkClient) {
        this.kafkaConfiguration = kafkaConfiguration;
        this.zkClient = zkClient;
    }

    public void setupMonitoring() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture scheduledFuture = executorService.scheduleAtFixedRate(new KafkaConsumerOffsetThread(), 5, kafkaConfiguration.getRefreshSeconds(), TimeUnit.SECONDS);
    }

    private class KafkaConsumerOffsetThread implements Runnable {
        @Override
        public void run() {
            try {
                List<KafkaOffsetMonitor> kafkaOffsetMonitors = new ArrayList<KafkaOffsetMonitor>();
                kafkaOffsetMonitors.addAll(getSpoutKafkaOffsetMonitors());
                kafkaOffsetMonitors.addAll(getRegularKafkaOffsetMonitors());
                Collections.sort(kafkaOffsetMonitors, new KafkaOffsetMonitorComparator());
                sendToStatsD(kafkaOffsetMonitors, kafkaConfiguration.getStatsDPrefix(), kafkaConfiguration.getStatsDHost(), kafkaConfiguration.getStatsDPort());
            } catch (Exception e) {
                LOG.error("Error while collecting kafka consumer offset metrics", e);
            }
        }
    }

    public List<KafkaOffsetMonitor> getSpoutKafkaOffsetMonitors() throws Exception {
        List<KafkaOffsetMonitor> kafkaOffsetMonitors = new ArrayList<KafkaOffsetMonitor>();
        List<String> activeSpoutConsumerGroupList = zkClient.getActiveSpoutConsumerGroups();
        List<String> partitions = new ArrayList<String>();
        for (String consumerGroup : activeSpoutConsumerGroupList) {
            try {
                partitions = zkClient.getChildren("/" + consumerGroup);
            } catch (Exception e) {
                LOG.error("Error while listing partitions for the consumer group: " + consumerGroup);
            }
            for (String partition : partitions) {
                byte[] byteData = zkClient.getData("/" + consumerGroup + "/" + partition);
                String data = "";
                if (byteData != null) {
                    data = new String(byteData);
                }
                if (!data.trim().isEmpty()) {
                    KafkaSpoutMetadata kafkaSpoutMetadata = new ObjectMapper().readValue(data, KafkaSpoutMetadata.class);
                    SimpleConsumer consumer = getConsumer(kafkaSpoutMetadata.getBroker().getHost(), kafkaSpoutMetadata.getBroker().getPort(), clientName);
                    long realOffset = getLastOffset(consumer, kafkaSpoutMetadata.getTopic(), kafkaSpoutMetadata.getPartition(), -1, clientName);
                    long lag = realOffset - kafkaSpoutMetadata.getOffset();
                    KafkaOffsetMonitor kafkaOffsetMonitor = new KafkaOffsetMonitor(consumerGroup, kafkaSpoutMetadata.getTopic(),
                            kafkaSpoutMetadata.getPartition(), realOffset, kafkaSpoutMetadata.getOffset(), lag);
                    kafkaOffsetMonitors.add(kafkaOffsetMonitor);
                }
            }
        }
        return kafkaOffsetMonitors;
    }

    public List<KafkaOffsetMonitor> getRegularKafkaOffsetMonitors() throws Exception {
        List<KafkaConsumerGroupMetadata> kafkaConsumerGroupMetadataList = zkClient.getActiveRegularConsumersAndTopics();
        List<KafkaOffsetMonitor> kafkaOffsetMonitors = new ArrayList<KafkaOffsetMonitor>();
        List<Broker> kafkaBrokers = getAllBrokers();
        SimpleConsumer consumer = getConsumer(kafkaBrokers.get(1).host(), kafkaBrokers.get(1).port(), clientName);
        for (KafkaConsumerGroupMetadata kafkaConsumerGroupMetadata : kafkaConsumerGroupMetadataList) {
            List<TopicPartitionLeader> partitions = getPartitions(consumer, kafkaConsumerGroupMetadata.getTopic());
            for (TopicPartitionLeader partition : partitions) {
                consumer = getConsumer(partition.getLeaderHost(), partition.getLeaderPort(), clientName);
                long kafkaTopicOffset = getLastOffset(consumer, kafkaConsumerGroupMetadata.getTopic(), partition.getPartitionId(), -1, clientName);
                long consumerOffset = 0;
                if (kafkaConsumerGroupMetadata.getPartitionOffsetMap().get(Integer.toString(partition.getPartitionId())) != null) {
                    consumerOffset = kafkaConsumerGroupMetadata.getPartitionOffsetMap().get(Integer.toString(partition.getPartitionId()));
                }
                long lag = kafkaTopicOffset - consumerOffset;
                KafkaOffsetMonitor kafkaOffsetMonitor = new KafkaOffsetMonitor(kafkaConsumerGroupMetadata.getConsumerGroup(), kafkaConsumerGroupMetadata.getTopic(), partition.getPartitionId(), kafkaTopicOffset, consumerOffset, lag);
                kafkaOffsetMonitors.add(kafkaOffsetMonitor);
            }
        }
        return kafkaOffsetMonitors;
    }

    public List<TopicPartitionLeader> getPartitions(SimpleConsumer consumer, String topic) {
        List<TopicPartitionLeader> partitions = new ArrayList<TopicPartitionLeader>();
        TopicMetadataRequest topicMetadataRequest = new TopicMetadataRequest(Collections.singletonList(topic));
        TopicMetadataResponse topicMetadataResponse = consumer.send(topicMetadataRequest);
        List<TopicMetadata> topicMetadataList = topicMetadataResponse.topicsMetadata();
        for (TopicMetadata topicMetadata : topicMetadataList) {
            List<PartitionMetadata> partitionMetadataList = topicMetadata.partitionsMetadata();
            for (PartitionMetadata partitionMetadata : partitionMetadataList) {
                if (partitionMetadata.leader() != null) {
                    String partitionLeaderHost = partitionMetadata.leader().host();
                    int partitionLeaderPort = partitionMetadata.leader().port();
                    int partitionId = partitionMetadata.partitionId();
                    TopicPartitionLeader topicPartitionLeader = new TopicPartitionLeader(topic, partitionId, partitionLeaderHost, partitionLeaderPort);
                    partitions.add(topicPartitionLeader);
                }
            }
        }
        return partitions;
    }

    public long getLastOffset(SimpleConsumer consumer, String topic, int partition,
                               long whichTime, String clientName) {
        long lastOffset = 0;
        try {
            List<String> topics = Collections.singletonList(topic);
            TopicMetadataRequest req = new TopicMetadataRequest(topics);
            kafka.javaapi.TopicMetadataResponse topicMetadataResponse = consumer.send(req);
            TopicAndPartition topicAndPartition = new TopicAndPartition(topic, partition);
            for (TopicMetadata topicMetadata : topicMetadataResponse.topicsMetadata()) {
                for (PartitionMetadata partitionMetadata : topicMetadata.partitionsMetadata()) {
                    if (partitionMetadata.partitionId() == partition) {
                        String partitionHost = partitionMetadata.leader().host();
                        consumer = getConsumer(partitionHost, partitionMetadata.leader().port(), clientName);
                        break;
                    }
                }
            }
            Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
            requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(whichTime, 1));
            kafka.javaapi.OffsetRequest request = new kafka.javaapi.OffsetRequest(requestInfo, kafka.api.OffsetRequest.CurrentVersion(), clientName);
            OffsetResponse response = consumer.getOffsetsBefore(request);
            if (response.hasError()) {
                LOG.error("Error fetching Offset Data from the Broker. Reason: " + response.errorCode(topic, partition));
                lastOffset = 0;
            }
            long[] offsets = response.offsets(topic, partition);
            lastOffset = offsets[0];
        } catch (Exception e) {
            LOG.error("Error while collecting the log Size for topic: " + topic + ", and partition: " + partition, e);
        }
        return lastOffset;
    }

    public SimpleConsumer getConsumer(String host, int port, String clientName) {
        SimpleConsumer consumer = consumerMap.get(host);
        if (consumer == null) {
            consumer = new SimpleConsumer(host, port, 100000, 64 * 1024, clientName);
            LOG.info("Created a new Kafka Consumer for host: " + host);
            consumerMap.put(host, consumer);
        }
        return consumer;
    }

    public static void closeConnection() {
        for (SimpleConsumer consumer : consumerMap.values()) {
            LOG.info("Closing connection for: " + consumer.host());
            consumer.close();
        }
    }

    public String htmlOutput(List<KafkaOffsetMonitor> kafkaOffsetMonitors) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><pre>");
        sb.append(String.format("%s \t %s \t %s \t %s \t %s \t %s \n",
                StringUtils.rightPad("Consumer Group", 40),
                StringUtils.rightPad("Topic", 40),
                StringUtils.rightPad("Partition", 10),
                StringUtils.rightPad("Log Size", 10),
                StringUtils.rightPad("Consumer Offset", 15),
                StringUtils.rightPad("Lag", 10)));
        for (KafkaOffsetMonitor kafkaOffsetMonitor : kafkaOffsetMonitors) {
            sb.append(String.format("%s \t %s \t %s \t %s \t %s \t %s \n",
                    StringUtils.rightPad(kafkaOffsetMonitor.getConsumerGroupName(), 40),
                    StringUtils.rightPad(kafkaOffsetMonitor.getTopic(), 40),
                    StringUtils.rightPad("" + kafkaOffsetMonitor.getPartition(), 10),
                    StringUtils.rightPad("" + kafkaOffsetMonitor.getLogSize(), 10),
                    StringUtils.rightPad("" + kafkaOffsetMonitor.getConsumerOffset(), 15),
                    StringUtils.rightPad("" + kafkaOffsetMonitor.getLag(), 10)));
        }
        sb.append("</pre></body></html>");
        return sb.toString();
    }

    public void sendToStatsD(List<KafkaOffsetMonitor> kafkaOffsetMonitors, String prefix, String statsDServer, int statsDPort) throws Exception {
        if (statsd == null) {
            statsd = new NonBlockingStatsDClient(prefix, statsDServer, statsDPort);
        }
        for (KafkaOffsetMonitor kafkaOffsetMonitor : kafkaOffsetMonitors) {
            String statsDSeriesName = kafkaOffsetMonitor.getConsumerGroupName() + "."
                    + kafkaOffsetMonitor.getTopic() + "." + kafkaOffsetMonitor.getPartition();
            LOG.info(prefix + "." + statsDSeriesName + ".topic_offset: " + kafkaOffsetMonitor.getLogSize());
            statsd.gauge(statsDSeriesName + ".topic_offset", kafkaOffsetMonitor.getLogSize());
            LOG.info(prefix + "." + statsDSeriesName + ".consumer_offset: " + kafkaOffsetMonitor.getConsumerOffset());
            statsd.gauge(statsDSeriesName + ".consumer_offset", kafkaOffsetMonitor.getConsumerOffset());
            LOG.info(prefix + "." + prefix + "." + statsDSeriesName + ".lag: " + kafkaOffsetMonitor.getLag());
            statsd.gauge(statsDSeriesName + ".lag", kafkaOffsetMonitor.getLag());
        }
    }

    // fetch all available brokers from the zookeeper
    private List<Broker> getAllBrokers() throws Exception {
        List<Broker> brokers = null;
        scala.collection.Seq<Broker> brokerSeq = ZkUtils.getAllBrokersInCluster(iotecZkClient);
        brokers = JavaConversions.seqAsJavaList(brokerSeq);
        return brokers;
    }
}
