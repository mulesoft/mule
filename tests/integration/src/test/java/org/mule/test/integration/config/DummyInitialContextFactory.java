/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.config;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationCallback;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.object.ObjectFactory;
import org.mule.api.service.Service;
import org.mule.component.simple.EchoComponent;
import org.mule.jndi.SimpleContext;
import org.mule.module.management.agent.Log4jAgent;
import org.mule.module.xml.transformer.XmlToObject;
import org.mule.tck.MuleTestUtils;
import org.mule.transport.vm.VMConnector;

/**
 * A dummy property factory for creating a Jndi context
 */
public class DummyInitialContextFactory implements ObjectFactory
{
    private MuleContext muleContext;

    public DummyInitialContextFactory(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public Object getInstance(MuleContext muleContext) throws Exception
    {
        SimpleContext c = new SimpleContext();
        c.bind("vmConnector", new VMConnector(muleContext));
        c.bind("endpointRef", "vm://my.object");
        c.bind("Log4JAgent", new Log4jAgent());
        c.bind("XmlToObject", new XmlToObject());
        Service d = MuleTestUtils.getTestService("EchoUMO", EchoComponent.class, this.muleContext);
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

    public void release(Object arg0)
    {
        // do nothing
    }

    public Class<?> getObjectClass()
    {
        throw new UnsupportedOperationException();
    }

    public void addObjectInitialisationCallback(InitialisationCallback callback)
    {
        throw new UnsupportedOperationException();        
    }

    public boolean isSingleton()
    {
        return false;
    }

    public boolean isExternallyManagedLifecycle()
    {
        return false;
    }

    public boolean isAutoWireObject()
    {
        return false;
    }
}
