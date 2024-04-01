package com.teamswork.scheduler.utils;

import static com.teamswork.scheduler.component.SchedulerConfigurator.DELIMITER;

public class StringUtils {

    public static String extractThreadGroupName(String threadGroupName) {
        if (threadGroupName == null) {
            return null;
        }
        if (threadGroupName.isEmpty()) {
            return threadGroupName;
        }

        if (threadGroupName.lastIndexOf(DELIMITER) == -1){
            return threadGroupName;
        }
        return threadGroupName.substring(0, threadGroupName.lastIndexOf(":")).trim();
    }
}
