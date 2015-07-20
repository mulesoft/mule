/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.concurrent.Latch;

import org.junit.Rule;
import org.junit.Test;

public class TimeoutFunctionalTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "timeout-config.xml";
    }

    @Test
    public void flowAndSessionVarsAreNotRemovedAfterTimeout() throws Exception
    {
        final Latch serverLatch = new Latch();

        getFunctionalTestComponent("server").setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                serverLatch.await();
            }
        });

        Flow flow = (Flow) getFlowConstruct("client");

        MuleEvent event = getTestEvent("<echo/>");
        event.setTimeout(1);

        flow.process(event);
        serverLatch.release();

        MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);

        assertThat(message.<String>getInboundProperty("flowVar"), equalTo("testFlowVar"));
        assertThat(message.<String>getInboundProperty("sessionVar"), equalTo("testSessionVar"));
    }
}
