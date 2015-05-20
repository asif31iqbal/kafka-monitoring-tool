package com.symantec.cpe.analytics.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseMessage {
    @JsonProperty(value = "Message")
    private String message;

    public ResponseMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
