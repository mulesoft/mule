/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.core.DefaultMuleEvent.getCurrentEvent;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

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
  private MuleContext mockMuleContext;
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
    messageProcessorNotificationExecutionInterceptor.setMuleContext(mockMuleContext);
    when(mockPipeline.getName()).thenReturn("flow");
    messageProcessorNotificationExecutionInterceptor.setFlowConstruct(mockPipeline);
    when(mockMuleContext.getNotificationManager()).thenReturn(mockNotificationManager);
  }

  @Test
  public void testExecutionSuccessfully() throws MuleException {
    final List<ServerNotification> serverNotifications = new ArrayList<>();
    when(mockMessageProcessor.process(mockMuleEvent)).thenReturn(mockResultMuleEvent);
    when(mockPipeline.getProcessorPath(mockMessageProcessor)).thenReturn("hi");
    when(mockMuleEvent.isNotificationsEnabled()).thenReturn(true);
    when(mockNextInterceptor.execute(mockMessageProcessor, mockMuleEvent)).thenReturn(mockResultMuleEvent);
    when(mockNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)).thenReturn(true);
    doAnswer(invocationOnMock -> {
      serverNotifications.add((ServerNotification) invocationOnMock.getArguments()[0]);
      return null;
    }).when(mockNotificationManager).fireNotification(any(ServerNotification.class));
    MuleEvent result = messageProcessorNotificationExecutionInterceptor.execute(mockMessageProcessor, mockMuleEvent);
    assertThat(result, is(mockResultMuleEvent));
    assertThat(serverNotifications.size(), is(2));
    MessageProcessorNotification beforeMessageProcessorNotification = (MessageProcessorNotification) serverNotifications.get(0);
    MessageProcessorNotification afterMessageProcessorNotification = (MessageProcessorNotification) serverNotifications.get(1);
    assertThat(beforeMessageProcessorNotification.getAction(), is(MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE));
    assertThat(beforeMessageProcessorNotification.getProcessor(), is(mockMessageProcessor));
    assertThat(beforeMessageProcessorNotification.getExceptionThrown(), nullValue());
    assertThat(afterMessageProcessorNotification.getAction(), is(MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE));
    assertThat(afterMessageProcessorNotification.getProcessor(), is(mockMessageProcessor));
    assertThat(afterMessageProcessorNotification.getExceptionThrown(), nullValue());
  }

  @Test
  public void ignoresSuccessfulNotificationIfDisabled() throws MuleException {
    final List<ServerNotification> serverNotifications = new ArrayList<>();
    when(mockMessageProcessor.process(mockMuleEvent)).thenReturn(mockResultMuleEvent);
    when(mockNextInterceptor.execute(mockMessageProcessor, mockMuleEvent)).thenReturn(mockResultMuleEvent);
    when(mockNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)).thenReturn(true);
    doAnswer(invocationOnMock -> {
      serverNotifications.add((ServerNotification) invocationOnMock.getArguments()[0]);
      return null;
    }).when(mockNotificationManager).fireNotification(any(ServerNotification.class));

    MuleEvent result = messageProcessorNotificationExecutionInterceptor.execute(mockMessageProcessor, mockMuleEvent);

    assertThat(result, is(mockResultMuleEvent));
    assertThat(serverNotifications.size(), is(0));
  }

  /**
   * Validates that event to be processed is set to RequestContext for those cases whenever a messageProcessor modifies the RC
   * during its execution.
   */
  @Test
  public void requestContextSetBeforeProcessingEventBlockingPrcocessor() throws MuleException {
    final List<ServerNotification> serverNotifications = new ArrayList<>();
    when(mockMessageProcessor.process(mockMuleEvent)).thenReturn(mockResultMuleEvent);
    when(mockNextInterceptor.execute(mockMessageProcessor, mockMuleEvent)).thenReturn(mockResultMuleEvent);
    when(mockNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)).thenReturn(true);
    doAnswer(invocationOnMock -> {
      serverNotifications.add((ServerNotification) invocationOnMock.getArguments()[0]);
      return null;
    }).when(mockNotificationManager).fireNotification(any(ServerNotification.class));

    setCurrentEvent(mockMuleEventPreviousExecution);

    MuleEvent result = messageProcessorNotificationExecutionInterceptor.execute(mockMessageProcessor, mockMuleEvent);

    assertThat(result, is(mockResultMuleEvent));
    assertThat(serverNotifications.size(), is(0));

    assertThat(getCurrentEvent(), is(mockMuleEvent));
    assertThat(getCurrentEvent(), not(mockMuleEventPreviousExecution));
  }

  /**
   * Validates that event to be processed is copied and set to RequestContext for those cases whenever a messageProcessor modifies
   * the RC during its execution.
   */
  @Test
  public void requestContextSetBeforeProcessingEventNonBlockingPrcocessor() throws MuleException {
    final List<ServerNotification> serverNotifications = new ArrayList<>();
    mockMessageProcessor = mock(MessageProcessor.class,
                                withSettings().extraInterfaces(NonBlockingMessageProcessor.class));

    when(mockMessageProcessor.process(mockMuleEvent)).thenReturn(mockResultMuleEvent);
    when(mockNextInterceptor.execute(Mockito.eq(mockMessageProcessor), any(MuleEvent.class)))
        .thenReturn(mockResultMuleEvent);

    String muleEventIdToProcess = UUID.getUUID();
    when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);
    when(mockMuleEvent.getReplyToHandler()).thenReturn(mockReplyToHandler);
    when(mockMuleEvent.getId()).thenReturn(muleEventIdToProcess);

    when(mockNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)).thenReturn(true);
    doAnswer(invocationOnMock -> {
      serverNotifications.add((ServerNotification) invocationOnMock.getArguments()[0]);
      return null;
    }).when(mockNotificationManager).fireNotification(any(ServerNotification.class));

    setCurrentEvent(mockMuleEventPreviousExecution);

    MuleEvent result = messageProcessorNotificationExecutionInterceptor.execute(mockMessageProcessor, mockMuleEvent);

    assertThat(result, is(mockResultMuleEvent));
    assertThat(serverNotifications.size(), is(0));

    assertThat(getCurrentEvent(), not(mockMuleEvent));
    assertThat(getCurrentEvent(), not(mockMuleEventPreviousExecution));
  }

  @Test
  public void testExecutionFailure() throws MuleException {
    final List<ServerNotification> serverNotifications = new ArrayList<>();
    when(mockNextInterceptor.execute(mockMessageProcessor, mockMuleEvent)).thenThrow(mockMessagingException);
    when(mockPipeline.getProcessorPath(mockMessageProcessor)).thenReturn("hi");
    when(mockMuleEvent.isNotificationsEnabled()).thenReturn(true);
    when(mockNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)).thenReturn(true);
    doAnswer(invocationOnMock -> {
      serverNotifications.add((ServerNotification) invocationOnMock.getArguments()[0]);
      return null;
    }).when(mockNotificationManager).fireNotification(any(ServerNotification.class));
    try {
      messageProcessorNotificationExecutionInterceptor.execute(mockMessageProcessor, mockMuleEvent);
      fail("Exception should be thrown");
    } catch (MessagingException e) {
    }
    assertThat(serverNotifications.size(), is(2));
    MessageProcessorNotification beforeMessageProcessorNotification = (MessageProcessorNotification) serverNotifications.get(0);
    MessageProcessorNotification afterMessageProcessorNotification = (MessageProcessorNotification) serverNotifications.get(1);
    assertThat(beforeMessageProcessorNotification.getAction(), is(MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE));
    assertThat(beforeMessageProcessorNotification.getProcessor(), is(mockMessageProcessor));
    assertThat(beforeMessageProcessorNotification.getExceptionThrown(), nullValue());
    assertThat(afterMessageProcessorNotification.getAction(), is(MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE));
    assertThat(afterMessageProcessorNotification.getProcessor(), is(mockMessageProcessor));
    assertThat(afterMessageProcessorNotification.getExceptionThrown(), is(mockMessagingException));
  }

  @Test
  public void ignoresFailureNotificationIfDisabled() throws MuleException {
    final List<ServerNotification> serverNotifications = new ArrayList<>();
    when(mockNextInterceptor.execute(mockMessageProcessor, mockMuleEvent)).thenThrow(mockMessagingException);
    when(mockNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)).thenReturn(true);
    doAnswer(invocationOnMock -> {
      serverNotifications.add((ServerNotification) invocationOnMock.getArguments()[0]);
      return null;
    }).when(mockNotificationManager).fireNotification(any(ServerNotification.class));

    try {
      messageProcessorNotificationExecutionInterceptor.execute(mockMessageProcessor, mockMuleEvent);
      fail("Exception should be thrown");
    } catch (MessagingException e) {
    }

    assertThat(serverNotifications.size(), is(0));
  }
}
