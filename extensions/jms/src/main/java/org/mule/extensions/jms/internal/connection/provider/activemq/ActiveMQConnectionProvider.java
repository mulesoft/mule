/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.connection.provider.activemq;

import static org.mule.extensions.jms.api.connection.JmsSpecification.JMS_2_0;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.connection.JmsConnection;
import org.mule.extensions.jms.internal.connection.exception.ActiveMQConnectionException;
import org.mule.extensions.jms.internal.connection.provider.BaseConnectionProvider;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.util.proxy.TargetInvocationHandler;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;

/**
 * A {@link ConnectionProvider} that contains custom logic to handle ActiveMQ connections in particular
 *
 * @since 4.0
 */
@Alias("active-mq")
public class ActiveMQConnectionProvider extends BaseConnectionProvider {

  private static final Logger LOGGER = getLogger(ActiveMQConnectionProvider.class);

  /**
   * a provider for an {@link ActiveMQConnectionFactory}
   */
  @ParameterGroup("Connection Factory")
  private ActiveMQConnectionFactoryProvider connectionFactoryProvider;

  private ConnectionFactory connectionFactory;

  @Override
  public ConnectionFactory getConnectionFactory() throws ActiveMQConnectionException {
    if (connectionFactory != null) {
      return connectionFactory;
    }

    createConnectionFactory();
    return connectionFactory;
  }

  private void createConnectionFactory() throws ActiveMQConnectionException {
    connectionFactory = connectionFactoryProvider.getConnectionFactory();
    if (connectionFactory == null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("No custom connection factory provided, creating the default for ActiveMq");
      }
      if (JMS_2_0.equals(getConnectionParameters().getSpecification())) {

        //TODO we could support a JMS 2.0 default using ActiveMQ Artemis (HornetQ) instead of ActiveMQ 5.x
        throw new IllegalArgumentException(
                                           "No ConnectionFactory was provided, but JMS 2.0 specification was selected."
                                               + " Default ActiveMQConnectionFactory implementation provides support only for JMS 1.1 and 1.0.2b versions");
      }

      connectionFactory = connectionFactoryProvider.createDefaultConnectionFactory();
    }
  }

  @Override
  protected void doClose(JmsConnection jmsConnection) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Performing custom doClose for ActiveMq");
    }

    Connection connection = jmsConnection.get();

    try {
      executeCleanup(connection);
    } catch (Exception e) {
      LOGGER.warn("Exception cleaning up ActiveMQ JMS connection: ", e);
    } finally {
      super.doClose(jmsConnection);
    }
  }

  private void executeCleanup(Connection connection) throws Exception {
    Method cleanupMethod = null;

    try {
      final Class clazz = connection.getClass();
      if (Proxy.isProxyClass(clazz)) {
        InvocationHandler invocationHandler = Proxy.getInvocationHandler(connection);

        // When using caching-connection-factory, the connections are proxy objects that do nothing on the
        // close and stop methods so that they remain open when returning to the cache. In that case, we don't
        // need to do any custom cleanup, as the connections will be closed when destroying the cache. The
        // type of the invocation handler for these connections is SharedConnectionInvocationHandler.

        if (invocationHandler instanceof TargetInvocationHandler) {
          // this is really an XA connection, bypass the java.lang.reflect.Proxy as it
          // can't delegate to non-interfaced methods (like proprietary 'cleanup' one)
          TargetInvocationHandler targetInvocationHandler = (TargetInvocationHandler) invocationHandler;
          connection = (Connection) targetInvocationHandler.getTargetObject();
          Class realConnectionClass = connection.getClass();
          cleanupMethod = realConnectionClass.getMethod("cleanup", (Class[]) null);
        } else {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("InvocationHandler of the JMS connection proxy is of type %s, not doing " +
                "any extra cleanup", invocationHandler.getClass().getName()));
          }
        }
      } else {
        cleanupMethod = clazz.getMethod("cleanup", (Class[]) null);
      }
    } catch (NoSuchMethodException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Failed to perform a deep cleanup on ActiveMQ connection: ", e);
      }
    }

    if (cleanupMethod != null) {
      cleanupMethod.invoke(connection, (Object[]) null);
    }
  }

  public ActiveMQConnectionFactoryProvider getConnectionFactoryProvider() {
    return connectionFactoryProvider;
  }
}
