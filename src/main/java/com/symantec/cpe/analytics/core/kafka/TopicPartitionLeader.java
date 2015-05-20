package com.symantec.cpe.analytics.core.kafka;

public class TopicPartitionLeader {
    private String topic;
    private int partitionId;
    private String leaderHost;
    private int leaderPort;

    public TopicPartitionLeader(String topic, int partitionId, String leaderHost, int leaderPort) {
        this.topic = topic;
        this.partitionId = partitionId;
        this.leaderHost = leaderHost;
        this.leaderPort = leaderPort;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(int partitionId) {
        this.partitionId = partitionId;
    }

    public String getLeaderHost() {
        return leaderHost;
    }

    public void setLeaderHost(String leaderHost) {
        this.leaderHost = leaderHost;
    }

    public int getLeaderPort() {
        return leaderPort;
    }

    public void setLeaderPort(int leaderPort) {
        this.leaderPort = leaderPort;
    }
}
