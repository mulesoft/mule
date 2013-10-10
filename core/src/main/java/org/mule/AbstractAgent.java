/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.agent.Agent;
import org.mule.api.context.MuleContextAware;

/**
 * Implements common methods for all Agents. Importantly, the MuleContext is made available to Agents that
 * extend this.
 */
public abstract class AbstractAgent implements Agent, MuleContextAware
{

    protected MuleContext muleContext;

    protected String name;

    protected AbstractAgent(String name)
    {
        this.name = name;
    }

    public final String getName()
    {
        return name;
    }

    public final void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return name;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    /**
     * Quietly unregister ourselves.
     */
    protected void unregisterMeQuietly()
    {
        try
        {
            // remove the agent from the list, it's not functional
            muleContext.getRegistry().unregisterAgent(this.getName());
        }
        catch (MuleException e)
        {
            // not interested, really
        }
    }
}
