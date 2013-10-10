/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.registry;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.construct.Flow;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestConnector;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RegistryBrokerTestCase extends AbstractMuleContextTestCase
{

    private String tracker;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        tracker = new String();
    }

    @Override
    protected boolean isStartContext()
    {
        return false;
    }

    @Test
    public void testCrossRegistryLifecycleOrder() throws MuleException
    {

        TransientRegistry reg1 = new TransientRegistry(muleContext);
        reg1.initialise();
        TransientRegistry reg2 = new TransientRegistry(muleContext);
        reg2.initialise();

        reg1.registerObject("conn", new LifecycleTrackerConnector("conn", muleContext));
        reg2.registerObject("conn2", new LifecycleTrackerConnector("conn2", muleContext));
        reg1.registerObject("flow", new LifecycleTrackerFlow("flow", muleContext));
        reg2.registerObject("flow2", new LifecycleTrackerFlow("flow2", muleContext));

        muleContext.addRegistry(reg1);
        muleContext.addRegistry(reg2);

        muleContext.start();

        // Both connectors are started before either flow
        assertEquals("conn2-start conn-start flow2-start flow-start ", tracker.toString());

        tracker = new String();
        muleContext.stop();

        // Both services are stopped before either connector
        assertEquals("flow2-stop flow-stop conn2-stop conn-stop ", tracker);
    }

    class LifecycleTrackerConnector extends TestConnector
    {

        public LifecycleTrackerConnector(String name, MuleContext context)
        {
            super(context);
            this.name = name;
        }

        @Override
        protected void doStart()
        {
            super.doStart();
            tracker += name + "-start ";
        }

        @Override
        protected void doStop()
        {
            super.doStop();
            tracker += name + "-stop ";
        }
    }

    class LifecycleTrackerFlow extends Flow
    {

        public LifecycleTrackerFlow(String name, MuleContext muleContext)
        {
            super(name, muleContext);
        }

        @Override
        protected void doStart() throws MuleException
        {
            super.doStart();
            tracker += name + "-start ";
        }

        @Override
        protected void doStop() throws MuleException
        {
            super.doStop();
            tracker += name + "-stop ";
        }
    }

}
