/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl;

import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOAgent;

/**
 * Impleents common methods for all Agents. Improtantly, the Management context is made available to Agents that
 * extend this.
 */
public abstract class AbstractAgent implements UMOAgent, ManagementContextAware
{

    protected UMOManagementContext managementContext;

    protected String name;

    protected AbstractAgent(String name)
    {
        this.name = name;
    }

    public final String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return name;
    }


    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
    }

    public void initialise() throws InitialisationException
    {
        //template
    }

}
