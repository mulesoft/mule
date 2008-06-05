/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.agent;

import org.mule.api.MuleException;
import org.mule.api.agent.Agent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.ClassUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MockAgent extends Object implements Agent
{
    private String name;
    private List dependencies = Collections.EMPTY_LIST;
    
    public MockAgent()
    {
        super();
    }
    
    public MockAgent(Class[] classes)
    {
        super();
        dependencies = Arrays.asList(classes);
    }

    public List getDependentAgents()
    {
        return dependencies;
    }

    public String getDescription()
    {
        return ClassUtils.getSimpleName(this.getClass());
    }

    public void registered()
    {
        // nothing to do
    }

    public void unregistered()
    {
        // nothing to do
    }

    public void initialise() throws InitialisationException
    {
        // nothing to do
    }

    public void start() throws MuleException
    {
        // nothing to do
    }

    public void stop() throws MuleException
    {
        // nothing to do
    }

    public void dispose()
    {
        // nothing to do
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

}


