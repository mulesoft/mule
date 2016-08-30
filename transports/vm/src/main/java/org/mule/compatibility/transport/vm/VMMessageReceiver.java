/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.message.MuleCompatibilityMessage;
import org.mule.compatibility.core.message.MuleCompatibilityMessageBuilder;
import org.mule.compatibility.core.transport.ContinuousPollingReceiverWorker;
import org.mule.compatibility.core.transport.PollingReceiverWorker;
import org.mule.compatibility.core.transport.TransactedPollingMessageReceiver;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.util.queue.Queue;
import org.mule.runtime.core.util.queue.QueueSession;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

/**
 * <code>VMMessageReceiver</code> is a listener for events from a Mule service which then simply passes the events on to the
 * target service.
 */
public class VMMessageReceiver extends TransactedPollingMessageReceiver {

  private VMConnector connector;

  public VMMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint) throws CreateException {
    super(connector, flowConstruct, endpoint);
    this.setReceiveMessagesInTransaction(endpoint.getTransactionConfig().isTransacted());
    this.connector = (VMConnector) connector;
  }

  /*
   * We only need to start scheduling this receiver if event queueing is enabled on the connector; otherwise events are delivered
   * via onEvent/onCall.
   */
  @Override
  protected void schedule() throws RejectedExecutionException, NullPointerException, IllegalArgumentException {
    if (!endpoint.getExchangePattern().hasResponse()) {
      super.schedule();
    }
  }

  @Override
  protected void doDispose() {
    // template method
  }

  @Override
  protected void doConnect() throws Exception {
    if (!endpoint.getExchangePattern().hasResponse()) {
      // Ensure we can create a vm queue
      QueueSession queueSession = connector.getQueueSession();
      Queue q = queueSession.getQueue(endpoint.getEndpointURI().getAddress());
      if (logger.isDebugEnabled()) {
        logger.debug("Current queue depth for queue: " + endpoint.getEndpointURI().getAddress() + " is: " + q.size());
      }
    }
  }

  @Override
  protected void doDisconnect() throws Exception {
    // template method
  }

  public void onMessage(MuleMessage message) throws MuleException {
    // Rewrite the message to treat it as a new message
    MuleCompatibilityMessage newMessage =
        (MuleCompatibilityMessage) new MuleCompatibilityMessageBuilder(message).payload(message.getPayload()).build();
    routeMessage(newMessage);
  }

  public MuleMessage onCall(final MuleCompatibilityMessage message) throws MuleException {

    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    // TODO remove code to change message MuleContext once MULE-7357 gets fixed
    try {
      Thread.currentThread().setContextClassLoader(endpoint.getMuleContext().getExecutionClassLoader());
      ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();
      MuleEvent resultEvent = executionTemplate.execute(() -> {
        MuleEvent event = routeMessage(message);
        if (event != null && !VoidMuleEvent.getInstance().equals(event) && getEndpoint().getExchangePattern().hasResponse()) {
          return event;
        }
        return null;
      });
      if (resultEvent != null) {
        return resultEvent.getMessage();
      } else {
        return null;
      }
    } catch (MessagingException e) {
      // Already handled by TransactionTemplate, return ES result
      return e.getMuleMessage();
    } catch (MuleException e) {
      endpoint.getMuleContext().getExceptionListener().handleException(e);
      throw e;
    } catch (Exception e) {
      endpoint.getMuleContext().getExceptionListener().handleException(e);
      throw new DefaultMuleException(e);
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }
  }

  /**
   * It's impossible to process all messages in the receive transaction
   */
  @Override
  protected List<MuleMessage> getMessages() throws Exception {
    if (isReceiveMessagesInTransaction()) {
      MuleMessage message = getFirstMessage();
      if (message == null) {
        return null;
      }

      List<MuleMessage> messages = new ArrayList<>(1);
      messages.add(message);
      return messages;
    } else {
      return getFirstMessages();
    }
  }

  protected List<MuleMessage> getFirstMessages() throws Exception {
    // The queue from which to pull events
    QueueSession qs = connector.getTransactionalResource(endpoint);
    Queue queue = qs.getQueue(endpoint.getEndpointURI().getAddress());

    // The list of retrieved messages that will be returned
    List<MuleMessage> messages = new LinkedList<>();

    int batchSize = getBatchSize(queue.size());

    // try to get the first event off the queue
    MuleMessage message = getMessage(queue, connector.getQueueTimeout());

    if (message != null) {
      messages.add(message);

      // keep batching if more events are available
      for (int i = 0; i < batchSize && message != null; i++) {
        message = getMessage(queue, 0);
        if (message != null) {
          messages.add(message);
        }
      }
    }

    // let our workManager handle the batch of events
    return messages;
  }

  private MuleMessage getMessage(Queue queue, int timeout) throws InterruptedException {
    Serializable polledItem = queue.poll(timeout);
    if (polledItem instanceof MuleEvent) {
      return ((MuleEvent) polledItem).getMessage();
    } else {
      return (MuleMessage) polledItem;
    }
  }

  protected MuleMessage getFirstMessage() throws Exception {
    // The queue from which to pull events
    QueueSession qs = connector.getTransactionalResource(endpoint);
    Queue queue = qs.getQueue(endpoint.getEndpointURI().getAddress());

    return getMessage(queue, connector.getQueueTimeout());
  }

  @Override
  protected boolean hasNoMessages() {
    try {
      QueueSession qs = connector.getQueueSession();
      Queue queue = qs.getQueue(endpoint.getEndpointURI().getAddress());
      return queue.size() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  protected MuleEvent processMessage(Object msg) throws Exception {
    return routeMessage((MuleCompatibilityMessage) msg);
  }

  /*
   * We create our own "polling" worker here since we need to evade the standard scheduler.
   */
  @Override
  protected PollingReceiverWorker createWork() {
    return new ContinuousPollingReceiverWorker(this);
  }
}
