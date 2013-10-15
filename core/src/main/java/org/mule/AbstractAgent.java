/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
