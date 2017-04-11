/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.source;

import org.mule.extensions.jms.JmsSessionManager;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.config.JmsConfig;
import org.mule.extensions.jms.api.connection.JmsSession;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import javax.jms.Message;

/**
 * Factory pattern implementation in charge of creating instances of {@link JmsMessageListener}
 *
 * @since 4.0
 */
final class JmsMessageListenerFactory {

  private final AckMode ackMode;
  private final String encoding;
  private final String contentType;
  private JmsConfig config;
  private final JmsSessionManager sessionManager;
  private JmsSupport jmsSupport;
  private SourceCallback<Object, JmsAttributes> sourceCallback;

  /**
   * Creates a new factory with the common information that is shared between {@link JmsMessageListener} of the same
   * {@link JmsListener}
   *
   * @param ackMode        Acknowledgement mode to use to consume the messages
   * @param encoding       Default encoding if the consumed message doesn't provide one
   * @param contentType    Default contentType if the consumed message doesn't provide one
   * @param config         JMS extension configuration
   * @param sessionManager manager to store the session and ACK ID of each dispatched message
   * @param jmsSupport     JMS Support that communicates the used specification
   * @param sourceCallback callback use to dispatch the {@link Message} to the mule flow
   */
  JmsMessageListenerFactory(AckMode ackMode, String encoding, String contentType, JmsConfig config,
                            JmsSessionManager sessionManager, JmsSupport jmsSupport,
                            SourceCallback<Object, JmsAttributes> sourceCallback) {
    this.ackMode = ackMode;
    this.encoding = encoding;
    this.contentType = contentType;
    this.config = config;
    this.sessionManager = sessionManager;
    this.jmsSupport = jmsSupport;
    this.sourceCallback = sourceCallback;
  }

  /**
   * Creates a new {@link JmsMessageListener} with a given {@link JmsSession} and {@link JmsListenerLock} to be able
   * to synchronize the message dispatch.
   *
   * @param session the session to create the JMS Consumer
   * @param jmsLock the lock to use to synchronize the message dispatch
   * @return An instance of a {@link JmsMessageListener} ready to listen for messages and dispatch them to a Mule Flow
   */
  JmsMessageListener createMessageListener(JmsSession session, JmsListenerLock jmsLock) {
    return new JmsMessageListener(session, config, jmsLock, sessionManager, sourceCallback, jmsSupport, ackMode, encoding,
                                  contentType);
  }
}
