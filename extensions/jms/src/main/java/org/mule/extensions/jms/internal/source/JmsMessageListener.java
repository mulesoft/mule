/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.source;

import static org.mule.extensions.jms.internal.common.JmsCommons.evaluateMessageAck;
import static org.mule.extensions.jms.internal.common.JmsCommons.resolveMessageContentType;
import static org.mule.extensions.jms.internal.common.JmsCommons.resolveMessageEncoding;
import static org.mule.extensions.jms.internal.common.JmsCommons.resolveOverride;
import static org.mule.extensions.jms.internal.config.InternalAckMode.TRANSACTED;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.exception.JmsExtensionException;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.internal.config.InternalAckMode;
import org.mule.extensions.jms.internal.config.JmsConfig;
import org.mule.extensions.jms.internal.connection.JmsSession;
import org.mule.extensions.jms.internal.connection.session.JmsSessionManager;
import org.mule.extensions.jms.internal.message.JmsResultFactory;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.slf4j.Logger;

/**
 * {@link MessageListener} for the {@link JmsListener} to subscribe to a TOPIC or QUEUE receives {@link Message} and
 * dispatch those messages through the flow.
 *
 * @since 4.0
 */
public final class JmsMessageListener implements MessageListener {

  private static final Logger LOGGER = getLogger(JmsMessageListener.class);
  private final JmsSession session;
  private final SourceCallback<Object, JmsAttributes> sourceCallback;
  private final JmsListenerLock jmsLock;
  private final InternalAckMode ackMode;
  private final String encoding;
  private final String contentType;
  private final JmsConfig config;
  private final JmsSessionManager sessionManager;
  private final JmsSupport jmsSupport;
  private final JmsResultFactory resultFactory = new JmsResultFactory();

  /**
   * Creates a new instance of a {@link JmsMessageListener}
   *
   * @param session        the session to create the JMS Consumer
   * @param config         JMS
   * @param jmsLock        the lock to use to synchronize the message dispatch
   * @param sessionManager manager to store the session and ACK ID of each dispatched message
   * @param sourceCallback callback use to dispatch the {@link Message} to the mule flow
   * @param jmsSupport     JMS Support that communicates the used specification
   * @param ackMode        Acknowledgement mode to use to consume the messages
   * @param encoding       Default encoding if the consumed message doesn't provide one
   * @param contentType    Default contentType if the consumed message doesn't provide one
   */
  JmsMessageListener(JmsSession session, JmsConfig config, JmsListenerLock jmsLock, JmsSessionManager sessionManager,
                     SourceCallback<Object, JmsAttributes> sourceCallback, JmsSupport jmsSupport, InternalAckMode ackMode,
                     String encoding, String contentType) {
    this.session = session;
    this.sourceCallback = sourceCallback;
    this.jmsLock = jmsLock;
    this.ackMode = ackMode;
    this.encoding = encoding;
    this.contentType = contentType;
    this.config = config;
    this.sessionManager = sessionManager;
    this.jmsSupport = jmsSupport;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onMessage(Message message) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Received message on session: " + session.get().toString());
    }

    SourceCallbackContext context = sourceCallback.createContext();
    if (ackMode.equals(TRANSACTED)) {
      sessionManager.bindToTransaction(session);
    }
    evaluateAckAction(message);
    saveReplyToDestination(message, context);
    context.addVariable(JmsListener.JMS_LOCK_VAR, jmsLock);
    context.addVariable(JmsListener.JMS_SESSION_VAR, session);
    dispatchMessage(message, context, resolveEncoding(message), resolveContentType(message));
    waitForMessageToBeProcessed(jmsLock);
  }

  private void evaluateAckAction(Message message) {
    try {
      evaluateMessageAck(ackMode, session, message, sessionManager, jmsLock);
    } catch (JMSException e) {
      LOGGER.error("An error occurred while processing an incoming message: ", e);
      sourceCallback.onSourceException(e);
    }
  }

  private String resolveContentType(Message message) {
    // If no explicit content type was provided to the operation, fallback to the
    // one communicated in the message properties. Finally if no property was set,
    // use the default one provided by the config
    return resolveOverride(resolveMessageContentType(message, config.getContentType()), contentType);
  }

  private String resolveEncoding(Message message) {
    // If no explicit content type was provided to the operation, fallback to the
    // one communicated in the message properties. Finally if no property was set,
    // use the default one provided by the config
    return resolveOverride(resolveMessageEncoding(message, config.getEncoding()), encoding);
  }

  private void saveReplyToDestination(Message message, SourceCallbackContext context) {
    try {
      Destination replyTo = message.getJMSReplyTo();
      if (replyTo != null) {
        context.addVariable(JmsListener.REPLY_TO_DESTINATION_VAR, replyTo);
      }
    } catch (JMSException e) {
      LOGGER.error("An error occurred while obtaining the ReplyTo destination: ", e);
      sourceCallback
          .onSourceException(new JmsExtensionException(e, "An error occurred while obtaining the ReplyTo destination: "));
    }
  }

  private void dispatchMessage(Message message, SourceCallbackContext context, String encoding, String contentType) {
    try {
      Result<Object, JmsAttributes> result =
          resultFactory.createResult(message, jmsSupport.getSpecification(), contentType, encoding, session.getAckId());
      sourceCallback.handle(result, context);

    } catch (Exception e) {
      LOGGER.error("An error occurred while creating the initial message", e);
      sourceCallback.onSourceException(e);
    }
  }

  private void waitForMessageToBeProcessed(JmsListenerLock jmsLock) {
    jmsLock.lock();
  }
}
