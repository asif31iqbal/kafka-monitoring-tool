package com.symantec.cpe.analytics.core.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.symantec.cpe.analytics.core.kafka.Broker;

import java.util.List;

public class KafkaTopicPartitionMetadata {

    @JsonProperty
    Broker leader;

    @JsonProperty
    List<Broker> replicas;

    @JsonProperty
    List<Broker> isr;

    @JsonProperty
    Integer partitionId;

    public Broker getLeader() {
        return leader;
    }

    public void setLeader(Broker leader) {
        this.leader = leader;
    }

    public List<Broker> getReplicas() {
        return replicas;
    }

    public void setReplicas(List<Broker> replicas) {
        this.replicas = replicas;
    }

    public List<Broker> getIsr() {
        return isr;
    }

    public void setIsr(List<Broker> isr) {
        this.isr = isr;
    }

    public Integer getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(Integer partitionId) {
        this.partitionId = partitionId;
    }
}
