package com.teamswork.scheduler.component;

import com.atlassian.jira.component.ComponentAccessor;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.caesium.impl.CaesiumSchedulerService;
import com.atlassian.scheduler.caesium.spi.CaesiumSchedulerConfiguration;
import com.atlassian.scheduler.config.JobRunnerKey;
import com.atlassian.scheduler.core.LifecycleAwareSchedulerService;
import com.teamswork.scheduler.model.CurrentConfiguration;
import com.teamswork.scheduler.model.OperationResult;
import com.teamswork.scheduler.service.ThreadGroupUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.teamswork.scheduler.utils.StringUtils.extractThreadGroupName;
import static com.teamswork.scheduler.component.SchedulerInitializer.SCHEDULER_THREADS_KEY;

/**
 * Allows for configuration of the Jira Caesium scheduler.
 * This class can be used in the following way:
 * 1 - Call the configureThreadCount method to set the number of threads for the scheduler, if needed.
 * 2 - Call the replaceSchedulerConfiguration method to switch the original configuration with the enhanced version.
 * 3 - Call the pauseScheduler method to pause the scheduler.
 * 4-  Call the startSchedulerWithExtraThreadGroup method to start the scheduler with an extra thread group.
 * There are convenience methods to start/pause the scheduler.
 * Threads groups can only be attempted to be destroyed when the scheduler is paused.
 * Invoking 'startSchedulerWithExtraThreadGroup' multiple times will create multiple thread groups.
 * Note, this will create at least one extra thread group for the scheduler.
 * There are methods to make an attempt at destroying these but, we don't have good hooks into the thread management
 * aspect of the scheduler. We'd advise not using these as it is highly likely your Jira instance will become unstable.
 */
@Named
@SuppressWarnings("unused")
public class SchedulerConfigurator {
    private static final Logger log = LoggerFactory.getLogger(SchedulerConfigurator.class);
    private static final String defaultThreadGroup = "Caesium-1";
    private static final String defaultThreadGroupPrefix = "Caesium-";
    public static final String DELIMITER = ":";
    private static final String STARTED = " Started";
    private static final String PENDING = " Pending";
    private static final String PAUSED = " Paused";
    private static final String DESTROYED = " Destroyed";

    private final CaesiumSchedulerConfiguration enhancedConfig;
    private final ApplicationProperties applicationProperties;
    private final ThreadGroupUtils threadGroupUtils;
    final I18nHelper i18nHelper;
    private boolean schedulerReconfigured = false;

    // This is a map of thread group names to states.
    private final ConcurrentHashMap<String, String> threadGroupState = new ConcurrentHashMap<>();

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
     * This sets the schedulerReconfigured flag to true if the configuration was replaced successfully and
     * also increments the current thread group count.
     * @return OperationResult with the details of the configuration.
     */
    public OperationResult replaceSchedulerConfiguration() {
        log.debug("Re-configuring the Jira caesium scheduler.");
        log.debug("Unregistering the original configuration of the caesium scheduler and the scheduler.");
        try {
            final CaesiumSchedulerService caesiumSchedulerService = ComponentAccessor.getComponent(CaesiumSchedulerService.class);
            final Field field = caesiumSchedulerService.getClass().getSuperclass().getDeclaredField("config");
            field.setAccessible(true);

            if (isJava8()) {
                // Java 8 approach (uses "modifiers" field, allowed in Java 8)
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            } else {
                // Java 9+ approach (uses VarHandle, safe in Java 9+)
                VarHandle modifiersVarHandle = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup())
                        .findVarHandle(Field.class, "modifiers", int.class);
                modifiersVarHandle.set(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            }

            field.set(caesiumSchedulerService, enhancedConfig);
            schedulerReconfigured = true;
            threadGroupState.put(getThreadGroupName(), PENDING);
        } catch (Exception e) {
            log.error("Error re-configuring the caesium scheduler", e);
            schedulerReconfigured = false;
            return new OperationResult(false, i18nHelper.getText("jes.scheduler.failed.to.configure"));
        }

        if (schedulerReconfigured) {
            log.debug("Scheduler re-configured successfully.");
        }

        return new OperationResult(schedulerReconfigured, i18nHelper.getText("jes.scheduler.reconfigured.state", schedulerReconfigured));
    }

    /**
     * Check if the named thread group exists.
     * @param name The group to check
     * @return true if the thread group exists, false otherwise.
     */
    public boolean threadGroupExists(final String name) {
        final ThreadGroup threadGroup = threadGroupUtils.getThreadGroupByName(name);
        return threadGroup != null && threadGroup.activeCount() > 0;
    }

    /**
     * Pause the scheduler.
     * @return a message indicating the success or failure of the operation.
     */
    public OperationResult pauseScheduler() {
        try {
            final CaesiumSchedulerService caesiumSchedulerService = ComponentAccessor.getComponent(CaesiumSchedulerService.class);
            if (caesiumSchedulerService.getState().equals(LifecycleAwareSchedulerService.State.STARTED)) {
                log.debug("Pausing the caesium scheduler.");
                caesiumSchedulerService.standby();
                if (threadGroupExists(getThreadGroupName())) {
                    threadGroupState.put(getThreadGroupName(), PAUSED);
                }

                Set<JobRunnerKey> keys= caesiumSchedulerService.getJobRunnerKeysForAllScheduledJobs();
                for (JobRunnerKey key : keys) {
                    JobRunner jobRunner = caesiumSchedulerService.getJobRunner(key);
                    log.debug("JobRunner: {}", jobRunner);
                }
            }
        } catch (Exception e) {
            log.error("Error pausing the caesium scheduler: {}", e.getMessage());
        }
        return new OperationResult(true, i18nHelper.getText("jes.scheduler.paused"));
    }

    /**
     * Start the scheduler. This will resume processing with the default thread group. If any extra threads
     * were started, this will include those in the processing
     * @return a message indicating the success or failure of the operation.
     */
    public OperationResult startScheduler() {
        try {
            final CaesiumSchedulerService caesiumSchedulerService = ComponentAccessor.getComponent(CaesiumSchedulerService.class);
            if (caesiumSchedulerService.getState().equals(LifecycleAwareSchedulerService.State.STANDBY)) {
                log.debug("Starting the caesium scheduler.");
                caesiumSchedulerService.start();
                if (threadGroupExists(getThreadGroupName())) {
                    threadGroupState.put(getThreadGroupName(), STARTED);
                }

            }
        } catch (Exception e) {
            log.error("Error starting the caesium scheduler: {}", e.getMessage());
            return new OperationResult(false, i18nHelper.getText("jes.caesium.scheduler.failed.to.start"));
        }

        return new OperationResult(threadGroupExists(getThreadGroupName()), i18nHelper.getText("jes.scheduler.started"));
    }

    /**
     * Start the scheduler. If the scheduler has been reconfigured, then we need to start the extra thread group.
     * We'll then manipulate some internal variables in Caesium to get it to start new threads.
     * This isn't ideal, but it's where we are with the lack of API or ability to replace the SchedulerConfiguration
     * object on startup.
     * @return a message indicating the success or failure of the operation.
     */
    public OperationResult startSchedulerWithExtraThreadGroup() {
        try {
            final CaesiumSchedulerService service = ComponentAccessor.getComponent(CaesiumSchedulerService.class);
            if (schedulerReconfigured) {
                service.standby();
                Field field = service.getClass().getSuperclass().getDeclaredField("started");
                field.setAccessible(true);
                AtomicBoolean schedulerStarted = (AtomicBoolean) field.get(service);
                schedulerStarted.set(false);
            } else {
                log.info("The scheduler has not been reconfigured successfully.");
                return new OperationResult(false, i18nHelper.getText("jes.scheduler.failed.to.reconfigure"));            }

            service.start();
            if (threadGroupExists(getThreadGroupName())) {
                threadGroupState.put(getThreadGroupName(), STARTED);
            }
        } catch (Exception e) {
            log.error("Error starting the caesium scheduler: " + e.getMessage());
            return new OperationResult(false, i18nHelper.getText("jes.scheduler.failed.to.start"));
        }

        return new OperationResult(threadGroupExists(getThreadGroupName()), i18nHelper.getText("jes.scheduler.started"));
    }

    /**
     * Has the scheduler been reconfigured?
     * @return true if the scheduler has been reconfigured, false otherwise.
     */
    public boolean isSchedulerReconfigured() {
        return schedulerReconfigured;
    }

    /**
     * Get the current configuration of the scheduler.
     * @return a string with the configuration details.
     */
    public String getSchedulerConfig() {
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
            log.error("Error getting the caesium scheduler configuration: {}", e.getMessage());
        }
        return configurationDetails;
    }

    /**
     * Get the current configuration of this object and the scheduler.
     * @return a CurrentConfiguration object with the configuration details.
     */
    public CurrentConfiguration getCurrentConfiguration() {
        final CaesiumSchedulerService caesiumSchedulerService = ComponentAccessor.getComponent(CaesiumSchedulerService.class);
        final CurrentConfiguration currentConfiguration = new CurrentConfiguration();
        currentConfiguration.setExtraThreadsToConfigure(enhancedConfig.workerThreadCount());

        if (schedulerReconfigured &&
                threadGroupExists(getThreadGroupName()) &&
                !defaultThreadGroup.equals(getThreadGroupName())) {
            currentConfiguration.setExtraThreadsRunning(enhancedConfig.workerThreadCount());
        } else {
            currentConfiguration.setExtraThreadsRunning(0);
        }

        currentConfiguration.setSchedulerReconfigured(schedulerReconfigured);
        currentConfiguration.setThreadGroupName(getThreadGroupName());
        if (currentConfiguration.getThreadGroupName().equals(defaultThreadGroup)) {
            currentConfiguration.setThreadGroupName(i18nHelper.getText("jes.thread.group.not.started"));
        } else {
            currentConfiguration.setThreadGroupName(getThreadGroupName() + DELIMITER + threadGroupState.get(getThreadGroupName()));
        }
        currentConfiguration.setExtraThreadGroupStarted(threadGroupExists(getThreadGroupName()) && !defaultThreadGroup.equals(getThreadGroupName()));
        currentConfiguration.setDefaultThreadGroup(defaultThreadGroup);
        currentConfiguration.setSchedulerRunning(caesiumSchedulerService.getState().equals(LifecycleAwareSchedulerService.State.STARTED));

        return currentConfiguration;
    }

    /**
     * Destroy a thread group by name. This is unsafe.
     * Recommend not using this as it is highly likely your Jira instance will become unstable.
     * We invoke the deprecated method Thread.stop() to kill the threads.
     * The scheduler will be paused before the thread group is destroyed, then restarted.
     * @param threadGroupName The name of the thread group to destroy.
     * @return a message indicating the success or failure of the operation.
     */
    public OperationResult destroyThreadGroupByName(final String threadGroupName) {
        log.debug("Destroying the extra thread group in the Caesium scheduler:{}", threadGroupName);
        pauseScheduler();
        final String actualThreadGroupName = extractThreadGroupName(threadGroupName);
        try {
            if (this.schedulerReconfigured && threadGroupExists(actualThreadGroupName)) {
                threadGroupState.put(actualThreadGroupName, DESTROYED);
                destroyThreadGroup(actualThreadGroupName);
                log.debug("The thread group named: {} has been destroyed successfully.", actualThreadGroupName);
            } else {
                log.warn("Cannot destroy the extra thread group in the Caesium scheduler because the scheduler has not" +
                        " been re-configured or the extra thread group has not been started.");
            }
        } catch (Exception e) {
            log.error("Error destroying the extra thread group: {}", e.getMessage());
        }

        return new OperationResult(threadGroupExists(threadGroupName), i18nHelper.getText("jes.thread.group.destroyed", threadGroupName));
    }

    /**
     * Destroy the known extra thread group in the scheduler.
     * Recommend not using this as it is highly likely your Jira instance will become unstable.
     */
    public void destroyExtraSchedulerThreadGroup() {
        destroyThreadGroupByName(getThreadGroupName());
    }

    /**
     * Destroy any extra thread groups in the scheduler. Recommend not using this as it is highly likely your
     * Jira instance will become unstable.
     * This will attempt to destroy any groups with the name 'Caesium-' followed by a number greater than 1
     * and less or equal to the value of threadGroupCount.
     */
    public OperationResult destroyAllExtraSchedulerThreadGroups() {
        for (int i = 2; i <= getThreadGroupCount(); i++) {
            destroyThreadGroupByName(defaultThreadGroupPrefix + i);
        }
        return new OperationResult(true, i18nHelper.getText("jes.all.thread.groups.destroyed"));
    }

    private String getThreadGroupName() {
        return defaultThreadGroupPrefix + getThreadGroupCount();
    }

    private int getThreadGroupCount() {
        int threadGroupCount = 1;
        try {
            Class<?> accessHelperClass = Class.forName("com.atlassian.scheduler.caesium.impl.WorkerThreadFactory");
            Field field = accessHelperClass.getDeclaredField("FACTORY_COUNTER");
            field.setAccessible(true);
            AtomicInteger threadCounterValue = (AtomicInteger) field.get(null);
            threadGroupCount = threadCounterValue.get();
        } catch (Exception e) {
            log.error("Error getting the thread count: {}", e.getMessage());
        }
        return threadGroupCount;
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
                        log.error("Error interrupting the thread: {}", e.getMessage());
                        log.error("Thread name: {}", thread.getName());
                    }
                }
                threadGroup.destroy();
            }
        } catch (Exception e) {
            log.error("Error destroying the thread group: {}", e.getMessage());
        }
    }

    private boolean isJava8() {
        String version = System.getProperty("java.version");
        return version.startsWith("1.8");
    }
}