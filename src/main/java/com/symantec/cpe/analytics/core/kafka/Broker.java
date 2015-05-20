package com.symantec.cpe.analytics.core.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Broker {
    @JsonProperty
    String host;

    @JsonProperty
    Integer port;

    @JsonProperty
    Integer id;

    public Broker(String host, Integer port, Integer id) {
        this.host = host;
        this.port = port;
        this.id = id;
    }

    public Broker() {
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Broker{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", id=" + id +
                '}';
    }
}
