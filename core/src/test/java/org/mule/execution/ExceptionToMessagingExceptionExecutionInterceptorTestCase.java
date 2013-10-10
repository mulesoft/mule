/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.execution;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
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
            assertThat(e, is(mockMessagingException));
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
        }
    }
}
