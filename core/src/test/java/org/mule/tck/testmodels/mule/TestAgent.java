/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.api.MuleException;
import org.mule.api.agent.Agent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;

/**
 * Mock agent
 */
public class TestAgent implements Agent
{

    private String frobbit;

    public String getName()
    {
        return "Test Agent";
    }

    public void setName(String name)
    {
        // nothing to do
    }

    public String getDescription()
    {
        return "Test JMX Agent";
    }

    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        return LifecycleTransitionResult.OK;
    }

    public LifecycleTransitionResult start() throws MuleException
    {
        return LifecycleTransitionResult.OK;
    }

    public LifecycleTransitionResult stop() throws MuleException
    {
        return LifecycleTransitionResult.OK;
    }

    public void dispose()
    {
        // nothing to do
    }

    public void registered()
    {
        // nothing to do
    }

    public void unregistered()
    {
        // nothing to do
    }

    public String getFrobbit()
    {
        return frobbit;
    }

    public void setFrobbit(String frobbit)
    {
        this.frobbit = frobbit;
    }
}
