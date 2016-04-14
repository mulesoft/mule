/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.api.MuleContext;
import org.mule.api.agent.Agent;
import org.mule.config.i18n.CoreMessages;

import java.util.Collection;
import java.util.StringTokenizer;

public class ApplicationStartupSplashScreen extends SplashScreen
{
    @Override
    protected void doHeader(MuleContext context)
    {
        header.add("Application: " + context.getConfiguration().getId());
        header.add(String.format("OS encoding: %s, Mule encoding: %s",
                                 SystemUtils.FILE_SEPARATOR,
                                 context.getConfiguration().getDefaultEncoding()));
        header.add(" ");
    }

    @Override
    protected void doFooter(MuleContext context)
    {
        // Mule Agents
        if (!body.isEmpty())
        {
            footer.add(" ");
        }
        //List agents
        Collection<Agent> agents = context.getRegistry().lookupObjects(Agent.class);
        if (agents.size() == 0)
        {
            footer.add(CoreMessages.agentsRunning().getMessage() + " "
                    + CoreMessages.none().getMessage());
        }
        else
        {
            footer.add(CoreMessages.agentsRunning().getMessage());
            for (Agent agent : agents)
            {
                String description = agent.getDescription();
                if (description.startsWith("'''"))
                {
                    description = description.substring("'''".length());
                    // handle multiline descriptions better
                    for (StringTokenizer st = new StringTokenizer(description, String.format("%n")); st.hasMoreTokens();)
                    {
                        footer.add("  " + st.nextToken());
                    }
                }
                else
                {
                    footer.add("  " + description);
                }
            }
        }
    }
}
