package com.teamswork.scheduler.component;

import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

/**
 * Needed to import the IsAdmin condition for web items.
 */
@SuppressWarnings("unused")
public class Imports {
    @ComponentImport
    private GlobalPermissionManager globalPermissionManager;
}
