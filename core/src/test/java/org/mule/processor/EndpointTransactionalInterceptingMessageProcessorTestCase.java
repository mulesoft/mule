/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.junit.Before;
import org.junit.Test;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transaction.TransactionConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class EndpointTransactionalInterceptingMessageProcessorTestCase extends AbstractMuleTestCase
{
    private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS.get());
    private EndpointTransactionalInterceptingMessageProcessor processor;
    private MessageProcessor next = mock(MessageProcessor.class);
    private TransactionConfig config = mock(TransactionConfig.class);
    private MuleEvent event = mock(MuleEvent.class);

    @Before
    public void setup()
    {
        processor = new EndpointTransactionalInterceptingMessageProcessor(config);
        processor.setMuleContext(mockMuleContext);
        processor.setListener(next);
        when(event.getMuleContext()).thenReturn(mockMuleContext);
        when(config.isInteractWithExternal()).thenReturn(true);
    }

    @Test
    public void executesNextIfTransactionIsConfigured() throws MuleException
    {
        when(config.isConfigured()).thenReturn(true);
        processor.process(event);
        verify(next).process(any(MuleEvent.class));
    }

    @Test
    public void executesNextIfTransactionIsNotConfigured() throws MuleException
    {
        when(config.isConfigured()).thenReturn(false);
        processor.process(event);
        verify(next).process(any(MuleEvent.class));
    }

    @Test
    public void usesTransactionConfigurationWhenConfigured() throws MuleException
    {
        when(config.isConfigured()).thenReturn(true);
        processor.process(event);
        verify(config).getFactory();
    }

    @Test
    public void doesntUseTransactionConfigurationWhenConfigured() throws MuleException
    {
        when(config.isConfigured()).thenReturn(false);
        processor.process(event);
        verify(config, never()).getFactory();
    }
}
