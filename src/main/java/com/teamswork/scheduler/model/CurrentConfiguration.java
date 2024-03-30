package com.teamswork.scheduler.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@SuppressWarnings("unused")
@XmlRootElement(name = "currentConfiguration")
@XmlAccessorType(XmlAccessType.FIELD)
public class CurrentConfiguration {

    @JsonProperty("extraThreadsToConfigure")
    private int extraThreadsToConfigure;

    @JsonProperty("extraThreadsRunning")
    private int extraThreadsRunning;

    @JsonProperty("threadGroupName")
    private String threadGroupName;

    @JsonProperty("extraThreadGroupStarted")
    private boolean extraThreadGroupStarted;

    @JsonProperty("defaultThreadGroup")
    private String defaultThreadGroup;

    @JsonProperty("schedulerRunning")
    private boolean schedulerRunning;

    @JsonProperty("schedulerReconfigured")
    private boolean schedulerReconfigured;

    public int getExtraThreadsToConfigure() {
        return extraThreadsToConfigure;
    }

    public void setExtraThreadsToConfigure(final int extraThreadsToConfigure) {
        this.extraThreadsToConfigure = extraThreadsToConfigure;
    }

    public int getExtraThreadsRunning() {
        return extraThreadsRunning;
    }

    public void setExtraThreadsRunning(final int extraThreadsRunning) {
        this.extraThreadsRunning = extraThreadsRunning;
    }

    public String getThreadGroupName() {
        return threadGroupName;
    }

    public void setThreadGroupName(final String threadGroupName) {
        this.threadGroupName = threadGroupName;
    }

    public boolean isExtraThreadGroupStarted() {
        return extraThreadGroupStarted;
    }

    public void setExtraThreadGroupStarted(final boolean extraThreadGroupStarted) {
        this.extraThreadGroupStarted = extraThreadGroupStarted;
    }

    public String getDefaultThreadGroup() {
        return defaultThreadGroup;
    }

    public void setDefaultThreadGroup(final String defaultThreadGroup) {
        this.defaultThreadGroup = defaultThreadGroup;
    }

    public boolean isSchedulerRunning() {
        return schedulerRunning;
    }

    public void setSchedulerRunning(final boolean schedulerRunning) {
        this.schedulerRunning = schedulerRunning;
    }

    public boolean isSchedulerReconfigured() {
        return schedulerReconfigured;
    }

    public void setSchedulerReconfigured(final boolean schedulerReconfigured) {
        this.schedulerReconfigured = schedulerReconfigured;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CurrentConfiguration)) return false;

        final CurrentConfiguration that = (CurrentConfiguration) o;

        if (extraThreadsToConfigure != that.extraThreadsToConfigure) return false;
        if (extraThreadsRunning != that.extraThreadsRunning) return false;
        if (extraThreadGroupStarted != that.extraThreadGroupStarted) return false;
        if (schedulerRunning != that.schedulerRunning) return false;
        if (schedulerReconfigured != that.schedulerReconfigured) return false;
        if (!Objects.equals(threadGroupName, that.threadGroupName))
            return false;
        return Objects.equals(defaultThreadGroup, that.defaultThreadGroup);
    }

    @Override
    public int hashCode() {
        int result = extraThreadsToConfigure;
        result = 31 * result + extraThreadsRunning;
        result = 31 * result + (threadGroupName != null ? threadGroupName.hashCode() : 0);
        result = 31 * result + (extraThreadGroupStarted ? 1 : 0);
        result = 31 * result + (defaultThreadGroup != null ? defaultThreadGroup.hashCode() : 0);
        result = 31 * result + (schedulerRunning ? 1 : 0);
        result = 31 * result + (schedulerReconfigured ? 1 : 0);
        return result;
    }
}
