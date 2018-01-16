/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;

public class TxPropagatesFlowToExceptionHandlerFlowTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "tx-propagates-flow-to-exception-handler-flow.xml";
    }

    @Test
    public void testIt() throws Exception
    {
        assertThat(WithFlowConstructProcessor.flow, sameInstance(muleContext.getRegistry().lookupFlowConstruct("transactional-exception-strategy-main-appFlow")));
    }

    public static class WithFlowConstructProcessor implements MessageProcessor, FlowConstructAware{

        public static FlowConstruct flow;
        
        @Override
        public void setFlowConstruct(FlowConstruct flowConstruct)
        {
            flow = flowConstruct;
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return event;
        }
    }
}
