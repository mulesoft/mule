/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.connection;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.extensions.jms.api.connection.caching.CachingConfiguration;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Stoppable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.TopicConnection;

import org.springframework.jms.connection.CachingConnectionFactory;


/**
 * Decorates the JMS {@link javax.jms.ConnectionFactory} in order to ensure JMS session instances are reused.
 * Applies only if the supplied connection factory isn't already an instance of {@link CachingConnectionFactory}
 * NOTE: Currently only Non-XA JMS {@link javax.jms.ConnectionFactory}'s will be decorated to provide caching.
 *
 * @since 4.0
 */
public class JmsCachingConnectionFactory extends CachingConnectionFactory implements Stoppable, Disposable {

  private final String username;
  private final String password;
  private final JmsSupport jmsSupport;
  private final String clientId;

  public JmsCachingConnectionFactory(ConnectionFactory targetConnectionFactory, String username, String password, String clientId,
                                     CachingConfiguration config, JmsSupport jmsSupport, ExceptionListener exceptionListener) {
    super(targetConnectionFactory);
    checkArgument(!(targetConnectionFactory instanceof CachingConnectionFactory),
                  "The ConnectionFactory provided shouldn't be wrapped in a JmsCachingConnectionFactory");

    super.setCacheConsumers(config.isConsumersCache());
    super.setCacheProducers(config.isProducersCache());
    super.setSessionCacheSize(config.getSessionCacheSize());
    super.setReconnectOnException(false);
    super.setExceptionListener(exceptionListener);

    this.username = username;
    this.password = password;
    this.clientId = clientId;
    this.jmsSupport = jmsSupport;
  }

  @Override
  protected Connection doCreateConnection() throws JMSException {
    Connection connection;
    if (isBlank(username)) {
      connection = jmsSupport.createConnection(getTargetConnectionFactory());
    } else {
      connection = jmsSupport.createConnection(getTargetConnectionFactory(), username, password);
    }

    if (!isBlank(clientId)) {
      connection.setClientID(clientId);
    }

    return connection;
  }

  @Override
  public void stop() {
    resetConnection();
  }

  @Override
  public void dispose() {
    destroy();
  }

  @Override
  public Connection createConnection(String username, String password) throws JMSException {
    throw new javax.jms.IllegalStateException(
                                              "JmsCachingConnectionFactory does not support creating a connection with username and password. Provide the desired username and password when the instance is defined");
  }

  @Override
  public QueueConnection createQueueConnection(String username, String password) throws JMSException {
    throw new javax.jms.IllegalStateException(
                                              "JmsCachingConnectionFactory does not support creating a connection with username and password. Provide the desired username and password when the instance is defined");
  }

  @Override
  public TopicConnection createTopicConnection(String username, String password) throws JMSException {
    throw new javax.jms.IllegalStateException(
                                              "JmsCachingConnectionFactory does not support creating a connection with username and password. Provide the desired username and password when the instance is defined");
  }

  @Override
  public JMSContext createContext() {
    // We'll use the classic API
    return null;
  }

  @Override
  public JMSContext createContext(String userName, String password) {
    // We'll use the classic API
    return null;
  }

  @Override
  public JMSContext createContext(String userName, String password, int sessionMode) {
    // We'll use the classic API
    return null;
  }

  @Override
  public JMSContext createContext(int sessionMode) {
    // We'll use the classic API
    return null;
  }
}
