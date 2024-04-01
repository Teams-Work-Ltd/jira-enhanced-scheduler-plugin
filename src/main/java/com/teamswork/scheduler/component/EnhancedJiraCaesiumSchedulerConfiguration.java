package com.teamswork.scheduler.component;

import com.atlassian.jira.cluster.ClusterNodeProperties;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.scheduler.caesium.spi.CaesiumSchedulerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.TimeZone;

import static com.teamswork.scheduler.component.SchedulerInitializer.*;

/**
 * Enhanced Jira Caesium Scheduler Configuration.
 * Allows for the configuration of the Jira Caesium Scheduler threads via ApplicationProperties.
 */
@Named
public class EnhancedJiraCaesiumSchedulerConfiguration implements CaesiumSchedulerConfiguration {
    private static final Logger log = LoggerFactory.getLogger(EnhancedJiraCaesiumSchedulerConfiguration.class);
    private static final int REFRESH_INTERVAL_IN_MINUTES = 5;
    private int workerThreadCount;

    private final ApplicationProperties applicationProperties;
    private final ClusterNodeProperties clusterNodeProperties;

    public EnhancedJiraCaesiumSchedulerConfiguration(
            @ComponentImport final ApplicationProperties applicationProperties,
            @ComponentImport final ClusterNodeProperties clusterNodeProperties) {
        this.applicationProperties = applicationProperties;
        this.clusterNodeProperties = clusterNodeProperties;
        workerThreadCount = getWorkerThreadCount();
    }

    @Override
    public TimeZone getDefaultTimeZone() {
        final String zoneId = applicationProperties.getString(APKeys.JIRA_DEFAULT_TIMEZONE);
        return (zoneId != null) ? TimeZone.getTimeZone(zoneId) : null;
    }

    @Override
    public int refreshClusteredJobsIntervalInMinutes() {
        return isClustered() ? REFRESH_INTERVAL_IN_MINUTES : 0;
    }

    @Override
    public int workerThreadCount() {
        return getWorkerThreadCount();
    }

    @Override
    public boolean useQuartzJobDataMapMigration() {
        return true;
    }

    @Override
    public boolean useFineGrainedSchedules() {
        return false;
    }

    private boolean isClustered() {
        return clusterNodeProperties.getNodeId() != null;
    }

    @Override
    public String toString() {
        return "EnhancedJiraCaesiumSchedulerConfiguration {" +
                ", isClustered=" + isClustered() +
                ", refreshClusteredJobsIntervalInMinutes=" + refreshClusteredJobsIntervalInMinutes() +
                ", workerThreadCount=" + workerThreadCount() +
                ", useQuartzJobDataMapMigration=" + useQuartzJobDataMapMigration() +
                ", useFineGrainedSchedules=" + useFineGrainedSchedules() +
                ", getDefaultTimeZone=" + getDefaultTimeZone() +
                ", applicationProperties=" + applicationProperties +
                ", clusterNodeProperties=" + clusterNodeProperties +
                " }";
    }

    private int getWorkerThreadCount() {
        final String workers = applicationProperties.getString(SCHEDULER_THREADS_KEY);
        if (workers != null) {
            try {
                workerThreadCount = Integer.parseInt(workers);
            } catch (NumberFormatException e) {
                log.error("Error parsing worker thread count from application properties. Using default value.");
                workerThreadCount = DEFAULT_THREAD_COUNT;
            }
            log.debug("Worker thread count set to: " + workerThreadCount);
        } else {
            log.debug("Worker thread count not set in application properties. Using default value.");
            workerThreadCount = DEFAULT_THREAD_COUNT;
        }
        return workerThreadCount;
    }
}
