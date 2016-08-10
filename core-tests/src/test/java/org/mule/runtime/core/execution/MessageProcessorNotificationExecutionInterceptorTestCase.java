/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.DefaultMuleEvent.getCurrentEvent;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.context.notification.MessageProcessorNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.util.UUID;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MessageProcessorNotificationExecutionInterceptorTestCase extends AbstractMuleTestCase {

  @Mock
  private ServerNotificationManager mockNotificationManager;
  @Mock
  private MessageProcessorExecutionInterceptor mockNextInterceptor;
  @Mock
  private MessageProcessor mockMessageProcessor;
  @Mock
  private Pipeline mockPipeline;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleEvent mockMuleEvent;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleEvent mockMuleEventPreviousExecution;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleEvent mockResultMuleEvent;

  @Mock
  private ReplyToHandler mockReplyToHandler;

  @Mock
  private MessagingException mockMessagingException;
  private MessageProcessorNotificationExecutionInterceptor messageProcessorNotificationExecutionInterceptor;

  @Before
  public void setUpTest() {
    messageProcessorNotificationExecutionInterceptor = new MessageProcessorNotificationExecutionInterceptor(mockNextInterceptor);
  }

  @Test
  public void testExecutionSuccessfully() throws MuleException {
    final List<ServerNotification> serverNotifications = new ArrayList<ServerNotification>();
    Mockito.when(mockMessageProcessor.process(mockMuleEvent)).thenReturn(mockResultMuleEvent);
    Mockito.when(mockPipeline.getProcessorPath(mockMessageProcessor)).thenReturn("hi");
    Mockito.when(mockMuleEvent.getMuleContext().getNotificationManager()).thenReturn(mockNotificationManager);
    Mockito.when(mockMuleEvent.getFlowConstruct()).thenReturn(mockPipeline);
    Mockito.when(mockMuleEvent.isNotificationsEnabled()).thenReturn(true);
    Mockito.when(mockNextInterceptor.execute(mockMessageProcessor, mockMuleEvent)).thenReturn(mockResultMuleEvent);
    Mockito.when(mockNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)).thenReturn(true);
    Mockito.doAnswer(new Answer<Object>() {

      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        serverNotifications.add((ServerNotification) invocationOnMock.getArguments()[0]);
        return null;
      }
    }).when(mockNotificationManager).fireNotification(Mockito.any(ServerNotification.class));
    MuleEvent result = messageProcessorNotificationExecutionInterceptor.execute(mockMessageProcessor, mockMuleEvent);
    assertThat(result, is(mockResultMuleEvent));
    assertThat(serverNotifications.size(), Is.is(2));
    MessageProcessorNotification beforeMessageProcessorNotification = (MessageProcessorNotification) serverNotifications.get(0);
    MessageProcessorNotification afterMessageProcessorNotification = (MessageProcessorNotification) serverNotifications.get(1);
    assertThat(beforeMessageProcessorNotification.getAction(), is(MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE));
    assertThat(beforeMessageProcessorNotification.getProcessor(), is(mockMessageProcessor));
    assertThat(beforeMessageProcessorNotification.getExceptionThrown(), IsNull.nullValue());
    assertThat(afterMessageProcessorNotification.getAction(), is(MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE));
    assertThat(afterMessageProcessorNotification.getProcessor(), is(mockMessageProcessor));
    assertThat(afterMessageProcessorNotification.getExceptionThrown(), IsNull.nullValue());
  }

  @Test
  public void ignoresSuccessfulNotificationIfDisabled() throws MuleException {
    final List<ServerNotification> serverNotifications = new ArrayList<ServerNotification>();
    Mockito.when(mockMessageProcessor.process(mockMuleEvent)).thenReturn(mockResultMuleEvent);
    Mockito.when(mockMuleEvent.getMuleContext().getNotificationManager()).thenReturn(mockNotificationManager);
    Mockito.when(mockNextInterceptor.execute(mockMessageProcessor, mockMuleEvent)).thenReturn(mockResultMuleEvent);
    Mockito.when(mockNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)).thenReturn(true);
    Mockito.doAnswer(new Answer<Object>() {

      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        serverNotifications.add((ServerNotification) invocationOnMock.getArguments()[0]);
        return null;
      }
    }).when(mockNotificationManager).fireNotification(Mockito.any(ServerNotification.class));

    MuleEvent result = messageProcessorNotificationExecutionInterceptor.execute(mockMessageProcessor, mockMuleEvent);

    assertThat(result, is(mockResultMuleEvent));
    assertThat(serverNotifications.size(), Is.is(0));
  }

  /**
   * Validates that event to be processed is set to RequestContext for those cases whenever a messageProcessor modifies the RC
   * during its execution.
   */
  @Test
  public void requestContextSetBeforeProcessingEventBlockingPrcocessor() throws MuleException {
    final List<ServerNotification> serverNotifications = new ArrayList<ServerNotification>();
    Mockito.when(mockMessageProcessor.process(mockMuleEvent)).thenReturn(mockResultMuleEvent);
    Mockito.when(mockMuleEvent.getMuleContext().getNotificationManager()).thenReturn(mockNotificationManager);
    Mockito.when(mockNextInterceptor.execute(mockMessageProcessor, mockMuleEvent)).thenReturn(mockResultMuleEvent);
    Mockito.when(mockNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)).thenReturn(true);
    Mockito.doAnswer(new Answer<Object>() {

      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        serverNotifications.add((ServerNotification) invocationOnMock.getArguments()[0]);
        return null;
      }
    }).when(mockNotificationManager).fireNotification(Mockito.any(ServerNotification.class));

    setCurrentEvent(mockMuleEventPreviousExecution);

    MuleEvent result = messageProcessorNotificationExecutionInterceptor.execute(mockMessageProcessor, mockMuleEvent);

    assertThat(result, is(mockResultMuleEvent));
    assertThat(serverNotifications.size(), Is.is(0));

    assertThat(getCurrentEvent(), is(mockMuleEvent));
    assertThat(getCurrentEvent(), not(mockMuleEventPreviousExecution));
  }

  /**
   * Validates that event to be processed is copied and set to RequestContext for those cases whenever a messageProcessor modifies
   * the RC during its execution.
   */
  @Test
  public void requestContextSetBeforeProcessingEventNonBlockingPrcocessor() throws MuleException {
    final List<ServerNotification> serverNotifications = new ArrayList<ServerNotification>();
    mockMessageProcessor = Mockito.mock(MessageProcessor.class,
                                        Mockito.withSettings().extraInterfaces(NonBlockingMessageProcessor.class));

    Mockito.when(mockMessageProcessor.process(mockMuleEvent)).thenReturn(mockResultMuleEvent);
    Mockito.when(mockMuleEvent.getMuleContext().getNotificationManager()).thenReturn(mockNotificationManager);
    Mockito.when(mockNextInterceptor.execute(Mockito.eq(mockMessageProcessor), Mockito.any(MuleEvent.class)))
        .thenReturn(mockResultMuleEvent);

    String muleEventIdToProcess = UUID.getUUID();
    Mockito.when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);
    Mockito.when(mockMuleEvent.getReplyToHandler()).thenReturn(mockReplyToHandler);
    Mockito.when(mockMuleEvent.getId()).thenReturn(muleEventIdToProcess);

    Mockito.when(mockNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)).thenReturn(true);
    Mockito.doAnswer(new Answer<Object>() {

      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        serverNotifications.add((ServerNotification) invocationOnMock.getArguments()[0]);
        return null;
      }
    }).when(mockNotificationManager).fireNotification(Mockito.any(ServerNotification.class));

    setCurrentEvent(mockMuleEventPreviousExecution);

    MuleEvent result = messageProcessorNotificationExecutionInterceptor.execute(mockMessageProcessor, mockMuleEvent);

    assertThat(result, is(mockResultMuleEvent));
    assertThat(serverNotifications.size(), Is.is(0));

    assertThat(getCurrentEvent().getId(), equalTo(muleEventIdToProcess));
    assertThat(getCurrentEvent(), not(mockMuleEvent));
    assertThat(getCurrentEvent(), not(mockMuleEventPreviousExecution));
  }

  @Test
  public void testExecutionFailure() throws MuleException {
    final List<ServerNotification> serverNotifications = new ArrayList<ServerNotification>();
    Mockito.when(mockNextInterceptor.execute(mockMessageProcessor, mockMuleEvent)).thenThrow(mockMessagingException);
    Mockito.when(mockPipeline.getProcessorPath(mockMessageProcessor)).thenReturn("hi");
    Mockito.when(mockMuleEvent.getFlowConstruct()).thenReturn(mockPipeline);
    Mockito.when(mockMuleEvent.getMuleContext().getNotificationManager()).thenReturn(mockNotificationManager);
    Mockito.when(mockMuleEvent.isNotificationsEnabled()).thenReturn(true);
    Mockito.when(mockNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)).thenReturn(true);
    Mockito.doAnswer(new Answer<Object>() {

      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        serverNotifications.add((ServerNotification) invocationOnMock.getArguments()[0]);
        return null;
      }
    }).when(mockNotificationManager).fireNotification(Mockito.any(ServerNotification.class));
    try {
      messageProcessorNotificationExecutionInterceptor.execute(mockMessageProcessor, mockMuleEvent);
      fail("Exception should be thrown");
    } catch (MessagingException e) {
    }
    assertThat(serverNotifications.size(), Is.is(2));
    MessageProcessorNotification beforeMessageProcessorNotification = (MessageProcessorNotification) serverNotifications.get(0);
    MessageProcessorNotification afterMessageProcessorNotification = (MessageProcessorNotification) serverNotifications.get(1);
    assertThat(beforeMessageProcessorNotification.getAction(), is(MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE));
    assertThat(beforeMessageProcessorNotification.getProcessor(), is(mockMessageProcessor));
    assertThat(beforeMessageProcessorNotification.getExceptionThrown(), IsNull.nullValue());
    assertThat(afterMessageProcessorNotification.getAction(), is(MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE));
    assertThat(afterMessageProcessorNotification.getProcessor(), is(mockMessageProcessor));
    assertThat(afterMessageProcessorNotification.getExceptionThrown(), Is.is(mockMessagingException));
  }

  @Test
  public void ignoresFailureNotificationIfDisabled() throws MuleException {
    final List<ServerNotification> serverNotifications = new ArrayList<ServerNotification>();
    Mockito.when(mockNextInterceptor.execute(mockMessageProcessor, mockMuleEvent)).thenThrow(mockMessagingException);
    Mockito.when(mockMuleEvent.getMuleContext().getNotificationManager()).thenReturn(mockNotificationManager);
    Mockito.when(mockNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)).thenReturn(true);
    Mockito.doAnswer(new Answer<Object>() {

      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        serverNotifications.add((ServerNotification) invocationOnMock.getArguments()[0]);
        return null;
      }
    }).when(mockNotificationManager).fireNotification(Mockito.any(ServerNotification.class));

    try {
      messageProcessorNotificationExecutionInterceptor.execute(mockMessageProcessor, mockMuleEvent);
      fail("Exception should be thrown");
    } catch (MessagingException e) {
    }

    assertThat(serverNotifications.size(), Is.is(0));
  }
}
