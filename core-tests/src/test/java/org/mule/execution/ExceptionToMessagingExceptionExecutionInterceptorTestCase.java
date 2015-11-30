/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExceptionToMessagingExceptionExecutionInterceptorTestCase extends AbstractMuleTestCase
{
    @Mock
    private MessageProcessor mockMessageProcessor;
    @Mock 
    private MuleEvent mockMuleEvent;
    @Mock
    private MuleEvent mockResultMuleEvent;
    @Mock
    private MessagingException mockMessagingException;
    @Mock
    private MuleException mockMuleException;
    private ExceptionToMessagingExceptionExecutionInterceptor cut = new ExceptionToMessagingExceptionExecutionInterceptor();

    @Before
    public void before()
    {
        when(mockMuleEvent.getMuleContext()).thenReturn(mock(MuleContext.class));
        
        when(mockMessagingException.getFailingMessageProcessor()).thenCallRealMethod();
    }

    @Test
    public void executionSuccessfully() throws MuleException
    {
        when(mockMessageProcessor.process(mockMuleEvent)).thenReturn(mockResultMuleEvent);
        MuleEvent result = cut.execute(mockMessageProcessor, mockMuleEvent);
        assertThat(result, is(mockResultMuleEvent));
    }
    
    @Test
    public void messageExceptionThrown() throws MuleException
    {
        when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(mockMessagingException);
        try
        {
            cut.execute(mockMessageProcessor, mockMuleEvent);
            fail("Exception should be thrown");
        }
        catch (MessagingException e)
        {
            assertThat(e, is(mockMessagingException));
        }
    }
    
    @Test
    public void checkedExceptionThrown() throws MuleException
    {
        when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(mockMuleException);
        try
        {
            cut.execute(mockMessageProcessor, mockMuleEvent);
            fail("Exception should be thrown");
        }
        catch (MessagingException e)
        {
            assertThat((MuleException) e.getCause(), is(mockMuleException));
        }
    }
    
    @Test
    public void runtimeExceptionThrown() throws MuleException
    {
        RuntimeException runtimeException = new RuntimeException();
        when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(runtimeException);
        try
        {
            cut.execute(mockMessageProcessor, mockMuleEvent);
            fail("Exception should be thrown");
        }
        catch (MessagingException e)
        {
            assertThat((RuntimeException) e.getCause(), is(runtimeException));
        }
    }

    @Test
    public void errorThrown() throws MuleException
    {
        Error error = new Error();
        when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(error);
        try
        {
            cut.execute(mockMessageProcessor, mockMuleEvent);
            fail("Exception should be thrown");
        }
        catch (MessagingException e)
        {
            assertThat((Error) e.getCause(), is(error));
        }
    }
}
