/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.exception.MuleException.INFO_LOCATION_KEY;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.context.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.core.exception.MessagingException.PAYLOAD_INFO_KEY;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.AbstractProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.exception.MessagingExceptionLocationProvider;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class MessagingExceptionTestCase extends AbstractMuleContextTestCase {

  private static final String message = "a message";
  private static final String value = "Hello world!";

  private boolean originalVerboseExceptions;

  private MessagingExceptionLocationProvider locationProvider = new MessagingExceptionLocationProvider();

  @Mock
  private MuleContext mockContext;

  private Event testEvent;

  @Mock
  private FlowConstruct flowConstruct;

  @Mock
  private TransformationService transformationService;

  @Before
  public void before() throws MuleException {
    originalVerboseExceptions = MuleException.verboseExceptions;

    locationProvider.setMuleContext(mockContext);

    DefaultMuleConfiguration mockConfiguration = mock(DefaultMuleConfiguration.class);
    when(mockConfiguration.getId()).thenReturn("MessagingExceptionTestCase");
    when(mockContext.getConfiguration()).thenReturn(mockConfiguration);
    when(mockContext.getTransformationService()).thenReturn(transformationService);

    testEvent = eventBuilder().message(of(TEST_PAYLOAD)).build();
  }

  @After
  public void after() {
    MuleException.verboseExceptions = originalVerboseExceptions;
  }

  @Test
  public void getCauseExceptionWithoutCause() {
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent);
    assertThat(exception.getRootCause(), is(exception));
  }

  @Test
  public void getCauseExceptionWithMuleCause() {
    DefaultMuleException causeException = new DefaultMuleException("");
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, causeException);
    assertThat(exception.getRootCause(), is(causeException));
  }

  @Test
  public void getCauseExceptionWithMuleCauseWithMuleCause() {
    DefaultMuleException causeCauseException = new DefaultMuleException("");
    DefaultMuleException causeException = new DefaultMuleException(causeCauseException);
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, causeException);
    assertThat(exception.getRootCause(), is(causeCauseException));
  }

  @Test
  public void getCauseExceptionWithNonMuleCause() {
    IOException causeException = new IOException("");
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, causeException);
    assertThat(exception.getRootCause(), is(causeException));
  }

  @Test
  public void getCauseExceptionWithNonMuleCauseWithNonMuleCause() {
    ConnectException causeCauseException = new ConnectException();
    IOException causeException = new IOException(causeCauseException);
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, causeException);
    assertThat(exception.getRootCause(), is(causeCauseException));
  }

  @Test
  public void causedByWithNullCause() {
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent);
    assertThat(exception.causedBy(MessagingException.class), Is.is(true));
    assertThat(exception.causedBy(Exception.class), Is.is(true));
    assertThat(exception.causedBy(DefaultMuleException.class), Is.is(false));
    assertThat(exception.causedBy(IOException.class), Is.is(false));
  }

  @Test
  public void causedByWithMuleCauseWithMuleCause() {
    DefaultMuleException causeCauseException = new DefaultMuleException("");
    DefaultMuleException causeException = new DefaultMuleException(causeCauseException);
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, causeException);
    assertThat(exception.causedBy(DefaultMuleException.class), is(true));
    assertThat(exception.causedBy(MessagingException.class), is(true));
  }

  @Test
  public void causedByWithNonMuleCause() {
    IOException causeException = new IOException("");
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, causeException);
    assertThat(exception.causedBy(IOException.class), is(true));
    assertThat(exception.causedBy(MessagingException.class), is(true));
    assertThat(exception.causedBy(Exception.class), is(true));
    assertThat(exception.causedBy(NullPointerException.class), is(false));
  }

  @Test
  public void causedByWithNonMuleCauseWithNonMuleCause() {
    ConnectException causeCauseException = new ConnectException();
    IOException causeException = new IOException(causeCauseException);
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, causeException);
    assertThat(exception.causedBy(NullPointerException.class), is(false));
    assertThat(exception.causedBy(SocketException.class), is(true));
    assertThat(exception.causedBy(IOException.class), is(true));
    assertThat(exception.causedBy(MessagingException.class), is(true));
  }

  @Test
  public void causedExactlyByWithNullCause() {
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent);
    assertThat(exception.causedExactlyBy(MessagingException.class), Is.is(true));
    assertThat(exception.causedExactlyBy(Exception.class), Is.is(false));
    assertThat(exception.causedExactlyBy(DefaultMuleException.class), Is.is(false));
    assertThat(exception.causedExactlyBy(IOException.class), Is.is(false));
  }

  @Test
  public void causedExactlyByWithMuleCauseWithMuleCause() {
    DefaultMuleException causeCauseException = new DefaultMuleException("");
    DefaultMuleException causeException = new DefaultMuleException(causeCauseException);
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, causeException);
    assertThat(exception.causedExactlyBy(DefaultMuleException.class), is(true));
    assertThat(exception.causedExactlyBy(MessagingException.class), is(true));
  }

  @Test
  public void causedExactlyByWithNonMuleCause() {
    IOException causeException = new IOException("");
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, causeException);
    assertThat(exception.causedExactlyBy(IOException.class), is(true));
    assertThat(exception.causedExactlyBy(MessagingException.class), is(true));
    assertThat(exception.causedExactlyBy(Exception.class), is(false));
    assertThat(exception.causedExactlyBy(NullPointerException.class), is(false));
  }

  @Test
  public void causedExactlyByWithNonMuleCauseWithNonMuleCause() {
    ConnectException causeCauseException = new ConnectException();
    IOException causeException = new IOException(causeCauseException);
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, causeException);
    assertThat(exception.causedExactlyBy(ConnectException.class), is(true));
    assertThat(exception.causedExactlyBy(SocketException.class), is(false));
    assertThat(exception.causedExactlyBy(IOException.class), is(true));
    assertThat(exception.causedExactlyBy(MessagingException.class), is(true));
  }

  @Test
  public void withFailingProcessorNoPathResolver() {
    Processor mockProcessor = mock(Processor.class);
    when(mockProcessor.toString()).thenReturn("Mock@1");
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, mockProcessor);
    exception.getInfo()
        .putAll(locationProvider.getContextInfo(createInfo(testEvent, exception, mockProcessor), mockProcessor, null));
    assertThat(exception.getInfo().get(INFO_LOCATION_KEY).toString(),
               is("Mock@1 @ MessagingExceptionTestCase"));
  }

  @Test
  public void withFailingProcessorPathResolver() {
    AbstractProcessor mockProcessor = mock(AbstractProcessor.class);
    ComponentLocation componentLocation = mock(ComponentLocation.class);
    when((mockProcessor).getAnnotation(eq(sourceFileNameAttrName))).thenReturn("muleApp.xml");
    when((mockProcessor).getAnnotation(eq(sourceFileLineAttrName))).thenReturn(10);
    when(mockProcessor.getLocation()).thenReturn(componentLocation);
    when(componentLocation.getLocation()).thenReturn("flow/processor");
    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, mockProcessor);
    exception.getInfo()
        .putAll(locationProvider.getContextInfo(createInfo(testEvent, exception, mockProcessor), mockProcessor, flowConstruct));
    assertThat(exception.getInfo().get(INFO_LOCATION_KEY).toString(),
               is("flow/processor @ MessagingExceptionTestCase:muleApp.xml:10"));
  }

  @Test
  public void withFailingProcessorNotPathResolver() {
    Processor mockProcessor = mock(Processor.class);
    FlowConstruct nonPathResolver = mock(FlowConstruct.class);
    when(mockProcessor.toString()).thenReturn("Mock@1");

    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, mockProcessor);
    exception.getInfo()
        .putAll(locationProvider.getContextInfo(createInfo(testEvent, exception, mockProcessor), mockProcessor, nonPathResolver));
    assertThat(exception.getInfo().get(INFO_LOCATION_KEY).toString(),
               is("Mock@1 @ MessagingExceptionTestCase"));
  }

  private static QName docNameAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "name");
  private static QName sourceFileNameAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileName");
  private static QName sourceFileLineAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileLine");

  @Test
  public void withAnnotatedFailingProcessorNoPathResolver() {
    Processor mockProcessor = mock(Processor.class, withSettings().extraInterfaces(AnnotatedObject.class));
    when(((AnnotatedObject) mockProcessor).getAnnotation(eq(docNameAttrName))).thenReturn("Mock Component");
    when(((AnnotatedObject) mockProcessor).getAnnotation(eq(sourceFileNameAttrName))).thenReturn("muleApp.xml");
    when(((AnnotatedObject) mockProcessor).getAnnotation(eq(sourceFileLineAttrName))).thenReturn(10);
    when(mockProcessor.toString()).thenReturn("Mock@1");

    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, mockProcessor);
    exception.getInfo()
        .putAll(locationProvider.getContextInfo(createInfo(testEvent, exception, mockProcessor), mockProcessor, null));
    assertThat(exception.getInfo().get(MuleException.INFO_LOCATION_KEY).toString(),
               is("Mock@1 @ MessagingExceptionTestCase:muleApp.xml:10 (Mock Component)"));
  }

  @Test
  public void withAnnotatedFailingProcessorPathResolver() {
    AbstractProcessor mockProcessor = mock(AbstractProcessor.class);
    when(mockProcessor.getAnnotation(eq(docNameAttrName))).thenReturn("Mock Component");
    when(mockProcessor.getAnnotation(eq(sourceFileNameAttrName))).thenReturn("muleApp.xml");
    when(mockProcessor.getAnnotation(eq(sourceFileLineAttrName))).thenReturn(10);
    ComponentLocation componentLocation = mock(ComponentLocation.class);
    when(mockProcessor.getLocation()).thenReturn(componentLocation);
    when(componentLocation.getLocation()).thenReturn("flow/processor");

    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, mockProcessor);
    exception.getInfo()
        .putAll(locationProvider.getContextInfo(createInfo(testEvent, exception, mockProcessor), mockProcessor, flowConstruct));
    assertThat(exception.getInfo().get(INFO_LOCATION_KEY).toString(),
               is("flow/processor @ MessagingExceptionTestCase:muleApp.xml:10 (Mock Component)"));
  }

  @Test
  public void withAnnotatedFailingProcessorNotPathResolver() {
    Processor mockProcessor = mock(Processor.class, withSettings().extraInterfaces(AnnotatedObject.class));
    when(((AnnotatedObject) mockProcessor).getAnnotation(eq(docNameAttrName))).thenReturn("Mock Component");
    when(((AnnotatedObject) mockProcessor).getAnnotation(eq(sourceFileNameAttrName))).thenReturn("muleApp.xml");
    when(((AnnotatedObject) mockProcessor).getAnnotation(eq(sourceFileLineAttrName))).thenReturn(10);
    FlowConstruct nonPathResolver = mock(FlowConstruct.class);
    when(mockProcessor.toString()).thenReturn("Mock@1");

    MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), testEvent, mockProcessor);
    exception.getInfo()
        .putAll(locationProvider.getContextInfo(createInfo(testEvent, exception, mockProcessor), mockProcessor, nonPathResolver));
    assertThat(exception.getInfo().get(INFO_LOCATION_KEY).toString(),
               is("Mock@1 @ MessagingExceptionTestCase:muleApp.xml:10 (Mock Component)"));
  }

  @Test
  public void serializableMessagingException() throws Exception {
    TestSerializableMessageProcessor processor = new TestSerializableMessageProcessor();
    processor.setValue(value);

    MessagingException e = new MessagingException(I18nMessageFactory.createStaticMessage(message), testEvent, processor);

    e = SerializationTestUtils.testException(e, muleContext);

    assertThat(e.getMessage(), containsString(message));
    assertThat(e.getFailingMessageProcessor(), not(nullValue()));
    assertThat(e.getFailingMessageProcessor(), instanceOf(TestSerializableMessageProcessor.class));
    assertThat(((TestSerializableMessageProcessor) e.getFailingMessageProcessor()).getValue(), is(value));
  }

  @Test
  public void nonSerializableMessagingException() throws Exception {
    TestNotSerializableMessageProcessor processor = new TestNotSerializableMessageProcessor();

    MessagingException e = new MessagingException(I18nMessageFactory.createStaticMessage(message), testEvent, processor);

    e = SerializationTestUtils.testException(e, muleContext);

    assertThat(e.getMessage(), containsString(message));
    assertThat(e.getFailingMessageProcessor(), is(nullValue()));
  }

  @Test
  @Ignore("MULE-10266 review how the transformationService is obtained when building an exception.")
  public void payloadInfoNonConsumable() throws Exception {
    MuleException.verboseExceptions = true;

    Event testEvent = mock(Event.class);
    Object payload = mock(Object.class);
    // This has to be done this way since mockito doesn't allow to verify toString()
    when(payload.toString()).then(new FailAnswer("toString() expected not to be called."));
    Message muleMessage = of(payload);

    when(transformationService.transform(muleMessage, DataType.STRING)).thenReturn(of(value));
    when(testEvent.getMessage()).thenReturn(muleMessage);
    MessagingException e = new MessagingException(I18nMessageFactory.createStaticMessage(message), testEvent);

    assertThat(e.getInfo().get(PAYLOAD_INFO_KEY), is(value));
  }

  @Test
  public void payloadInfoConsumable() throws Exception {
    MuleException.verboseExceptions = true;

    Event testEvent = mock(Event.class);
    final ByteArrayInputStream payload = new ByteArrayInputStream(new byte[] {});
    Message muleMessage = of(payload);
    when(testEvent.getMessage()).thenReturn(muleMessage);
    MessagingException e = new MessagingException(I18nMessageFactory.createStaticMessage(message), testEvent);

    assertThat((String) e.getInfo().get(PAYLOAD_INFO_KEY), containsString(ByteArrayInputStream.class.getName() + "@"));

    verify(transformationService, never()).transform(muleMessage, DataType.STRING);
  }

  @Test
  @Ignore("MULE-10266 review how the transformationService is obtained when building an exception.")
  public void payloadInfoException() throws Exception {
    MuleException.verboseExceptions = true;

    Event testEvent = mock(Event.class);
    Object payload = mock(Object.class);
    // This has to be done this way since mockito doesn't allow to verify toString()
    when(payload.toString()).then(new FailAnswer("toString() expected not to be called."));
    Message muleMessage = of(payload);

    when(transformationService.transform(muleMessage, DataType.STRING))
        .thenThrow(new TransformerException(CoreMessages.createStaticMessage("exception thrown")));
    when(testEvent.getMessage()).thenReturn(muleMessage);
    MessagingException e = new MessagingException(I18nMessageFactory.createStaticMessage(message), testEvent);

    assertThat(e.getInfo().get(PAYLOAD_INFO_KEY),
               is(TransformerException.class.getName() + " while getting payload: exception thrown"));
  }

  @Test
  public void payloadInfoNonVerbose() throws Exception {
    MuleException.verboseExceptions = false;

    Event testEvent = mock(Event.class);
    Message muleMessage = spy(of(""));
    when(testEvent.getMessage()).thenReturn(muleMessage);
    MessagingException e = new MessagingException(I18nMessageFactory.createStaticMessage(message), testEvent);

    assertThat(e.getInfo().get(PAYLOAD_INFO_KEY), nullValue());

    verify(muleMessage, never()).getPayload();
    verify(transformationService, never()).transform(muleMessage, DataType.STRING);
  }

  private static final class FailAnswer implements Answer<String> {

    private final String failMessage;

    private FailAnswer(String failMessage) {
      this.failMessage = failMessage;
    }

    @Override
    public String answer(InvocationOnMock invocation) throws Throwable {
      fail(failMessage);
      return null;
    }
  }

}
