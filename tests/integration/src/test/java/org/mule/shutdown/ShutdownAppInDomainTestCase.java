/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.DomainFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that threads in pools defined in a domain do not hold references to objects of the application in their thread
 * locals.
 */
public class ShutdownAppInDomainTestCase extends DomainFunctionalTestCase
{

    private static final int PROBER_POLLING_INTERVAL = 100;
    private static final int PROBER_POLIING_TIMEOUT = 5000;
    private static final int MESSAGE_TIMEOUT = 2000;

    private static final Set<PhantomReference<MuleEvent>> requestContextRefs = new HashSet<>();

    public static class RetrieveRequestContext implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            requestContextRefs.add(new PhantomReference<MuleEvent>(RequestContext.getEvent(),
                    new ReferenceQueue<MuleEvent>()));
            return event;
        }
    }

    @Before
    public void before()
    {
        requestContextRefs.clear();
    }

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Override
    protected String getDomainConfig()
    {
        return "org/mule/shutdown/domain-with-connectors.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {
                                        new ApplicationConfig("app-with-flows", new String[] {"org/mule/shutdown/app-with-flows.xml"})
        };
    }

    @Test
    public void httpListener() throws MuleException
    {
        MuleContext muleContextForApp = getMuleContextForApp("app-with-flows");

        muleContextForApp.getClient().request("http://localhost:" + httpPort.getNumber() + "/sync", MESSAGE_TIMEOUT);
        muleContextForApp.dispose();

        assertEventsUnreferenced();
    }

    @Test
    public void httpListenerNonBlocking() throws MuleException
    {
        MuleContext muleContextForApp = getMuleContextForApp("app-with-flows");

        muleContextForApp.getClient().request("http://localhost:" + httpPort.getNumber() + "/nonBlocking", MESSAGE_TIMEOUT);
        muleContextForApp.dispose();

        assertEventsUnreferenced();
    }

    @Test
    public void httpRequest() throws MuleException
    {
        MuleContext muleContextForApp = getMuleContextForApp("app-with-flows");

        muleContextForApp.getClient().request("http://localhost:" + httpPort.getNumber() + "/request", MESSAGE_TIMEOUT);
        muleContextForApp.dispose();

        assertEventsUnreferenced();
    }

    @Test
    public void jms() throws MuleException
    {
        final MuleContext muleContextForApp = getMuleContextForApp("app-with-flows");

        muleContextForApp.getClient().dispatch("jms://in?connector=sharedJmsConnector", new DefaultMuleMessage("payload", muleContextForApp));
        muleContextForApp.getClient().request("jms://out?connector=sharedJmsConnector", MESSAGE_TIMEOUT);

        muleContextForApp.dispose();

        assertEventsUnreferenced();
    }

    private void assertEventsUnreferenced()
    {
        new PollingProber(PROBER_POLIING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitProbe()
        {
            @Override
            protected boolean test() throws Exception
            {
                System.gc();
                for (PhantomReference<MuleEvent> phantomReference : requestContextRefs)
                {
                    assertThat(phantomReference.isEnqueued(), is(true));
                }
                return true;
            }
        });
    }
}
