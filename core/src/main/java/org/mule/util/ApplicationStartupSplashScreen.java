/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
                                 System.getProperty("file.encoding"),
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
