/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.config;

import org.mule.MuleServer;
import org.mule.components.simple.EchoComponent;
import org.mule.impl.jndi.SimpleContext;
import org.mule.management.agents.Log4jAgent;
import org.mule.providers.vm.VMConnector;
import org.mule.tck.MuleTestUtils;
import org.mule.transformers.xml.XmlToObject;
import org.mule.umo.UMOComponent;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.object.ObjectFactory;

/**
 * A dummy property factory for creating a Jndi context
 */
public class DummyInitialContextFactory implements ObjectFactory
{
    public Object getOrCreate() throws Exception
    {
        SimpleContext c = new SimpleContext();
        c.bind("vmConnector", new VMConnector());
        c.bind("endpointRef", "vm://my.object");
        c.bind("Log4JAgent", new Log4jAgent());
        c.bind("XmlToObject", new XmlToObject());
        UMOComponent d = MuleTestUtils.getTestComponent("EchoUMO", EchoComponent.class, MuleServer.getManagementContext());
        c.bind("EchoUMO", d);
        return c;
    }

    public void initialise() throws InitialisationException
    {
        // do nothing
    }

    public void dispose()
    {
        // do nothing
    }

    public Object lookup(String arg0) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    public void release(Object arg0) throws Exception
    {
        // do nothing
    }
}
