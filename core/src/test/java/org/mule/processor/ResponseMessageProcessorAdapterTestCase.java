/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
