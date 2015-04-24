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
import static org.junit.Assert.assertNotNull;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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

    @Test
    public void testExecutionSuccessfully() throws MuleException
    {
        Mockito.when(mockMessageProcessor.process(mockMuleEvent)).thenReturn(mockResultMuleEvent);
        MuleEvent result = cut.execute(mockMessageProcessor, mockMuleEvent);
        assertThat(result, is(mockResultMuleEvent));
    }
    
    @Test
    public void testMessageExceptionThrown() throws MuleException
    {
        Mockito.when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(mockMessagingException);
        try
        {
            cut.execute(mockMessageProcessor, mockMuleEvent);
            fail("Exception should be thrown");
        }
        catch (MessagingException e)
        {
            assertThat(e.getCause(), is(mockMessagingException.getCause()));
            assertThat(e.getEvent(), is(mockMessagingException.getEvent()));
            assertNotNull(e.getFailingMessageProcessor());
        }
    }
    
    @Test
    public void testCheckedExceptionThrown() throws MuleException
    {
        Mockito.when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(mockMuleException);
        try
        {
            cut.execute(mockMessageProcessor, mockMuleEvent);
            fail("Exception should be thrown");
        }
        catch (MessagingException e)
        {
            assertThat((MuleException) e.getCause(), is(mockMuleException));
            assertNotNull(e.getFailingMessageProcessor());
        }
    }
    
    @Test
    public void testRuntimeExceptionThrown() throws MuleException
    {
        RuntimeException runtimeException = new RuntimeException();
        Mockito.when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(runtimeException);
        try
        {
            cut.execute(mockMessageProcessor, mockMuleEvent);
            fail("Exception should be thrown");
        }
        catch (MessagingException e)
        {
            assertThat((RuntimeException) e.getCause(), is(runtimeException));
            assertNotNull(e.getFailingMessageProcessor());
        }
    }

    @Test
    public void testErrorThrown() throws MuleException
    {
        Error error = new Error();
        Mockito.when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(error);
        try
        {
            cut.execute(mockMessageProcessor, mockMuleEvent);
            fail("Exception should be thrown");
        }
        catch (MessagingException e)
        {
            assertThat((Error) e.getCause(), is(error));
            assertNotNull(e.getFailingMessageProcessor());
        }
    }
}
