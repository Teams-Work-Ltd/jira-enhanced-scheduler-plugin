Enhanced Jira scheduler
==============
This plugin modifies the existing Atlassian scheduler plugin in Jira/Jira Software. (https://bitbucket.org/atlassian/atlassian-scheduler/src/master/)

This introduces some new capabilities to configure the Jira scheduler. 
The main feature is the ability to configure the number of threads for the scheduler. 
This can be useful if you have a large number of jobs that need to be executed in parallel. 

You can also now start and stop the scheduler and view some limited information about the thread group state.

The existing scheduler is hard limited to 4 threads. This will allow you to either use the REST api to reconfigure
the number of threads, or there is an admin section in the Jira administration area to do this also.

Note: this is additive. So, the original 4 threads will still be used, but you can add more threads to this. 
Unfortunately due to limitations in the scheduler code, managing these threads is nigh-on impossible without
creating a new version of the scheduler. This means that the only way to reset the threads is to restart the Jira instance/node.

Note - this also works on a per node in cluster basis - i.e. if you start extra threads on one node in a cluster, they will be pinned to that node.
To add extra threads in other nodes, you need to go directly to that node, bypassing the load balancer.


Going deeper
---------------

The main entry point to this is the SchedulerConfigurator class.
By default, this will set a value in ApplicationProperties that is then used as the value to configure the number 
of threads used in the scheduler. This is set to 2, to keep things safe. 

If using this, we recommend increasing the number gradually and testing the impact on your Jira instance.
 
This class can be used in the following way:
 * Call the configureThreadCount method to set the number of threads for the scheduler, if needed.
 * Call the replaceSchedulerConfiguration method to switch the original configuration with the enhanced version.
 * Call the pauseScheduler method to pause the scheduler.
 * Call the startSchedulerWithExtraThreadGroup method to start the scheduler with an extra thread group.

There are also convenience methods to start/pause the scheduler.
 * Note, this will create at least one extra thread group for the scheduler.

There are also  methods to make an attempt at destroying these but, we don't have good hooks into the thread management
aspect of the scheduler. We'd advise not using these as it is highly likely your Jira instance will become unstable.

We use some reflection under the hood to enable this. This is not ideal, but it is the only way to get at the scheduler
thread pool without an API to do so.


Limitations
-----
As mentioned, we add threads, not replace them. So the original 4 threads will still be used. Everything else is additive.

Ideally, we'd be able to replace the defined thread count at startup time, however, 
the ability to do any work in this area was limited in Jira 8 and limited further in Jira 9.
https://developer.atlassian.com/server/jira/platform/picocontainer-and-jira/

So, this is what we're left with.


This is a Atlassian plugin
======


If you want to work with it, feel free. Here are the SDK commands you'll use immediately:

* atlas-run   -- installs this plugin into the product and starts it on localhost
* atlas-debug -- same as atlas-run, but allows a debugger to attach at port 5005
* atlas-help  -- prints description for all commands in the SDK

Full documentation is always available at:

https://developer.atlassian.com/display/DOCS/Introduction+to+the+Atlassian+Plugin+SDK
