/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.client.MuleClient;
import org.mule.api.construct.FlowConstruct;
import org.mule.exception.AbstractMessagingExceptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

/**
 * Assert that flows do not propagate exceptions via vm request-response endpoints or use of flow-ref. Also
 * assert that a sub-flow/processor-chain does not handle it's own exception but they are rather handled by
 * calling flow.
 */
public class ExceptionPropagationMule5737TestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-propagation-mule-5737-config.xml";
    }

    @Test
    public void testVMRequestResponseEndpointExceptionPropagation() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://flow-in", "", null);
    }

    @Test
    public void testFlowWithChildFlowExceptionPropagation() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        FlowConstruct flow = muleContext.getRegistry().lookupFlowConstruct("flowWithChildFlow");
        FlowConstruct childFlow = muleContext.getRegistry().lookupFlowConstruct("childFlow");
        SensingExceptionStrategy parentES = (SensingExceptionStrategy) flow.getExceptionListener();
        SensingExceptionStrategy childFlowES = (SensingExceptionStrategy) childFlow.getExceptionListener();

        client.send("vm://flowWithChildFlow-in", "", null);

        assertFalse(parentES.caught);
        assertTrue(childFlowES.caught);

    }

    @Test
    public void testFlowWithSubFlowExceptionPropagation() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        SensingExceptionStrategy parentES = (SensingExceptionStrategy) muleContext.getRegistry()
            .lookupFlowConstruct("flowWithSubFlow")
            .getExceptionListener();

        client.send("vm://flowWithSubFlow-in", "", null);

        assertTrue(parentES.caught);
    }

    @Test
    public void testFlowWithChildServiceExceptionPropagation() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        SensingExceptionStrategy parentES = (SensingExceptionStrategy) muleContext.getRegistry()
            .lookupFlowConstruct("flowWithChildService")
            .getExceptionListener();
        SensingExceptionStrategy childServiceES = (SensingExceptionStrategy) muleContext.getRegistry()
            .lookupFlowConstruct("childService")
            .getExceptionListener();

        client.send("vm://flowWithChildService-in", "", null);

        assertFalse(parentES.caught);
        assertTrue(childServiceES.caught);
    }

    public static class SensingExceptionStrategy extends AbstractMessagingExceptionStrategy
    {

        public SensingExceptionStrategy()
        {
            super(null);
        }

        boolean caught;

        @Override
        public MuleEvent handleException(Exception e, MuleEvent event)
        {
            caught = true;
            MuleEvent resultEvent = super.handleException(e, event);
            resultEvent.getMessage().setExceptionPayload(null);
            ((MessagingException)e).setHandled(true);
            return resultEvent;
        }

    }

}
