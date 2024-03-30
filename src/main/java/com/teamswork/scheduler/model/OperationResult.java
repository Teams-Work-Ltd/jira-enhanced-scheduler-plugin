package com.teamswork.scheduler.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("unused")
@XmlRootElement(name = "currentConfiguration")
@XmlAccessorType(XmlAccessType.FIELD)
public class OperationResult {

    private boolean success;
    private String message;

    public OperationResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "OperationResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
