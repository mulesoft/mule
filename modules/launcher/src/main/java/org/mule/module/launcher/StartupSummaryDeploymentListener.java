/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Prints application status summary table on Mule startup.
 */
public class StartupSummaryDeploymentListener implements StartupListener
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
