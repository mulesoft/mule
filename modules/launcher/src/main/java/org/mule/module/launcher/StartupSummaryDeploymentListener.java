/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Prints application status summary table on Mule startup.
 */
public class StartupSummaryDeploymentListener implements DeploymentService.StartupListener
{
    protected transient final Log logger = LogFactory.getLog(getClass());

    protected DeploymentStatusTracker tracker;

    public StartupSummaryDeploymentListener(DeploymentStatusTracker tracker)
    {
        this.tracker = tracker;
    }

    public void onAfterStartup()
    {
        if (!logger.isInfoEnabled())
        {
            return;
        }

        Map<String, DeploymentStatusTracker.DeploymentState> applicationStates = tracker.getDeploymentStates();

        if (applicationStates.isEmpty())
        {
            return;
        }

        SimpleLoggingTable applicationTable = new SimpleLoggingTable();
        applicationTable.addColumn("APPLICATION", 45);
        applicationTable.addColumn("STATUS", 18);

        for (String app : applicationStates.keySet())
        {
            String[] data = new String[] {app, applicationStates.get(app).toString()};
            applicationTable.addDataRow(data);
        }

        String message = String.format("%n%n%s", applicationTable);

        logger.info(message);
    }
}
