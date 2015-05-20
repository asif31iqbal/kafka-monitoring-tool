package com.symantec.cpe.analytics.core.kafka;

import java.util.Map;

public class KafkaConsumerGroupMetadata {
    private String consumerGroup;
    private String topic;
    private Map<String, Long> partitionOffsetMap;

    public KafkaConsumerGroupMetadata(String consumerGroup, String topic, Map<String, Long> partitionOffsetMap) {
        this.consumerGroup = consumerGroup;
        this.topic = topic;
        this.partitionOffsetMap = partitionOffsetMap;
    }

    public KafkaConsumerGroupMetadata() {
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Map<String, Long> getPartitionOffsetMap() {
        return partitionOffsetMap;
    }

    public void setPartitionOffsetMap(Map<String, Long> partitionOffsetMap) {
        this.partitionOffsetMap = partitionOffsetMap;
    }

    @Override
    public String toString() {
        return "KafkaConsumerGroupMetadata{" +
                "consumerGroup='" + consumerGroup + '\'' +
                ", topic='" + topic + '\'' +
                ", partitionOffsetMap=" + partitionOffsetMap +
                '}';
    }
}
