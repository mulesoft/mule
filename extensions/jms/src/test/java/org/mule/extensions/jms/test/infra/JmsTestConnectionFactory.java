/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.infra;

import static java.lang.String.format;
import static org.mockito.Mockito.mock;

import javax.jms.Connection;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

import org.mockito.Answers;

public class JmsTestConnectionFactory implements QueueConnectionFactory, TopicConnectionFactory {

  private String providerProperty = "NOT_SET";
  private String connectionFactoryProperty = "NOT_SET";
  private Object customProperty;

  @Override
  public Connection createConnection() throws JMSException {
    return mock(Connection.class, Answers.RETURNS_DEEP_STUBS.get());
  }

  @Override
  public Connection createConnection(String username, String password) throws JMSException {
    return createConnection();
  }

  @Override
  public QueueConnection createQueueConnection() throws JMSException {
    return mock(QueueConnection.class, Answers.RETURNS_DEEP_STUBS.get());
  }

  @Override
  public QueueConnection createQueueConnection(String string, String string1) throws JMSException {
    return createQueueConnection();
  }

  @Override
  public TopicConnection createTopicConnection() throws JMSException {
    return mock(TopicConnection.class, Answers.RETURNS_DEEP_STUBS.get());
  }

  @Override
  public TopicConnection createTopicConnection(String userName, String password) throws JMSException {
    return createTopicConnection();
  }

  @Override
  public JMSContext createContext() {
    throw new RuntimeException("We should always use the Classic API, but [createContext()] was invoked");
  }

  @Override
  public JMSContext createContext(String userName, String password) {
    throw new RuntimeException(format("We should always use the Classic API,"
        + " but [createContext(%s, %s)] was invoked", userName, password));
  }

  @Override
  public JMSContext createContext(String userName, String password, int sessionMode) {
    throw new RuntimeException(format("We should always use the Classic API,"
        + " but [createContext(%s, %s, %s)] was invoked", userName, password, sessionMode));
  }

  @Override
  public JMSContext createContext(int sessionMode) {
    throw new RuntimeException(format("We should always use the Classic API,"
        + " but [createContext(%s)] was invoked", sessionMode));
  }

  public String getProviderProperty() {
    return providerProperty;
  }

  /**
   * Should NOT be called.
   */
  public void setProviderProperty(final String providerProperty) {
    throw new RuntimeException("Should never be called.");
  }

  public String getConnectionFactoryProperty() {
    return connectionFactoryProperty;
  }

  /**
   * MUST be called
   */
  public void setConnectionFactoryProperty(final String connectionFactoryProperty) {
    this.connectionFactoryProperty = connectionFactoryProperty;
  }

  public Object getCustomProperty() {
    return customProperty;
  }

  public void setCustomProperty(Object custom) {
    customProperty = custom;
  }

}
