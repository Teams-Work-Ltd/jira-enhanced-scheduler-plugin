package com.teamswork.scheduler.component;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
public class SchedulerInitializer extends PluginStateListener {
    public static final String SCHEDULER_THREADS_VALUE = "4";
    public static final int DEFAULT_THREAD_COUNT = 4;
    public static final String SCHEDULER_THREADS_KEY = "jes-scheduler-threads-key";

    private static final Logger log = LoggerFactory.getLogger(SchedulerInitializer.class);

    private final ApplicationProperties applicationProperties;

    public SchedulerInitializer(@ComponentImport final ApplicationProperties applicationProperties,
                                @ComponentImport final EventPublisher eventPublisher) {
        super(eventPublisher);
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected void onAppStart() {
        try {
            final String key = applicationProperties.getString(SCHEDULER_THREADS_KEY);
            if (isEmpty(key)) {
                log.debug("Setting default scheduler threads value.");
                applicationProperties.setString(SCHEDULER_THREADS_KEY, SCHEDULER_THREADS_VALUE);
            } else {
                log.debug("Default scheduler value already set.");
            }
        } catch (final Exception e) {
            log.error("Error setting the default scheduler value key. ", e);
        }
    }

    @Override
    protected void onAppShutdown() {
        try {
            log.debug("Scheduler configurator shutting down.");
        } catch (final Throwable t) {
            log.error("Error in shutdown " + t.getMessage());
        }
    }
}