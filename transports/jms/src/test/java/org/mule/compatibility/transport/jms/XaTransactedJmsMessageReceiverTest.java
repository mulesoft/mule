/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.compatibility.core.api.endpoint.EndpointURI;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.transaction.XaTransaction;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class XaTransactedJmsMessageReceiverTest extends AbstractMuleTestCase {


  @Mock
  private JmsSupport jmsSupport;
  @Mock
  private JmsConnector mockJmsConnector;
  @Mock
  private FlowConstruct mockFlowConstruct;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private InboundEndpoint mockInboundEndpoint;
  @Mock
  private MessageConsumer messageConsumer;
  @Mock
  private Transaction transaction;

  private ExecutorService executor;

  @Before
  public void setUpMocks() throws JMSException {
    when(mockJmsConnector.getJmsSupport()).thenReturn(jmsSupport);
    when(mockJmsConnector.isConnected()).thenReturn(true);
    when(mockJmsConnector.getTopicResolver()).thenReturn(mock(JmsTopicResolver.class));
    when(mockJmsConnector.getSelector(mockInboundEndpoint)).thenReturn(null);
    when(mockJmsConnector.getSession(mockInboundEndpoint))
        .thenReturn(mock(Session.class, withSettings().extraInterfaces(XaTransaction.MuleXaObject.class)));

    when(mockInboundEndpoint.getEndpointURI()).thenReturn(mock(EndpointURI.class));
    when(mockInboundEndpoint.getProperties()).thenReturn(emptyMap());
    when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);
  }

  @After
  public void clearInterruptedFlag() {
    Thread.interrupted();
  }

  @After
  public void shutdownExecutor() {
    if (executor != null) {
      executor.shutdown();
    }
  }

  @Test
  public void testTopicReceiverShouldBeStartedOnlyInPrimaryNode() throws Exception {
    when(mockJmsConnector.getTopicResolver().isTopic(mockInboundEndpoint)).thenReturn(true);
    when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);
    XaTransactedJmsMessageReceiver messageReceiver =
        new XaTransactedJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint);
    assertThat("receiver must be started only in primary node", messageReceiver.shouldConsumeInEveryNode(), is(false));
  }

  @Test
  public void testQueueReceiverShouldBeStartedInEveryNode() throws Exception {
    when(mockJmsConnector.getTopicResolver().isTopic(mockInboundEndpoint)).thenReturn(false);
    when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);
    XaTransactedJmsMessageReceiver messageReceiver =
        new XaTransactedJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint);
    assertThat("receiver must be started only in primary node", messageReceiver.shouldConsumeInEveryNode(), is(true));
  }

  private void doDisconnectExceptionTest(final Exception exceptionToThrow) throws Exception {
    when(mockJmsConnector.getTopicResolver().isTopic(mockInboundEndpoint)).thenReturn(false);
    when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);

    XaTransactedJmsMessageReceiver messageReceiver =
        spy(new XaTransactedJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint));
    doReturn(messageConsumer).when(messageReceiver).createConsumer();

    when(messageConsumer.receive(messageReceiver.timeout)).thenAnswer(invocation -> {
      Thread.currentThread().interrupt();
      throw exceptionToThrow;
    });

    doAnswer(invocation -> {
      assertThat(Thread.currentThread().isInterrupted(), is(true));
      return null;
    }).when(transaction).setRollbackOnly();
    TransactionCoordination.getInstance().bindTransaction(transaction);
    messageReceiver.getMessages();

    verify(transaction).setRollbackOnly();
  }


  @Test
  public void jmsExceptionWhileDisconnecting() throws Exception {
    doDisconnectExceptionTest(new JMSException("Test exception"));
  }

  @Test
  public void undeclaredThrowableExceptionWhileDisconnecting() throws Exception {
    doDisconnectExceptionTest(new UndeclaredThrowableException(new RuntimeException(new JMSException("Test exception"))));
  }

  @Test(expected = RuntimeException.class)
  public void otherExceptionWhileDisconnecting() throws Exception {
    doDisconnectExceptionTest(new RuntimeException("Test exception"));
  }

  @Test
  public void disconnectFromOtherThread() throws Exception {
    final Latch receivingLatch = new Latch();
    final Latch disconnectedLatch = new Latch();

    final MessageConsumer consumer = mock(MessageConsumer.class);
    when(consumer.receive(anyLong())).then(buildLatchedReceiveAnswer(receivingLatch, disconnectedLatch));

    when(jmsSupport.createConsumer(any(Session.class), any(Destination.class), anyString(), anyBoolean(), anyString(),
                                   anyBoolean(), eq(mockInboundEndpoint))).thenReturn(consumer);

    final XaTransactedJmsMessageReceiver messageReceiver =
        new XaTransactedJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint);

    executor = newSingleThreadExecutor();
    executor.execute(buildReceiverPoller(messageReceiver));

    receivingLatch.await(10, TimeUnit.SECONDS);
    messageReceiver.disconnect();

    verify(mockJmsConnector).closeQuietly(eq(consumer));

    disconnectedLatch.countDown();
  }

  @Test
  public void receiverSharedAmongThreads() throws Exception {
    final CountDownLatch receivingLatch = new CountDownLatch(3);
    final Latch disconnectedLatch = new Latch();

    final MessageConsumer consumer1 = mock(MessageConsumer.class, "consumer1");
    final MessageConsumer consumer2 = mock(MessageConsumer.class, "consumer2");
    final MessageConsumer consumer3 = mock(MessageConsumer.class, "consumer3");
    when(consumer1.receive(anyLong())).then(buildLatchedReceiveAnswer(receivingLatch, disconnectedLatch));
    when(consumer2.receive(anyLong())).then(buildLatchedReceiveAnswer(receivingLatch, disconnectedLatch));
    when(consumer3.receive(anyLong())).then(buildLatchedReceiveAnswer(receivingLatch, disconnectedLatch));

    when(jmsSupport.createConsumer(any(Session.class), any(Destination.class), anyString(), anyBoolean(), anyString(),
                                   anyBoolean(), eq(mockInboundEndpoint))).thenReturn(consumer1, consumer2, consumer3);

    final XaTransactedJmsMessageReceiver messageReceiver =
        new XaTransactedJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint);

    executor = newFixedThreadPool(3);
    executor.execute(buildReceiverPoller(messageReceiver));
    executor.execute(buildReceiverPoller(messageReceiver));
    executor.execute(buildReceiverPoller(messageReceiver));

    receivingLatch.await(10, TimeUnit.SECONDS);
    messageReceiver.disconnect();

    verify(mockJmsConnector).closeQuietly(eq(consumer1));
    verify(mockJmsConnector).closeQuietly(eq(consumer2));
    verify(mockJmsConnector).closeQuietly(eq(consumer3));

    disconnectedLatch.countDown();
  }

  protected Answer<Message> buildLatchedReceiveAnswer(final CountDownLatch receivingLatch, final Latch disconnectedLatch) {
    return invocation -> {
      receivingLatch.countDown();
      disconnectedLatch.await();
      throw new JMSException("Mocking disconnection");
    };
  }

  protected Runnable buildReceiverPoller(final XaTransactedJmsMessageReceiver messageReceiver) {
    return () -> {
      try {
        messageReceiver.poll();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
}
