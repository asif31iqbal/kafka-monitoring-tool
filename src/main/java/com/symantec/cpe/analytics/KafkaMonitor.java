package com.symantec.cpe.analytics;

import com.symantec.cpe.analytics.core.managed.ZKClient;
import com.symantec.cpe.analytics.resources.kafka.KafkaResource;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class KafkaMonitor extends Application<KafkaMonitorConfiguration> {

    @Override
    public void initialize(Bootstrap<KafkaMonitorConfiguration> bootstrap) {
    }

    @Override
    public void run(KafkaMonitorConfiguration configuration, Environment environment)
            throws Exception {
        ZKClient zkClient = new ZKClient(configuration);
        environment.lifecycle().manage(zkClient);
        KafkaResource kafkaResource = new KafkaResource(configuration, zkClient);
        environment.jersey().register(kafkaResource);
    }

    @Override
    public String getName() {
        return "kafka-monitor";
    }

    public static void main(String[] args) throws Exception {
        new KafkaMonitor().run(args);
    }

}
