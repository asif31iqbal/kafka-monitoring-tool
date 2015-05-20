package com.symantec.cpe.analytics.core.managed;

import com.symantec.cpe.analytics.KafkaMonitorConfiguration;
import com.symantec.cpe.analytics.core.kafka.KafkaConsumerGroupMetadata;
import com.symantec.cpe.analytics.kafka.KafkaConsumerOffsetUtil;
import io.dropwizard.lifecycle.Managed;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ZKClient implements Managed {

    private static final Logger LOG = LoggerFactory.getLogger(ZKClient.class);
    private KafkaMonitorConfiguration kafkaConfiguration;
    private RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    private CuratorFramework client;
    private static final List<String> nonSpoutConsumerNodes = Arrays.asList("storm", "config", "consumers", "controller_epoch", "zookeeper", "admin", "controller", "brokers");

    public ZKClient(KafkaMonitorConfiguration kafkaConfiguration) {
        this.kafkaConfiguration = kafkaConfiguration;
    }


    @Override
    public void start() throws Exception {
        client = CuratorFrameworkFactory.newClient(kafkaConfiguration.getZookeeperUrls(), retryPolicy);
        client.start();
    }

    @Override
    public void stop() throws Exception {
        client.close();
        KafkaConsumerOffsetUtil.closeConnection();
    }

    public List<KafkaConsumerGroupMetadata> getActiveRegularConsumersAndTopics() throws Exception {
        List<KafkaConsumerGroupMetadata> kafkaConsumerGroupMetadataList = new ArrayList<KafkaConsumerGroupMetadata>();
        Set<String> consumerGroups = new HashSet<String>((client.getChildren().forPath("/consumers")));
        for (String consumerGroup : consumerGroups) {
            if (client.checkExists().forPath("/consumers/" + consumerGroup + "/offsets") != null) {
                List<String> topics = client.getChildren().forPath("/consumers/" + consumerGroup + "/offsets");
                for (String topic : topics) {
                    List<String> partitions = client.getChildren().forPath("/consumers/" + consumerGroup + "/offsets/" + topic);
                    Map<String, Long> partitionOffsetMap = new HashMap<String, Long>();
                    for (String partition : partitions) {
                        byte[] data = client.getData().forPath("/consumers/" + consumerGroup + "/offsets/" + topic + "/" + partition);
                        if (data != null) {
                            long offset = Long.parseLong(new String(data));
                            partitionOffsetMap.put(partition, offset);
                        }
                    }
                    KafkaConsumerGroupMetadata kafkaConsumerGroupMetadata = new KafkaConsumerGroupMetadata(consumerGroup, topic, partitionOffsetMap);
                    kafkaConsumerGroupMetadataList.add(kafkaConsumerGroupMetadata);
                }
            }
        }
        return kafkaConsumerGroupMetadataList;
    }

    public List<String> getActiveSpoutConsumerGroups() throws Exception {
        List<String> rootChildren = (client.getChildren().forPath("/"));
        List<String> activeSpoutConsumerGroupList = new ArrayList<String>();
        for(String rootChild : rootChildren) {
            if(!nonSpoutConsumerNodes.contains(rootChild)) {
                activeSpoutConsumerGroupList.add(rootChild);
            }
        }
        return activeSpoutConsumerGroupList;
    }

    public List<String> getChildren(String path) throws Exception {
        return client.getChildren().forPath(path);
    }

    public byte[] getData(String path) throws Exception {
        return client.getData().forPath(path);
    }
}
