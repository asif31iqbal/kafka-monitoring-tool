package com.symantec.cpe.analytics;

import io.dropwizard.Configuration;

import javax.validation.Valid;

public class KafkaMonitorConfiguration extends Configuration {

    @Valid
    private String zookeeperUrls = "localhost:2181";

    @Valid
    private int refreshSeconds = 60;

    @Valid
    private String statsDHost = "localhost";

    @Valid
    private int statsDPort = 8125;

    @Valid
    private String statsDPrefix = "kafka-monitoring";

    @Valid
    private boolean pushToStatsD = false;

    public String getZookeeperUrls() {
        return zookeeperUrls;
    }

    public void setZookeeperUrls(String zookeeperUrls) {
        this.zookeeperUrls = zookeeperUrls;
    }

    public int getRefreshSeconds() {
        return refreshSeconds;
    }

    public void setRefreshSeconds(int refreshSeconds) {
        this.refreshSeconds = refreshSeconds;
    }

    public String getStatsDHost() {
        return statsDHost;
    }

    public void setStatsDHost(String statsDHost) {
        this.statsDHost = statsDHost;
    }

    public int getStatsDPort() {
        return statsDPort;
    }

    public void setStatsDPort(int statsDPort) {
        this.statsDPort = statsDPort;
    }

    public String getStatsDPrefix() {
        return statsDPrefix;
    }

    public void setStatsDPrefix(String statsDPrefix) {
        this.statsDPrefix = statsDPrefix;
    }

    public boolean isPushToStatsD() {
        return pushToStatsD;
    }

    public void setPushToStatsD(boolean pushToStatsD) {
        this.pushToStatsD = pushToStatsD;
    }
}
