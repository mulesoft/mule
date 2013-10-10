/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
