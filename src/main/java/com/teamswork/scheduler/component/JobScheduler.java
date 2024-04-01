package com.teamswork.scheduler.component;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.scheduler.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
public class JobScheduler extends PluginStateListener {
    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);

    private final SchedulerService schedulerService;

    public JobScheduler(@ComponentImport final EventPublisher eventPublisher,
                        @ComponentImport final SchedulerService schedulerService) {
        super(eventPublisher);
        this.schedulerService = schedulerService;
    }

    /**
     * Use this method to register job runners and schedule periodic jobs.
     */
    @Override
    protected void onAppStart() {
        try{

        } catch( final Exception e){
            log.error("Error registering or running jobs." + e.getMessage());
        }
    }

    /**
     * Use this method to unregister job runners.
     */
    @Override
    protected void onAppShutdown() {
        try {
            log.debug("Unregistered jobs");
        } catch (final Throwable t) {
            log.error("Unable to unregister job runners ", t);
        }
    }
}