package com.symantec.cpe.analytics.kafka;

import com.google.common.collect.ComparisonChain;
import com.symantec.cpe.analytics.core.kafka.KafkaOffsetMonitor;

import java.util.Comparator;

public class KafkaOffsetMonitorComparator implements Comparator<KafkaOffsetMonitor> {
    public int compare(KafkaOffsetMonitor kafkaOffsetMonitor1, KafkaOffsetMonitor kafkaOffsetMonitor2) {
        return ComparisonChain.start()
                .compare(kafkaOffsetMonitor1.getConsumerGroupName(), kafkaOffsetMonitor2.getConsumerGroupName())
                .compare(kafkaOffsetMonitor1.getTopic(), kafkaOffsetMonitor2.getTopic())
                .compare(kafkaOffsetMonitor1.getPartition(), kafkaOffsetMonitor2.getPartition())
                .result();
    }
}