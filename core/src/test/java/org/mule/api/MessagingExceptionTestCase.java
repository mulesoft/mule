/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.SerializationTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class MessagingExceptionTestCase extends AbstractMuleContextTestCase
{

    private static final String message = "a message";
    private static final String value = "Hello world!";
    
    @Mock
    private MuleEvent mockEvent;

    @Test
    public void testGetCauseExceptionWithoutCause()
    {
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent);
        assertThat((MessagingException) exception.getCauseException(), is(exception));
    }

    @Test
    public void testGetCauseExceptionWithMuleCause()
    {
        DefaultMuleException causeException = new DefaultMuleException("");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat((DefaultMuleException) exception.getCauseException(), is(causeException));
    }

    @Test
    public void testGetCauseExceptionWithMuleCauseWithMuleCause()
    {
        DefaultMuleException causeCauseException = new DefaultMuleException("");
        DefaultMuleException causeException = new DefaultMuleException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat((DefaultMuleException) exception.getCauseException(), is(causeCauseException));
    }

    @Test
    public void testGetCauseExceptionWithNonMuleCause()
    {
        IOException causeException = new IOException("");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat((IOException) exception.getCauseException(), is(causeException));
    }

    @Test
    public void testGetCauseExceptionWithNonMuleCauseWithNonMuleCause()
    {
        ConnectException causeCauseException = new ConnectException();
        IOException causeException = new IOException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat((ConnectException) exception.getCauseException(), is(causeCauseException));
    }

    @Test
    public void testCausedByWithNullCause()
    {
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent);
        assertThat(exception.causedBy(MessagingException.class), Is.is(true));
        assertThat(exception.causedBy(Exception.class), Is.is(true));
        assertThat(exception.causedBy(DefaultMuleException.class), Is.is(false));
        assertThat(exception.causedBy(IOException.class), Is.is(false));
    }

    @Test
    public void testCausedByWithMuleCauseWithMuleCause()
    {
        DefaultMuleException causeCauseException = new DefaultMuleException("");
        DefaultMuleException causeException = new DefaultMuleException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedBy(DefaultMuleException.class), is(true));
        assertThat(exception.causedBy(MessagingException.class), is(true));
    }

    @Test
    public void testCausedByWithNonMuleCause()
    {
        IOException causeException = new IOException("");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedBy(IOException.class), is(true));
        assertThat(exception.causedBy(MessagingException.class), is(true));
        assertThat(exception.causedBy(Exception.class), is(true));
        assertThat(exception.causedBy(NullPointerException.class), is(false));
    }

    @Test
    public void testCausedByWithNonMuleCauseWithNonMuleCause()
    {
        ConnectException causeCauseException = new ConnectException();
        IOException causeException = new IOException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedBy(NullPointerException.class), is(false));
        assertThat(exception.causedBy(SocketException.class), is(true));
        assertThat(exception.causedBy(IOException.class), is(true));
        assertThat(exception.causedBy(MessagingException.class), is(true));
    }

    @Test
    public void testCausedExactlyByWithNullCause()
    {
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent);
        assertThat(exception.causedExactlyBy(MessagingException.class), Is.is(true));
        assertThat(exception.causedExactlyBy(Exception.class), Is.is(false));
        assertThat(exception.causedExactlyBy(DefaultMuleException.class), Is.is(false));
        assertThat(exception.causedExactlyBy(IOException.class), Is.is(false));
    }

    @Test
    public void testCausedExactlyByWithMuleCauseWithMuleCause()
    {
        DefaultMuleException causeCauseException = new DefaultMuleException("");
        DefaultMuleException causeException = new DefaultMuleException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedExactlyBy(DefaultMuleException.class), is(true));
        assertThat(exception.causedExactlyBy(MessagingException.class), is(true));
    }

    @Test
    public void testCausedExactlyByWithNonMuleCause()
    {
        IOException causeException = new IOException("");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedExactlyBy(IOException.class), is(true));
        assertThat(exception.causedExactlyBy(MessagingException.class), is(true));
        assertThat(exception.causedExactlyBy(Exception.class), is(false));
        assertThat(exception.causedExactlyBy(NullPointerException.class), is(false));
    }

    @Test
    public void testCausedExactlyByWithNonMuleCauseWithNonMuleCause()
    {
        ConnectException causeCauseException = new ConnectException();
        IOException causeException = new IOException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedExactlyBy(ConnectException.class), is(true));
        assertThat(exception.causedExactlyBy(SocketException.class), is(false));
        assertThat(exception.causedExactlyBy(IOException.class), is(true));
        assertThat(exception.causedExactlyBy(MessagingException.class), is(true));
    }
    
    @Test
    public void testSerializableMessagingException() throws Exception
    {
        TestSerializableMessageProcessor processor = new TestSerializableMessageProcessor();
        processor.setValue(value);

        MessagingException e = new MessagingException(MessageFactory.createStaticMessage(message),
            getTestEvent(""), processor);

        e = SerializationTestUtils.testException(e, muleContext);

        assertTrue(e.getMessage().contains(message));
        assertNotNull(e.getFailingMessageProcessor());
        assertTrue(e.getFailingMessageProcessor() instanceof TestSerializableMessageProcessor);
        assertEquals(value, ((TestSerializableMessageProcessor) e.getFailingMessageProcessor()).getValue());
    }

    @Test
    public void testNonSerializableMessagingException() throws Exception
    {
        TestNotSerializableMessageProcessor processor = new TestNotSerializableMessageProcessor();

        MessagingException e = new MessagingException(MessageFactory.createStaticMessage(message),
            getTestEvent(""), processor);

        e = SerializationTestUtils.testException(e, muleContext);

        assertTrue(e.getMessage().contains(message));
        assertNull(e.getFailingMessageProcessor());
    }
}
