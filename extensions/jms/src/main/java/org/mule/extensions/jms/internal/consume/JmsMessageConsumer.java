/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.consume;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.exception.JmsTimeoutException;

import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

/**
 * Wrapper implementation of a JMS {@link MessageConsumer}
 *
 * @since 4.0
 */
public final class JmsMessageConsumer implements AutoCloseable {

  private static final Logger LOGGER = getLogger(JmsMessageConsumer.class);
  private final MessageConsumer consumer;

  public JmsMessageConsumer(MessageConsumer consumer) {
    checkArgument(consumer != null, "A non null MessageConsumer is required to use as delegate");
    this.consumer = consumer;
  }

  public void listen(MessageListener listener) throws JMSException {
    consumer.setMessageListener(listener);
  }

  public Message consume(Long maximumWaitTime) throws JMSException, JmsTimeoutException {

    if (maximumWaitTime == -1) {
      return receive();
    }

    if (maximumWaitTime == 0) {
      return receiveNoWait();
    }

    return receiveWithTimeout(maximumWaitTime);
  }

  @Override
  public void close() throws JMSException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Closing consumer " + consumer);
    }
    consumer.close();
  }

  private Message receiveWithTimeout(Long maximumWaitTime) throws JMSException, JmsTimeoutException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Waiting for a message, timeout will be in [%s] millis", maximumWaitTime));
    }

    StopWatch timeoutValidator = new StopWatch();
    timeoutValidator.start();
    Message message = consumer.receive(maximumWaitTime);
    timeoutValidator.stop();

    if (message == null && timeoutValidator.getTime() > maximumWaitTime) {
      throw new JmsTimeoutException("Failed to retrieve a Message, operation timed out");
    }
    return message;
  }

  private Message receiveNoWait() throws JMSException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Trying to consume an immediately available message");
    }

    return consumer.receiveNoWait();
  }

  private Message receive() throws JMSException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("No Timeout set, waiting for a message until one arrives");
    }

    return consumer.receive();
  }

}
