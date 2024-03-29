package com.teamswork.scheduler.rest;

import com.atlassian.jira.component.pico.ComponentManager;
import com.atlassian.scheduler.caesium.impl.CaesiumSchedulerService;
import com.atlassian.scheduler.caesium.spi.CaesiumSchedulerConfiguration;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST resource for checking the configuration.
 */
@Path("/config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces({MediaType.APPLICATION_JSON})
public class ConfigurationResource {

    public ConfigurationResource() {
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCaesiumConfig() {
        final CaesiumSchedulerConfiguration config = ComponentManager.getInstance().getComponent(CaesiumSchedulerConfiguration.class);
        final CaesiumSchedulerService service = ComponentManager.getInstance().getComponent(CaesiumSchedulerService.class);
        return Response.ok(config.toString()).build();
    }
}
