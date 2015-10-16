/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.api.MessagingException.PAYLOAD_INFO_KEY;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.MessageProcessorPathResolver;
import org.mule.api.execution.LocationExecutionContextProvider;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.exception.MessagingExceptionLocationProvider;
import org.mule.tck.SerializationTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;

import javax.xml.namespace.QName;

import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class MessagingExceptionTestCase extends AbstractMuleContextTestCase
{

    private static final String message = "a message";
    private static final String value = "Hello world!";
    
    private boolean originalVerboseExceptions;

    private LocationExecutionContextProvider locationProvider = new MessagingExceptionLocationProvider();
    
    @Mock
    private MuleEvent mockEvent;

    @Before
    public void before()
    {
        originalVerboseExceptions = DefaultMuleConfiguration.verboseExceptions;

        MuleContext mockContext = mock(MuleContext.class);
        DefaultMuleConfiguration mockConfiguration = mock(DefaultMuleConfiguration.class);
        when(mockConfiguration.getId()).thenReturn("MessagingExceptionTestCase");
        when(mockContext.getConfiguration()).thenReturn(mockConfiguration);
        when(mockEvent.getMuleContext()).thenReturn(mockContext);
    }
    
    @After
    public void after()
    {
        DefaultMuleConfiguration.verboseExceptions = originalVerboseExceptions;
    }

    @Test
    public void getCauseExceptionWithoutCause()
    {
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent);
        assertThat((MessagingException) exception.getCauseException(), is(exception));
    }

    @Test
    public void getCauseExceptionWithMuleCause()
    {
        DefaultMuleException causeException = new DefaultMuleException("");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat((DefaultMuleException) exception.getCauseException(), is(causeException));
    }

    @Test
    public void getCauseExceptionWithMuleCauseWithMuleCause()
    {
        DefaultMuleException causeCauseException = new DefaultMuleException("");
        DefaultMuleException causeException = new DefaultMuleException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat((DefaultMuleException) exception.getCauseException(), is(causeCauseException));
    }

    @Test
    public void getCauseExceptionWithNonMuleCause()
    {
        IOException causeException = new IOException("");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat((IOException) exception.getCauseException(), is(causeException));
    }

    @Test
    public void getCauseExceptionWithNonMuleCauseWithNonMuleCause()
    {
        ConnectException causeCauseException = new ConnectException();
        IOException causeException = new IOException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat((ConnectException) exception.getCauseException(), is(causeCauseException));
    }

    @Test
    public void causedByWithNullCause()
    {
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent);
        assertThat(exception.causedBy(MessagingException.class), Is.is(true));
        assertThat(exception.causedBy(Exception.class), Is.is(true));
        assertThat(exception.causedBy(DefaultMuleException.class), Is.is(false));
        assertThat(exception.causedBy(IOException.class), Is.is(false));
    }

    @Test
    public void causedByWithMuleCauseWithMuleCause()
    {
        DefaultMuleException causeCauseException = new DefaultMuleException("");
        DefaultMuleException causeException = new DefaultMuleException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedBy(DefaultMuleException.class), is(true));
        assertThat(exception.causedBy(MessagingException.class), is(true));
    }

    @Test
    public void causedByWithNonMuleCause()
    {
        IOException causeException = new IOException("");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedBy(IOException.class), is(true));
        assertThat(exception.causedBy(MessagingException.class), is(true));
        assertThat(exception.causedBy(Exception.class), is(true));
        assertThat(exception.causedBy(NullPointerException.class), is(false));
    }

    @Test
    public void causedByWithNonMuleCauseWithNonMuleCause()
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
    public void causedExactlyByWithNullCause()
    {
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent);
        assertThat(exception.causedExactlyBy(MessagingException.class), Is.is(true));
        assertThat(exception.causedExactlyBy(Exception.class), Is.is(false));
        assertThat(exception.causedExactlyBy(DefaultMuleException.class), Is.is(false));
        assertThat(exception.causedExactlyBy(IOException.class), Is.is(false));
    }

    @Test
    public void causedExactlyByWithMuleCauseWithMuleCause()
    {
        DefaultMuleException causeCauseException = new DefaultMuleException("");
        DefaultMuleException causeException = new DefaultMuleException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedExactlyBy(DefaultMuleException.class), is(true));
        assertThat(exception.causedExactlyBy(MessagingException.class), is(true));
    }

    @Test
    public void causedExactlyByWithNonMuleCause()
    {
        IOException causeException = new IOException("");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedExactlyBy(IOException.class), is(true));
        assertThat(exception.causedExactlyBy(MessagingException.class), is(true));
        assertThat(exception.causedExactlyBy(Exception.class), is(false));
        assertThat(exception.causedExactlyBy(NullPointerException.class), is(false));
    }

    @Test
    public void causedExactlyByWithNonMuleCauseWithNonMuleCause()
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
    public void withFailingProcessorNoPathResolver()
    {
        MessageProcessor mockProcessor = mock(MessageProcessor.class);
        when(mockEvent.getFlowConstruct()).thenReturn(null);
        when(mockProcessor.toString()).thenReturn("Mock@1");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), mockEvent, mockProcessor);
        exception.getInfo().putAll(locationProvider.getContextInfo(mockEvent, mockProcessor));
        assertThat(exception.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("Mock@1 @ MessagingExceptionTestCase"));
    }

    @Test
    public void withFailingProcessorPathResolver()
    {
        MessageProcessor mockProcessor = mock(MessageProcessor.class);
        MessageProcessorPathResolver pathResolver = mock(MessageProcessorPathResolver.class, withSettings().extraInterfaces(FlowConstruct.class));
        when(pathResolver.getProcessorPath(eq(mockProcessor))).thenReturn("/flow/processor");
        when(mockEvent.getFlowConstruct()).thenReturn((FlowConstruct) pathResolver);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), mockEvent, mockProcessor);
        exception.getInfo().putAll(locationProvider.getContextInfo(mockEvent, mockProcessor));
        assertThat(exception.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("/flow/processor @ MessagingExceptionTestCase"));
    }

    @Test
    public void withFailingProcessorNotPathResolver()
    {
        MessageProcessor mockProcessor = mock(MessageProcessor.class);
        FlowConstruct nonPathResolver = mock(FlowConstruct.class);
        when(mockEvent.getFlowConstruct()).thenReturn(nonPathResolver);
        when(mockProcessor.toString()).thenReturn("Mock@1");

        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), mockEvent, mockProcessor);
        exception.getInfo().putAll(locationProvider.getContextInfo(mockEvent, mockProcessor));
        assertThat(exception.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("Mock@1 @ MessagingExceptionTestCase"));
    }

    private static QName docNameAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "name");
    private static QName sourceFileNameAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileName");
    private static QName sourceFileLineAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileLine");

    @Test
    public void withAnnotatedFailingProcessorNoPathResolver()
    {
        MessageProcessor mockProcessor = mock(MessageProcessor.class, withSettings().extraInterfaces(AnnotatedObject.class));
        when(((AnnotatedObject) mockProcessor).getAnnotation(eq(docNameAttrName))).thenReturn("Mock Component");
        when(((AnnotatedObject) mockProcessor).getAnnotation(eq(sourceFileNameAttrName))).thenReturn("muleApp.xml");
        when(((AnnotatedObject) mockProcessor).getAnnotation(eq(sourceFileLineAttrName))).thenReturn(10);
        when(mockEvent.getFlowConstruct()).thenReturn(null);
        when(mockProcessor.toString()).thenReturn("Mock@1");

        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), mockEvent, mockProcessor);
        exception.getInfo().putAll(locationProvider.getContextInfo(mockEvent, mockProcessor));
        assertThat(exception.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("Mock@1 @ MessagingExceptionTestCase:muleApp.xml:10 (Mock Component)"));
    }

    @Test
    public void withAnnotatedFailingProcessorPathResolver()
    {
        MessageProcessor mockProcessor = mock(MessageProcessor.class, withSettings().extraInterfaces(AnnotatedObject.class));
        when(((AnnotatedObject) mockProcessor).getAnnotation(eq(docNameAttrName))).thenReturn("Mock Component");
        when(((AnnotatedObject) mockProcessor).getAnnotation(eq(sourceFileNameAttrName))).thenReturn("muleApp.xml");
        when(((AnnotatedObject) mockProcessor).getAnnotation(eq(sourceFileLineAttrName))).thenReturn(10);
        MessageProcessorPathResolver pathResolver = mock(MessageProcessorPathResolver.class, withSettings().extraInterfaces(FlowConstruct.class));
        when(pathResolver.getProcessorPath(eq(mockProcessor))).thenReturn("/flow/processor");
        when(mockEvent.getFlowConstruct()).thenReturn((FlowConstruct) pathResolver);

        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), mockEvent, mockProcessor);
        exception.getInfo().putAll(locationProvider.getContextInfo(mockEvent, mockProcessor));
        assertThat(exception.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("/flow/processor @ MessagingExceptionTestCase:muleApp.xml:10 (Mock Component)"));
    }

    @Test
    public void withAnnotatedFailingProcessorNotPathResolver()
    {
        MessageProcessor mockProcessor = mock(MessageProcessor.class, withSettings().extraInterfaces(AnnotatedObject.class));
        when(((AnnotatedObject) mockProcessor).getAnnotation(eq(docNameAttrName))).thenReturn("Mock Component");
        when(((AnnotatedObject) mockProcessor).getAnnotation(eq(sourceFileNameAttrName))).thenReturn("muleApp.xml");
        when(((AnnotatedObject) mockProcessor).getAnnotation(eq(sourceFileLineAttrName))).thenReturn(10);
        FlowConstruct nonPathResolver = mock(FlowConstruct.class);
        when(mockEvent.getFlowConstruct()).thenReturn(nonPathResolver);
        when(mockProcessor.toString()).thenReturn("Mock@1");

        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), mockEvent, mockProcessor);
        exception.getInfo().putAll(locationProvider.getContextInfo(mockEvent, mockProcessor));
        assertThat(exception.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("Mock@1 @ MessagingExceptionTestCase:muleApp.xml:10 (Mock Component)"));
    }

    @Test
    public void serializableMessagingException() throws Exception
    {
        TestSerializableMessageProcessor processor = new TestSerializableMessageProcessor();
        processor.setValue(value);

        MessagingException e = new MessagingException(MessageFactory.createStaticMessage(message),
            getTestEvent(""), processor);

        e = SerializationTestUtils.testException(e, muleContext);

        assertThat(e.getMessage(), containsString(message));
        assertThat(e.getFailingMessageProcessor(), not(nullValue()));
        assertThat(e.getFailingMessageProcessor(), instanceOf(TestSerializableMessageProcessor.class));
        assertThat(((TestSerializableMessageProcessor) e.getFailingMessageProcessor()).getValue(), is(value));
    }

    @Test
    public void nonSerializableMessagingException() throws Exception
    {
        TestNotSerializableMessageProcessor processor = new TestNotSerializableMessageProcessor();

        MessagingException e = new MessagingException(MessageFactory.createStaticMessage(message),
            getTestEvent(""), processor);

        e = SerializationTestUtils.testException(e, muleContext);

        assertThat(e.getMessage(), containsString(message));
        assertThat(e.getFailingMessageProcessor(), is(nullValue()));
    }
    
    @Test
    public void payloadInfoNonConsumable() throws Exception
    {
        DefaultMuleConfiguration.verboseExceptions = true;

        MuleEvent testEvent = mock(MuleEvent.class);
        MuleMessage muleMessage = mock(MuleMessage.class);
        Object payload = mock(Object.class);
        // This has to be done this way since mockito doesn't allow to verify toString()
        when(payload.toString()).then(new FailAnswer("toString() expected not to be called."));
        when(muleMessage.getPayload()).thenReturn(payload);
        when(muleMessage.getPayloadAsString()).thenReturn(value);
        when(testEvent.getMessage()).thenReturn(muleMessage);
        MessagingException e = new MessagingException(MessageFactory.createStaticMessage(message), testEvent);

        assertThat((String) e.getInfo().get(PAYLOAD_INFO_KEY), is(value));
    }

    @Test
    public void payloadInfoConsumable() throws Exception
    {
        DefaultMuleConfiguration.verboseExceptions = true;

        MuleEvent testEvent = mock(MuleEvent.class);
        MuleMessage muleMessage = mock(MuleMessage.class);
        when(muleMessage.getPayload()).thenReturn(new ByteArrayInputStream(new byte[] {}));
        when(testEvent.getMessage()).thenReturn(muleMessage);
        MessagingException e = new MessagingException(MessageFactory.createStaticMessage(message), testEvent);

        assertThat((String) e.getInfo().get(PAYLOAD_INFO_KEY), containsString(ByteArrayInputStream.class.getName() + "@"));

        verify(muleMessage, never()).getPayloadAsString();
    }

    @Test
    public void payloadInfoException() throws Exception
    {
        DefaultMuleConfiguration.verboseExceptions = true;

        MuleEvent testEvent = mock(MuleEvent.class);
        MuleMessage muleMessage = mock(MuleMessage.class);
        Object payload = mock(Object.class);
        // This has to be done this way since mockito doesn't allow to verify toString()
        when(payload.toString()).then(new FailAnswer("toString() expected not to be called."));
        when(muleMessage.getPayload()).thenReturn(payload);
        when(muleMessage.getPayloadAsString()).thenThrow(new Exception("exception thrown"));
        when(testEvent.getMessage()).thenReturn(muleMessage);
        MessagingException e = new MessagingException(MessageFactory.createStaticMessage(message), testEvent);

        assertThat((String) e.getInfo().get(PAYLOAD_INFO_KEY), is(Exception.class.getName() + " while getting payload: exception thrown"));
    }

    @Test
    public void payloadInfoNonVerbose() throws Exception
    {
        DefaultMuleConfiguration.verboseExceptions = false;

        MuleEvent testEvent = mock(MuleEvent.class);
        MuleMessage muleMessage = mock(MuleMessage.class);
        when(testEvent.getMessage()).thenReturn(muleMessage);
        MessagingException e = new MessagingException(MessageFactory.createStaticMessage(message), testEvent);

        assertThat(e.getInfo().get(PAYLOAD_INFO_KEY), nullValue());

        verify(muleMessage, never()).getPayload();
        verify(muleMessage, never()).getPayloadAsString();
    }

    private static final class FailAnswer implements Answer<String>
    {
        private final String failMessage;

        private FailAnswer(String failMessage)
        {
            this.failMessage = failMessage;
        }

        @Override
        public String answer(InvocationOnMock invocation) throws Throwable
        {
            fail(failMessage);
            return null;
        }
    }
}
