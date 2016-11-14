/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms;

import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.lookupEndpointBuilder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;
import static org.mule.runtime.core.util.NumberUtils.toInt;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointException;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.transport.AbstractMessageDispatcher;
import org.mule.compatibility.core.util.concurrent.WaitableBoolean;
import org.mule.compatibility.transport.jms.i18n.JmsMessages;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.execution.CompletionHandler;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;

/**
 * <code>JmsMessageDispatcher</code> is responsible for dispatching messages to JMS destinations. All JMS semantics apply and
 * settings such as replyTo and QoS properties are read from the event properties or defaults are used (according to the JMS
 * specification)
 */
public class JmsMessageDispatcher extends AbstractMessageDispatcher {

  private JmsConnector connector;
  private boolean disableTemporaryDestinations = false;
  private boolean returnOriginalMessageAsReply = false;

  public JmsMessageDispatcher(OutboundEndpoint endpoint) {
    super(endpoint);
    this.connector = (JmsConnector) endpoint.getConnector();
    disableTemporaryDestinations = connector.isDisableTemporaryReplyToDestinations()
        || ("true".equals(endpoint.getProperty(JmsConstants.DISABLE_TEMP_DESTINATIONS_PROPERTY)));
    returnOriginalMessageAsReply = connector.isReturnOriginalMessageAsReply()
        || ("true".equals(endpoint.getProperty(JmsConstants.RETURN_ORIGINAL_MESSAGE_PROPERTY)));
    if (returnOriginalMessageAsReply && !disableTemporaryDestinations) {
      logger
          .warn("The returnOriginalMessageAsReply property will be ignored because disableTemporaryReplyToDestinations=false.  You need to disable temporary ReplyTo destinations in order for this propery to take effect.");
    }
    logger.warn("Starting patched JmsMessageReceiver");
  }

  @Override
  protected void doDispatch(Event event) throws Exception {
    if (connector.getConnection() == null) {
      throw new IllegalStateException("No JMS Connection");
    }
    dispatchMessage(event, false, null);
  }

  @Override
  protected void doConnect() throws Exception {
    // template method
  }

  @Override
  protected void doDisconnect() throws Exception {
    // template method
  }

  protected boolean isDisableTemporaryDestinations() {
    return disableTemporaryDestinations;
  }

  private InternalMessage dispatchMessage(Event event, boolean doSend,
                                          final CompletionHandler<InternalMessage, Exception> completionHandler)
      throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("dispatching on endpoint: " + endpoint.getEndpointURI() + ". MuleEvent id is: " + event
          + ". Outbound transformers are: " + endpoint.getMessageProcessors());
    }

    final Message jmsMessage = getJmsMessagePayload(event);
    final InternalMessage muleRequestMessage = event.getMessage();

    boolean transacted = isTransacted();
    final boolean useReplyToDestination = isUseReplyToDestination(event, doSend, transacted);
    final boolean topic = connector.getTopicResolver().isTopic(endpoint, true);

    // QoS support
    final long ttl = muleRequestMessage.getOutboundProperty(JmsConstants.TIME_TO_LIVE_PROPERTY, Message.DEFAULT_TIME_TO_LIVE);
    int priority = muleRequestMessage.getOutboundProperty(JmsConstants.PRIORITY_PROPERTY, Message.DEFAULT_PRIORITY);
    boolean persistent =
        muleRequestMessage.getOutboundProperty(JmsConstants.PERSISTENT_DELIVERY_PROPERTY, connector.isPersistentDelivery());

    // If we are honouring the current QoS message headers we need to use the ones set on the current message
    if (connector.isHonorQosHeaders()) {
      Object priorityProp = muleRequestMessage.getInboundProperty(JmsConstants.JMS_PRIORITY);
      Object deliveryModeProp = muleRequestMessage.getInboundProperty(JmsConstants.JMS_DELIVERY_MODE);

      if (priorityProp != null) {
        priority = toInt(priorityProp);
      }
      if (deliveryModeProp != null) {
        persistent = toInt(deliveryModeProp) == DeliveryMode.PERSISTENT;
      }
    }

    Session session = null;
    MessageProducer producer = null;
    boolean delayedCleanup = false;

    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Sending message of type " + ClassUtils.getSimpleName(jmsMessage.getClass()));
        logger.debug("Sending JMS Message type " + jmsMessage.getJMSType() + "\n  JMSMessageID=" + jmsMessage.getJMSMessageID()
            + "\n  JMSCorrelationID=" + jmsMessage.getJMSCorrelationID() + "\n  JMSDeliveryMode="
            + (persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT) + "\n  JMSPriority=" + priority
            + "\n  JMSReplyTo=" + jmsMessage.getJMSReplyTo());
      }

      session = connector.getTransactionalResource(endpoint);
      producer = createProducer(session, topic);

      final Destination replyTo = getReplyToDestination(jmsMessage, session, event, useReplyToDestination, topic);

      // Set the replyTo property
      if (replyTo != null) {
        jmsMessage.setJMSReplyTo(replyTo);
      }

      jmsMessage.setStringProperty(MULE_CORRELATION_ID_PROPERTY, resolveMuleCorrelationId(event));
      // For the "One-way" scenario, we can resolve the Correlation ID.
      // For the "Request-Response" scenario, this cannot be done because we are supporting only the
      // JMS Message ID Pattern. In Mule 4 JMS should support this pattern, and the JMS Correlation ID Pattern
      if (!endpoint.getExchangePattern().hasResponse()) {
        jmsMessage.setJMSCorrelationID(resolveJmsCorrelationId(event));
      } else {
        jmsMessage.setJMSCorrelationID(event.getLegacyCorrelationId());
      }

      // Allow overrides to alter the message if necessary
      processMessage(jmsMessage, event);

      if (useReplyToDestination && replyTo != null) {
        final int timeout = endpoint.getResponseTimeout();
        try {
          if (topic) {
            return internalBlockingSendAndAwait(session, producer, replyTo, jmsMessage, topic, ttl, priority, persistent,
                                                timeout, event);

          } else {
            return internalBlockingSendAndReceive(session, producer, replyTo, jmsMessage, topic, ttl, priority, persistent,
                                                  timeout, event);
          }
        } finally {
          closeReplyQueue(session, replyTo);
        }
      } else {
        return internalSend(producer, jmsMessage, topic, ttl, priority, persistent);
      }
    } finally {
      if (!delayedCleanup) {
        connector.closeQuietly(producer);
        closeSession(session);
      }
    }
  }

  private InternalMessage internalSend(MessageProducer producer, Message jmsMessage, boolean topic, long ttl, int priority,
                                       boolean persistent)
      throws Exception {
    connector.getJmsSupport().send(producer, jmsMessage, persistent, priority, ttl, topic, endpoint);
    return returnOriginalMessageAsReply ? createMuleMessage(jmsMessage) : null;
  }

  private InternalMessage internalBlockingSendAndAwait(Session session, MessageProducer producer, Destination replyTo,
                                                       Message jmsMessage, boolean topic, long ttl, int priority,
                                                       boolean persistent, int timeout, Event event)
      throws Exception {
    connector.getJmsSupport().send(producer, jmsMessage, persistent, priority, ttl, topic, endpoint);

    final MessageConsumer consumer = createReplyToConsumer(jmsMessage, event, session, replyTo, topic);

    try {
      // need to register a listener for a topic
      Latch latch = new Latch();
      LatchReplyToListener listener = new LatchReplyToListener(latch);
      consumer.setMessageListener(listener);


      if (logger.isDebugEnabled()) {
        logger.debug("Waiting for response event for: " + timeout + " ms on " + replyTo);
      }

      latch.await(timeout, TimeUnit.MILLISECONDS);
      consumer.setMessageListener(null);
      listener.release();
      return createResponseMuleMessage(listener.getMessage(), replyTo);
    } finally {
      closeConsumer(consumer);
    }
  }

  private InternalMessage internalBlockingSendAndReceive(Session session, MessageProducer producer, Destination replyTo,
                                                         Message jmsMessage, boolean topic, long ttl, int priority,
                                                         boolean persistent, int timeout, Event event)
      throws Exception {
    connector.getJmsSupport().send(producer, jmsMessage, persistent, priority, ttl, topic, endpoint);

    if (logger.isDebugEnabled()) {
      logger.debug("Waiting for non-blocking response event for: " + timeout + " ms on " + replyTo);
    }
    final MessageConsumer consumer = createReplyToConsumer(jmsMessage, event, session, replyTo, topic);
    try {
      Message result = consumer.receive(timeout);
      return createResponseMuleMessage(result, replyTo);
    } finally {
      closeConsumer(consumer);
    }
  }

  private MessageProducer createProducer(Session session, boolean topic) throws JMSException {
    final Destination dest = connector.getJmsSupport().createDestination(session, endpoint);
    return connector.getJmsSupport().createProducer(session, dest, topic);
  }

  private Message getJmsMessagePayload(Event event) throws DispatchException {
    Object message = event.getMessage().getPayload().getValue();
    if (!(message instanceof Message)) {
      throw new DispatchException(JmsMessages.checkTransformer("JMS message", message.getClass(), connector.getName()),
                                  getEndpoint());
    }
    return (Message) message;
  }

  private boolean isUseReplyToDestination(Event event, boolean doSend, boolean transacted) {
    return returnResponse(event, doSend) && !transacted;
  }

  private boolean isTransacted() {
    Transaction muleTx = TransactionCoordination.getInstance().getTransaction();
    return (muleTx != null && muleTx.hasResource(connector.getConnection()) || endpoint.getTransactionConfig().isTransacted());
  }

  private void closeReplyQueue(Session session, Destination replyTo) {
    if (replyTo != null && (replyTo instanceof TemporaryQueue || replyTo instanceof TemporaryTopic)) {
      if (replyTo instanceof TemporaryQueue) {
        connector.closeQuietly((TemporaryQueue) replyTo);
      } else {
        connector.closeQuietly((TemporaryTopic) replyTo);
      }
    }
  }

  private void closeConsumer(MessageConsumer consumer) {
    connector.closeQuietly(consumer);
  }

  private void closeSession(Session session) {
    // If the session is from the current transaction, it is up to the
    // transaction to close it.
    if (session != null && !isTransacted()) {
      connector.closeQuietly(session);
    }
  }

  private InternalMessage createResponseMuleMessage(Message result, Destination replyTo) throws Exception {
    if (result == null) {
      logger.debug("No message was returned via replyTo destination " + replyTo);
      return createNullMuleMessage();
    } else {
      return createMessageWithJmsMessagePayload(result);
    }
  }

  /**
   * Resolve the value of correlationID that should be used for the JMS Message. This is done here and not as part of
   * transformation because of the need for visibility of MuleEvent and OutboundEndpoint.
   *
   * @param event the current MuleEvent
   */
  protected String resolveJmsCorrelationId(Event event) throws JMSException {
    return resolveMuleCorrelationId(event);
  }

  /**
   * Resolve the value of Mule correlationID that should be send as a JMS header. This is done here and not as part of
   * transformation because of the need for visibility of MuleEvent and OutboundEndpoint.
   *
   * @param event the current MuleEvent
   */
  private String resolveMuleCorrelationId(Event event) throws JMSException {
    return event.getCorrelationId();
  }

  protected InternalMessage createMessageWithJmsMessagePayload(Message jmsMessage) throws Exception {
    Object payload = JmsMessageUtils.toObject(jmsMessage, connector.getSpecification(), endpoint.getEncoding());
    return InternalMessage.builder(createMuleMessage(jmsMessage)).payload(payload).build();
  }

  /**
   * This method is called before the current message is transformed. It can be used to do any message body or header processing
   * before the transformer is called.
   *
   * @param message the current Message Being processed
   * @throws Exception
   */
  protected InternalMessage preTransformMessage(InternalMessage message) throws Exception {
    return message;
  }

  @Deprecated
  protected void handleMultiTx(Session session) throws Exception {
    logger.debug("Multi-transaction support is not available in Mule Community Edition.");
  }

  @Override
  protected InternalMessage doSend(Event event) throws Exception {
    return dispatchMessage(event, true, null);
  }

  @Override
  protected void doDispose() {
    // template method
  }

  /**
   * This method is called once the JMS message is created. It allows subclasses to alter the message if necessary.
   *
   * @param msg The JMS message that will be sent
   * @param event the current event
   * @throws JMSException if the JmsMessage cannot be written to, this should not happen because the JMSMessage passed in will
   *         always be newly created
   */
  protected void processMessage(Message msg, Event event) throws JMSException {
    // template Method
  }

  /**
   * Some JMS implementations do not support ReplyTo or require some further fiddling of the message
   *
   * @param msg The JMS message that will be sent
   * @param event the current event
   * @return true if this request should honour any JMSReplyTo settings on the message
   * @throws JMSException if the JmsMessage cannot be written to, this should not happen because the JMSMessage passed in will
   *         always be newly created
   */
  protected boolean isHandleReplyTo(Message msg, Event event) throws JMSException {
    return connector.supportsProperty(JmsConstants.JMS_REPLY_TO);
  }

  protected MessageConsumer createReplyToConsumer(Message jmsMessage, Event event, Session session, Destination replyTo,
                                                  boolean topic)
      throws JMSException {
    String selector = null;
    // Only used by topics
    String durableName;
    // If we're not using
    if (!(replyTo instanceof TemporaryQueue || replyTo instanceof TemporaryTopic)) {
      // Since we are supporting the JMS Message ID Pattern for request-response, the correlationID will be null
      // if it is not manually set up, and the selector must be set to the messageID.
      String jmsCorrelationId = jmsMessage.getJMSCorrelationID();
      if (jmsCorrelationId == null) {
        jmsCorrelationId = jmsMessage.getJMSMessageID();
      }
      selector = "JMSCorrelationID='" + jmsCorrelationId + "'";
      if (logger.isDebugEnabled()) {
        logger.debug("ReplyTo Selector is: " + selector);
      }
    }

    // We need to set the durableName and Selector if using topics
    if (topic) {
      String tempDurable = (String) event.getVariable(JmsConstants.DURABLE_PROPERTY).getValue();
      boolean durable = connector.isDurable();
      if (tempDurable != null) {
        durable = Boolean.valueOf(tempDurable);
      }
      // Get the durable subscriber name if there is one
      durableName = (String) event.getVariable(JmsConstants.DURABLE_NAME_PROPERTY).getValue();
      if (durableName == null && durable && topic) {
        durableName = "mule." + connector.getName() + "." + event.getContext().getOriginatingConnectorName();
        if (logger.isDebugEnabled()) {
          logger.debug("Jms Connector for this receiver is durable but no durable name has been specified. Defaulting to: "
              + durableName);
        }
      }
    }
    return connector.getJmsSupport().createConsumer(session, replyTo, selector, connector.isNoLocal(), null, topic, endpoint);
  }

  protected Destination getReplyToDestination(Message message, Session session, Event event, boolean remoteSync,
                                              boolean topic)
      throws JMSException, EndpointException, InitialisationException {
    Destination replyTo = null;

    // Some JMS implementations might not support the ReplyTo property.
    if (isHandleReplyTo(message, event)) {

      Object tempReplyTo = event.getMessage().getOutboundProperty(JmsConstants.JMS_REPLY_TO);
      if (tempReplyTo == null) {
        // It may be a Mule URI or global endpoint Ref
        tempReplyTo = event.getMessage().getOutboundProperty(MULE_REPLY_TO_PROPERTY);
        if (tempReplyTo != null) {
          int i = tempReplyTo.toString().indexOf("://");
          if (i > -1) {
            tempReplyTo = tempReplyTo.toString().substring(i + 3);
          } else {
            EndpointBuilder epb = lookupEndpointBuilder(endpoint.getMuleContext().getRegistry(), tempReplyTo.toString());
            if (epb != null) {
              tempReplyTo = epb.buildOutboundEndpoint().getEndpointURI().getAddress();
            }
          }
        }
      }
      if (tempReplyTo != null) {
        if (tempReplyTo instanceof Destination) {
          replyTo = (Destination) tempReplyTo;
        } else {
          // TODO AP should this drill-down be moved into the resolver as well?
          boolean replyToTopic = false;
          String reply = tempReplyTo.toString();
          int i = reply.indexOf(":");
          if (i > -1) {
            // TODO MULE-1409 this check will not work for ActiveMQ 4.x,
            // as they have temp-queue://<destination> and temp-topic://<destination> URIs
            // Extract to a custom resolver for ActiveMQ4.x
            // The code path can be exercised, e.g. by a LoanBrokerMuleTestCase
            String qtype = reply.substring(0, i);
            replyToTopic = JmsConstants.TOPIC_PROPERTY.equalsIgnoreCase(qtype);
            reply = reply.substring(i + 1);
          }
          replyTo = connector.getJmsSupport().createDestination(session, reply, replyToTopic, endpoint);
        }
      }
      // Are we going to wait for a return event ?
      if (remoteSync && replyTo == null && !disableTemporaryDestinations) {
        replyTo = connector.getJmsSupport().createTemporaryDestination(session, topic);
      }
    }
    return replyTo;

  }

  protected class LatchReplyToListener implements MessageListener {

    private final Latch latch;
    private volatile Message message;
    private final WaitableBoolean released = new WaitableBoolean(false);

    public LatchReplyToListener(Latch latch) {
      this.latch = latch;
    }

    public Message getMessage() {
      return message;
    }

    public void release() {
      released.set(true);
    }

    @Override
    public void onMessage(Message message) {
      this.message = message;
      latch.countDown();
      try {
        released.whenTrue(null);
      } catch (InterruptedException e) {
        // ignored
      }
    }
  }

  @Override
  protected Event applyOutboundTransformers(Event event) throws MuleException {
    try {
      event = Event.builder(event).message(preTransformMessage(event.getMessage())).build();
    } catch (Exception e) {
      throw new TransformerException(CoreMessages.failedToInvoke("preTransformMessage"), e);
    }
    return super.applyOutboundTransformers(event);
  }

}
