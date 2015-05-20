package com.symantec.cpe.analytics.core.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KafkaSpoutMetadata {
    @JsonProperty
    Long offset;

    @JsonProperty
    Topology topology;

    @JsonProperty
    Integer partition;

    @JsonProperty
    Broker broker;

    @JsonProperty
    String topic;

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public Integer getPartition() {
        return partition;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public Broker getBroker() {
        return broker;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Topology getTopology() {
        return topology;
    }

    public void setTopology(Topology topology) {
        this.topology = topology;
    }

    @Override
    public String toString() {
        return "KafkaSpoutMetadata{" +
                "offset=" + offset +
                ", topology=" + topology +
                ", partition=" + partition +
                ", broker=" + broker +
                ", topic='" + topic + '\'' +
                '}';
    }
}
