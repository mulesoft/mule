/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.construct.Flow;

import org.junit.Test;

public class FlowStateTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/components/flow-initial-state.xml";
    }

    @Test
    public void testDefaultInitialstate() throws Exception
    {
        doTestStarted("default");
    }

    @Test
    public void testStartedInitialstate() throws Exception
    {
        doTestStarted("started");
    }

    protected void doTestStarted(String flowName) throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(
            flowName + "Flow");
        // Flow initially started
        assertTrue(flow.isStarted());
        assertFalse(flow.isStopped());
        assertTrue(((TestMessageSource) flow.getMessageSource()).isStarted());
    }

    @Test
    public void testInitialStateStopped() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(
            "stoppedFlow");
        assertEquals("stopped", flow.getInitialState());
        // Flow initially stopped
        assertFalse(flow.isStarted());
        assertTrue(flow.isStopped());
        assertFalse(((TestMessageSource) flow.getMessageSource()).isStarted());

        flow.start();
        assertTrue(flow.isStarted());
        assertFalse(flow.isStopped());
        assertTrue(((TestMessageSource) flow.getMessageSource()).isStarted());
    }

    public static class TestMessageSource implements MessageSource, Startable
    {

        private boolean started;

        @Override
        public void setListener(MessageProcessor listener)
        {

        }

        @Override
        public void start() throws MuleException
        {
            started = true;
        }

        public boolean isStarted()
        {
            return started;
        }
    }

}
