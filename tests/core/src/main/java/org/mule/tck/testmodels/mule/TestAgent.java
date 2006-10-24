/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOAgent;

/**
 * Mock agent
 */
public class TestAgent implements UMOAgent
{
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

    public void initialise() throws InitialisationException
    {
        // nothing to do
    }

    public void start() throws UMOException
    {
        // nothing to do
    }

    public void stop() throws UMOException
    {
        // nothing to do
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
}
