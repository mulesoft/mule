/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class FlowRefTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/flow-ref.xml";
    }

    @Test
    public void testTwoFlowRefsToSubFlow() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage msg = client.send("vm://two.flow.ref.to.sub.flow", new DefaultMuleMessage("0",
            muleContext));

        assertEquals("012xyzabc312xyzabc3", msg.getPayloadAsString());

    }

    @Test
    public void testDynamicFlowRef() throws Exception
    {
        MuleEvent eventA = getTestEvent("0");
        eventA.setFlowVariable("letter", "A");
        MuleEvent eventB = getTestEvent("0");
        eventB.setFlowVariable("letter", "B");

        assertEquals("0A", ((Flow) getFlowConstruct("flow2")).process(eventA).getMessageAsString());
        assertEquals("0B", ((Flow) getFlowConstruct("flow2")).process(eventB).getMessageAsString());
    }

    @Test(expected=MessagingException.class)
    public void testFlowRefNotFound() throws Exception
    {
        MuleEvent eventC = getTestEvent("0");
        eventC.setFlowVariable("letter", "C");

        assertEquals("0C", ((Flow) getFlowConstruct("flow2")).process(eventC).getMessageAsString());

    }
}
