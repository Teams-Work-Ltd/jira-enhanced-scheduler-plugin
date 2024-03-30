package com.teamswork.scheduler.component;

import com.atlassian.jira.component.ComponentAccessor;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.scheduler.caesium.impl.CaesiumSchedulerService;
import com.atlassian.scheduler.caesium.spi.CaesiumSchedulerConfiguration;
import com.atlassian.scheduler.core.LifecycleAwareSchedulerService;
import com.teamswork.scheduler.model.CurrentConfiguration;
import com.teamswork.scheduler.model.OperationResult;
import com.teamswork.scheduler.service.ThreadGroupUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.teamswork.scheduler.component.SchedulerInitializer.SCHEDULER_THREADS_KEY;

/**
 * Allows for configurion the Jira caesium scheduler.
 * This class should be used in the following way:
 * 1 - Call the configureThreadCount method to set the number of threads for the scheduler, if needed.
 * 2 - Call the replaceCaesiumConfiguration method to replace the original configuration of the scheduler with the enhanced configuration.
 * 3 - Call the pauseScheduler method to pause the scheduler.
 * 4 - Call the startScheduler method to start the scheduler.
 * Note, this will create an extra thread group for the scheduler. These don't get destroyed, so tread carefully and
 * monitor the resources consumed by Jira.
 */
@Named
@SuppressWarnings("unused")
public class SchedulerConfigurator {
    private static final Logger log = LoggerFactory.getLogger(SchedulerConfigurator.class);
    private static final String defaultThreadGroup = "Caesium-1";
    private final CaesiumSchedulerConfiguration enhancedConfig;
    private final ApplicationProperties applicationProperties;
    private final ThreadGroupUtils threadGroupUtils;
    final I18nHelper i18nHelper;
    private boolean schedulerReconfigured = false;

    private final AtomicInteger threadGroupCount = new AtomicInteger(1);

    public SchedulerConfigurator(final EnhancedJiraCaesiumSchedulerConfiguration enhancedConfig,
                                 final ThreadGroupUtils threadGroupUtils,
                                 @ComponentImport ApplicationProperties applicationProperties,
                                 @ComponentImport final I18nHelper i18nHelper) {
        this.enhancedConfig = enhancedConfig;
        this.applicationProperties = applicationProperties;
        this.i18nHelper = i18nHelper;
        this.threadGroupUtils = threadGroupUtils;
    }

    /**
     * Configures the number of threads for the Jira caesium scheduler.
     * @param threadCount the number of threads to configure.
     * @return a message indicating the success or failure of the configuration.
     */
    public OperationResult configureThreadCount(final int threadCount) {
        if (threadCount < 1 || threadCount > 16) {
            return new OperationResult(false, i18nHelper.getText("jes.invalid.thread.count"));
        }
        applicationProperties.setString(SCHEDULER_THREADS_KEY, String.valueOf(threadCount));
        return new OperationResult(true, i18nHelper.getText("jes.thread.count.set", threadCount));
    }

    /**
     * Replaces the original configuration of the Jira caesium scheduler with the enhanced configuration.
     * We must go up a level to the superclass as we get a Jira scheduler back from ComponentAccessor.
     * @return OperationResult with the details of the configuration.
     */
    public OperationResult replaceCaesiumConfiguration() {
        log.debug("Re-configuring the Jira caesium scheduler.");
        log.debug("Unregistering the original configuration of the caesium scheduler and the scheduler.");
        try {
            final CaesiumSchedulerService caesiumSchedulerService = ComponentAccessor.getComponent(CaesiumSchedulerService.class);
            final Field field = caesiumSchedulerService.getClass().getSuperclass().getDeclaredField("config");
            field.setAccessible(true);
            final Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            field.set(caesiumSchedulerService, enhancedConfig);
            schedulerReconfigured = true;
            threadGroupCount.getAndIncrement();
        } catch (Exception e) {
            log.error("Error re-configuring the caesium scheduler: " + e.getMessage());
            schedulerReconfigured = false;
        }

        if (schedulerReconfigured) {
            log.debug("Scheduler re-configured successfully.");
        }

        return new OperationResult(schedulerReconfigured, i18nHelper.getText("jes.scheduler.reconfigured.state", schedulerReconfigured));
    }

    /**
     * Check if the named thread group is running.
     * @param name The group to check
     * @return true if the thread group is running, false otherwise.
     */
    public boolean isThreadGroupRunning(final String name) {
        final ThreadGroup threadGroup = threadGroupUtils.getThreadGroupByName(name);
        return threadGroup != null;
    }

    /**
     * Pause the caesium scheduler. Destroy any extra thread groups that have been started.
     * We attempt to only have one extra thread group.
     * @return a message indicating the success or failure of the operation.
     */
    public OperationResult pauseScheduler() {
        try {
            final CaesiumSchedulerService caesiumSchedulerService = ComponentAccessor.getComponent(CaesiumSchedulerService.class);
            caesiumSchedulerService.standby();
            destroyExtraCaesiumSchedulerThreadGroup();
        } catch (Exception e) {
            log.error("Error pausing the caesium scheduler: " + e.getMessage());
        }
        return new OperationResult(true, i18nHelper.getText("jes.scheduler.paused"));
    }

    /**
     * Start the caesium scheduler. If the scheduler has been reconfigured, then we need to start the extra thread group.
     * We'll increment the thread group count to create a new thread group, then manipulate some internal variables
     * in Caesium to get it to start new threads. This isn't idea, but it's where we are with the lack of API or
     * ability to replace the SchedulerConfiguration object on startup.
     * @return a message indicating the success or failure of the operation.
     */
    public OperationResult startScheduler() {
        try {
            final CaesiumSchedulerService caesiumSchedulerService = ComponentAccessor.getComponent(CaesiumSchedulerService.class);
            if (schedulerReconfigured) {
                if (!isThreadGroupRunning(getThreadGroupName())) {
                    Field field = caesiumSchedulerService.getClass().getSuperclass().getDeclaredField("started");
                    field.setAccessible(true);
                    AtomicBoolean schedulerStarted = (AtomicBoolean) field.get(caesiumSchedulerService);
                    schedulerStarted.set(false);
                }
            }

            caesiumSchedulerService.start();
        } catch (Exception e) {
            log.error("Error starting the caesium scheduler: " + e.getMessage());
        }

        return new OperationResult(isThreadGroupRunning(getThreadGroupName()), i18nHelper.getText("jes.scheduler.started"));
    }

    /**
     * Destroy the thread group by name. This is unsafe. We invoke the deprecated method Thread.stop() to kill the threads.
     * @param threadGroupName The name of the thread group to destroy.
     * @return a message indicating the success or failure of the operation.
     */
    public OperationResult destroyThreadGroupByName(final String threadGroupName) {
        log.debug("Destroying the extra thread group in the Caesium scheduler:" + threadGroupName);
        try {
            if (this.schedulerReconfigured && isThreadGroupRunning(threadGroupName)) {
                destroyThreadGroup(threadGroupName);
                log.debug("The extra thread group has been destroyed successfully.");
            } else {
                log.warn("Cannot destroy the extra thread group in the Caesium scheduler because the scheduler has not" +
                        " been re-configured or the extra thread group has not been started.");
            }
        } catch (Exception e) {
            log.error("Error destroying the extra thread group: " + e.getMessage());
        }

        return new OperationResult(isThreadGroupRunning(threadGroupName), i18nHelper.getText("jes.thread.group.destroyed", threadGroupName));
    }

    /**
     * Destroy the known extra thread group in the caesium scheduler.
     */
    public void destroyExtraCaesiumSchedulerThreadGroup() {
        destroyThreadGroupByName(getThreadGroupName());
    }

    /**
     * Has the scheduler been reconfigured?
     * @return true if the scheduler has been reconfigured, false otherwise.
     */
    public boolean isSchedulerReconfigured() {
        return schedulerReconfigured;
    }

    /**
     * Get the current configuration of the caesium scheduler.
     * @return a string with the configuration details.
     */
    public String getCaesiumConfig() {
        String configurationDetails = null;
        try {
            final CaesiumSchedulerService caesiumSchedulerService = ComponentAccessor.getComponent(CaesiumSchedulerService.class);
            Field field = caesiumSchedulerService.getClass().getSuperclass().getDeclaredField("config");
            field.setAccessible(true);
            final CaesiumSchedulerConfiguration config = (CaesiumSchedulerConfiguration) field.get(caesiumSchedulerService);
            configurationDetails = "CaesiumSchedulerConfiguration details:{" +
                    ", refreshClusteredJobsIntervalInMinutes=" + config.refreshClusteredJobsIntervalInMinutes() +
                    ", workerThreadCount=" + config.workerThreadCount() +
                    ", useQuartzJobDataMapMigration=" + config.useQuartzJobDataMapMigration() +
                    ", useFineGrainedSchedules=" + config.useFineGrainedSchedules() +
                    ", getDefaultTimeZone=" + config.getDefaultTimeZone() +
                    " }";
        } catch (Exception e) {
            log.error("Error getting the caesium scheduler configuration: " + e.getMessage());
        }
        return configurationDetails;
    }

    /**
     * Get the current configuration of this object and the caesium scheduler.
     * @return a CurrentConfiguration object with the configuration details.
     */
    public CurrentConfiguration getCurrentConfiguration() {
        final CaesiumSchedulerService caesiumSchedulerService = ComponentAccessor.getComponent(CaesiumSchedulerService.class);
        final CurrentConfiguration currentConfiguration = new CurrentConfiguration();
        currentConfiguration.setExtraThreadsToConfigure(enhancedConfig.workerThreadCount());

        if (schedulerReconfigured &&
                isThreadGroupRunning(getThreadGroupName()) &&
                !defaultThreadGroup.equals(getThreadGroupName())) {
            currentConfiguration.setExtraThreadsRunning(enhancedConfig.workerThreadCount());
        } else {
            currentConfiguration.setExtraThreadsRunning(0);
        }

        currentConfiguration.setSchedulerReconfigured(schedulerReconfigured);
        currentConfiguration.setThreadGroupName(getThreadGroupName());
        if (currentConfiguration.getThreadGroupName().equals(defaultThreadGroup)) {
            currentConfiguration.setThreadGroupName(i18nHelper.getText("jes.thread.group.not.started"));
        } else if (!isThreadGroupRunning(getThreadGroupName())) {
            currentConfiguration.setThreadGroupName(i18nHelper.getText("jes.thread.group.terminated", getThreadGroupName()));
        }
        currentConfiguration.setExtraThreadGroupStarted(isThreadGroupRunning(getThreadGroupName()) && !defaultThreadGroup.equals(getThreadGroupName()));
        currentConfiguration.setDefaultThreadGroup(defaultThreadGroup);
        currentConfiguration.setSchedulerRunning(caesiumSchedulerService.getState().equals(LifecycleAwareSchedulerService.State.STARTED));

        return currentConfiguration;
    }

    private String getThreadGroupName() {
        return "Caesium-" + threadGroupCount.get();
    }

    private void destroyThreadGroup(final String threadGroupName) {
        try {
            final ThreadGroup threadGroup = threadGroupUtils.getThreadGroupByName(threadGroupName);
            if (threadGroup != null) {
                Thread[] threads = new Thread[threadGroup.activeCount()];
                threadGroup.enumerate(threads);
                for (Thread thread : threads) {
                    try {
                        thread.stop();
                    } catch (Exception e) {
                        log.error("Error interrupting the thread: " + e.getMessage());
                        log.error("Thread name: " + thread.getName());
                    }
                }
                threadGroup.destroy();
            }
        } catch (Exception e) {
            log.error("Error destroying the thread group: " + e.getMessage());
        }
    }
}