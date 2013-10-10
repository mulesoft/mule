/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.issues;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.Transformer;
import org.mule.construct.Flow;
import org.mule.routing.MessageFilter;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertNotSame;

public class Mule5038TestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/issues/mule-5038-config.xml";
    }

    @Test
    public void testTransformerOnGlobalEndpoint()
    {
        Flow flow1 = muleContext.getRegistry().lookupObject("flow1");
        Filter flow1Filter = ((MessageFilter) flow1.getMessageProcessors().get(0)).getFilter();
        Flow flow2 = muleContext.getRegistry().lookupObject("flow2");
        Filter flow2Filter = ((MessageFilter) flow2.getMessageProcessors().get(0)).getFilter();

        assertNotSame(flow1Filter, flow2Filter);
    }

    @Test
    public void testFilterOnGlobalEndpoint()
    {
        Flow flow1 = muleContext.getRegistry().lookupObject("flow1");
        Transformer flow1Transoformer = (Transformer) flow1.getMessageProcessors().get(1);
        Flow flow2 = muleContext.getRegistry().lookupObject("flow2");
        Transformer flow2Transoformer = (Transformer) flow2.getMessageProcessors().get(1);

        assertNotSame(flow1Transoformer, flow2Transoformer);
    }

    @Test
    public void testCustomProcessorOnGlobalEndpoint()
    {
        Flow flow1 = muleContext.getRegistry().lookupObject("flow1");
        MessageProcessor flow1Processor = flow1.getMessageProcessors().get(3);
        Flow flow2 = muleContext.getRegistry().lookupObject("flow2");
        MessageProcessor flow2Processor = flow2.getMessageProcessors().get(3);

        assertNotSame(flow1Processor, flow2Processor);
    }

    @Test
    public void testCompositeProcessorOnGlobalEndpoint()
    {
        Flow flow1 = muleContext.getRegistry().lookupObject("flow1");
        MessageProcessor flow1Processor = flow1.getMessageProcessors().get(2);
        Flow flow2 = muleContext.getRegistry().lookupObject("flow2");
        MessageProcessor flow2Processor = flow2.getMessageProcessors().get(2);

        assertNotSame(flow1Processor, flow2Processor);
    }
}
