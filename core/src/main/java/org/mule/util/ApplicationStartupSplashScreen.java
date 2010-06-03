/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.api.MuleContext;
import org.mule.api.agent.Agent;
import org.mule.config.i18n.CoreMessages;

import java.util.Collection;
import java.util.Iterator;

public class ApplicationStartupSplashScreen extends SplashScreen
{
    protected void doHeader(MuleContext context)
    {
        if (context.getStartDate() > 0)
        {
            // TODO not sure this one makes sense for an app
            header.add(CoreMessages.serverStartedAt(context.getStartDate()).getMessage());
        }
        header.add("Application: " + context.getConfiguration().getId());

        header.add(" ");
    }
   
    protected void doFooter(MuleContext context)
    {
        // Mule Agents
        if (!body.isEmpty())
        {
            footer.add(" ");
        }
        //List agents
        Collection agents = context.getRegistry().lookupObjects(Agent.class);
        if (agents.size() == 0)
        {
            footer.add(CoreMessages.agentsRunning().getMessage() + " "
                    + CoreMessages.none().getMessage());
        }
        else
        {
            footer.add(CoreMessages.agentsRunning().getMessage());
            Agent agent;
            for (Iterator iterator = agents.iterator(); iterator.hasNext();)
            {
                agent = (Agent) iterator.next();
                footer.add("  " + agent.getDescription());
            }
        }
    }    
}
