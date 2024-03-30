package com.teamswork.scheduler.service;

import javax.inject.Named;

@Named
public class ThreadGroupUtils {
    /**
     * Get thread group by name
     *
     * @param name thread group name
     * @return thread group or null
     */
    public ThreadGroup getThreadGroupByName(String name) {
        ThreadGroup root = Thread.currentThread().getThreadGroup().getParent(); // Get the root thread group

        ThreadGroup result = null; // Initialize the result to null

        // Iterate through all active thread groups
        ThreadGroup[] groups = new ThreadGroup[root.activeGroupCount()];
        root.enumerate(groups, true);
        for (ThreadGroup group : groups) {
            if (group != null && group.getName().equals(name)) {
                result = group; // Found the thread group with the specified name
                break;
            }
        }

        return result; // Return the found thread group, or null if not found
    }
}
