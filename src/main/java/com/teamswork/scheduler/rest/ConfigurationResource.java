package com.teamswork.scheduler.rest;

import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.teamswork.scheduler.component.SchedulerConfigurator;
import com.teamswork.scheduler.model.CurrentConfiguration;
import com.teamswork.scheduler.model.OperationResult;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST resource for working with the enhanced scheduler configuration.
 */
@Path("/scheduler")
@Consumes(MediaType.APPLICATION_JSON)
@Produces({MediaType.APPLICATION_JSON})
public class ConfigurationResource {

    private final SchedulerConfigurator schedulerConfigurator;
    final I18nHelper i18nHelper;

    public ConfigurationResource(final SchedulerConfigurator schedulerConfigurator,
                                 @ComponentImport final I18nHelper i18nHelper) {
        this.schedulerConfigurator = schedulerConfigurator;
        this.i18nHelper = i18nHelper;
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public Response reconfigureSchedulerWithThreadCount(final Integer extraThreadsToConfigure) {
        OperationResult result = schedulerConfigurator.configureThreadCount(extraThreadsToConfigure);
        if (!result.isSuccess()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }

        result = schedulerConfigurator.replaceCaesiumConfiguration();
        return Response.ok(result).build();
    }

    @POST
    @Path("/reconfigure")
    @Produces({MediaType.APPLICATION_JSON})
    public Response reconfigureScheduler() {
        final OperationResult result = schedulerConfigurator.replaceCaesiumConfiguration();
        return Response.ok(result).build();
    }

    @POST
    @Path("/configureThreadCount")
    @Produces({MediaType.APPLICATION_JSON})
    public Response setThreadCount(final Integer threads) {
        final OperationResult result = schedulerConfigurator.configureThreadCount(threads);
        return Response.ok(result).build();
    }

    @DELETE
    @Path("/destroyThreadGroup")
    @Produces({MediaType.APPLICATION_JSON})
    public Response destroyThreadGroup(final String threadGroupName) {
        final OperationResult result = schedulerConfigurator.destroyThreadGroupByName(threadGroupName);
        return Response.ok(result).build();
    }

    @POST
    @Path("/start")
    @Produces({MediaType.APPLICATION_JSON})
    public Response startScheduler() {
        final OperationResult result = schedulerConfigurator.startScheduler();
        return Response.ok(result).build();
    }

    @POST
    @Path("/pause")
    @Produces({MediaType.APPLICATION_JSON})
    public Response pauseScheduler() {
        final OperationResult result = schedulerConfigurator.pauseScheduler();
        return Response.ok(result).build();
    }

    @GET
    @Path("/config")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCaesiumConfig() {
        final String configurationDetails = schedulerConfigurator.getCaesiumConfig();
        return Response.ok(configurationDetails).build();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCurrentConfig() {
        final CurrentConfiguration config = schedulerConfigurator.getCurrentConfiguration();
        return Response.ok(config).build();
    }
}
