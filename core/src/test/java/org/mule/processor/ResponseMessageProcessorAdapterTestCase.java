/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ResponseMessageProcessorAdapterTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testResponseAdaptorSingleMP() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new StringAppendTransformer("1"), new ResponseMessageProcessorAdapter(
            new StringAppendTransformer("3")), new StringAppendTransformer("2"));
        assertEquals("0123", builder.build().process(getTestEventUsingFlow("0")).getMessageAsString());
    }

    @Test
    public void testResponseAdaptorSingleMPReturnsNull() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new StringAppendTransformer("1"), new ResponseMessageProcessorAdapter(
            new ReturnNullMP()), new StringAppendTransformer("2"));
        assertEquals("012", builder.build().process(getTestEventUsingFlow("0")).getMessageAsString());
    }

    @Test
    public void testResponseAdaptorNestedChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
            new StringAppendTransformer("1"),
            new ResponseMessageProcessorAdapter(new DefaultMessageProcessorChainBuilder().chain(
                new StringAppendTransformer("a"), new StringAppendTransformer("b")).build()),
            new StringAppendTransformer("2"));
        assertEquals("012ab", builder.build().process(getTestEventUsingFlow("0")).getMessageAsString());
    }

    @Test
    public void testResponseAdaptorNestedChainReturnsNull() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
            new StringAppendTransformer("1"),
            new ResponseMessageProcessorAdapter(new DefaultMessageProcessorChainBuilder().chain(
                new StringAppendTransformer("a"), new StringAppendTransformer("b"), new ReturnNullMP())
                .build()), new StringAppendTransformer("2"));
        assertEquals("012", builder.build().process(getTestEventUsingFlow("0")).getMessageAsString());
    }

    private static class ReturnNullMP implements MessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return null;
        }
    }

}
