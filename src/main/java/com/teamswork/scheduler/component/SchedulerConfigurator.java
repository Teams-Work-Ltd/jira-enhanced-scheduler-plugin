package com.teamswork.scheduler.component;

import com.atlassian.jira.component.ComponentAccessor;

import com.atlassian.scheduler.caesium.impl.CaesiumSchedulerService;
import com.atlassian.scheduler.caesium.spi.CaesiumSchedulerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Named
@SuppressWarnings("unused")
public class SchedulerConfigurator {
    private static final Logger log = LoggerFactory.getLogger(SchedulerConfigurator.class);

    private final CaesiumSchedulerConfiguration enhancedJiraCaesiumSchedulerConfiguration;

    public SchedulerConfigurator(final EnhancedJiraCaesiumSchedulerConfiguration enhancedJiraCaesiumSchedulerConfiguration) {
        this.enhancedJiraCaesiumSchedulerConfiguration = enhancedJiraCaesiumSchedulerConfiguration;

        log.debug("Re-configuring the Jira caesium scheduler.");
        log.debug("Unregistering the original configuration of the caesium scheduler and the scheduler.");
        try {
            final CaesiumSchedulerService caesiumSchedulerService = ComponentAccessor.getComponent(CaesiumSchedulerService.class);

            // Replace the original configuration that has a fixed  number of 4 worker threads with the new configuration.
            // We get a Jira scheduler back from ComponentAccessor, so we need to go a level up to the Caesium scheduler.
            final Field field = caesiumSchedulerService.getClass().getSuperclass().getDeclaredField("config");
            field.setAccessible(true);
            final Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            field.set(caesiumSchedulerService, enhancedJiraCaesiumSchedulerConfiguration);

            // Add a new set of workers to the existing queue configuration.
            final Method startWorkers = caesiumSchedulerService.getClass().getSuperclass().getDeclaredMethod("startWorkers");
            startWorkers.setAccessible(true);
            startWorkers.invoke(caesiumSchedulerService);
        } catch (Exception e) {
            log.error("Error re-configuring the caesium scheduler: " + e.getMessage());
            return;
        }

        log.debug("Scheduler re-configured successfully.");
    }
}
