/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.connection.provider.activemq;

import static java.lang.String.format;
import static org.mule.runtime.core.util.ClassUtils.instanciateClass;
import org.mule.extensions.jms.api.connection.factory.activemq.ActiveMQConnectionFactoryConfiguration;
import org.mule.extensions.jms.internal.connection.exception.ActiveMQException;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ActiveMQConnectionFactory} provider.
 * If no custom {@link ConnectionFactory} is given, then this provider knows how to build
 * a default {@link ActiveMQConnectionFactory} and how to configure it using the given
 * {@link ActiveMQConnectionFactoryConfiguration}
 *
 * @since 4.0
 */
@ExclusiveOptionals
public class ActiveMQConnectionFactoryProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQConnectionFactoryProvider.class);

  private static final String ACTIVEMQ_CONNECTION_FACTORY_CLASS = "org.apache.activemq.ActiveMQConnectionFactory";
  private static final String ACTIVEMQ_XA_CONNECTION_FACTORY_CLASS = "org.apache.activemq.ActiveMQXAConnectionFactory";
  private static final int REDELIVERY_IGNORE = -1;

  /**
   * a custom {@link ConnectionFactory} that relates to an {@link ActiveMQConnectionFactory}
   */
  @Parameter
  @Optional
  private ConnectionFactory connectionFactory;

  /**
   * configuration parameters required to configure a default {@link ActiveMQConnectionFactory}
   */
  @Parameter
  @Optional
  @NullSafe
  private ActiveMQConnectionFactoryConfiguration factoryConfiguration;


  public ConnectionFactory getConnectionFactory() {
    return connectionFactory;
  }

  ConnectionFactory createDefaultConnectionFactory() throws ActiveMQException {

    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Creating new [%s]", getFactoryClass()));
      }

      this.connectionFactory = (ConnectionFactory) instanciateClass(getFactoryClass(),
                                                                    factoryConfiguration.getBrokerUrl());
      applyVendorSpecificConnectionFactoryProperties(connectionFactory);
      return connectionFactory;
    } catch (Exception e) {
      String message = format("Failed to create a default Connection Factory for ActiveMQ using the [%s] implementation: ",
                              getFactoryClass());
      LOGGER.error(message, e);
      throw new ActiveMQException(message, e);
    }
  }

  private void applyVendorSpecificConnectionFactoryProperties(ConnectionFactory connectionFactory) {
    try {
      Method getRedeliveryPolicyMethod = connectionFactory.getClass().getMethod("getRedeliveryPolicy");
      Object redeliveryPolicy = getRedeliveryPolicyMethod.invoke(connectionFactory);
      setMaximumRedeliveries(redeliveryPolicy);
      setInitialRedeliveryDelay(redeliveryPolicy);
      setRedeliveryDelay(redeliveryPolicy);

    } catch (Exception e) {
      LOGGER.error("Failed to set custom ConnectionFactoryProperties for ActiveMQ RedeliveryPolicy ", e);
    }
  }

  private void setMaximumRedeliveries(Object redeliveryPolicy)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method setMaximumRedeliveriesMethod = redeliveryPolicy.getClass().getMethod("setMaximumRedeliveries", Integer.TYPE);
    int maxRedelivery = factoryConfiguration.getMaxRedelivery();
    if (maxRedelivery != REDELIVERY_IGNORE) {
      // redelivery = deliveryCount - 1, but AMQ is considering the first delivery attempt
      // as a redelivery (wrong!). adjust for it
      maxRedelivery++;
    }
    setMaximumRedeliveriesMethod.invoke(redeliveryPolicy, maxRedelivery);
  }

  private void setInitialRedeliveryDelay(Object redeliveryPolicy)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method setInitialRedeliveryDelayMethod = redeliveryPolicy.getClass().getMethod("setInitialRedeliveryDelay", Long.TYPE);
    setInitialRedeliveryDelayMethod.invoke(redeliveryPolicy, factoryConfiguration.getInitialRedeliveryDelay());
  }

  private void setRedeliveryDelay(Object redeliveryPolicy)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method setRedeliveryDelayMethod = redeliveryPolicy.getClass().getMethod("setRedeliveryDelay", Long.TYPE);
    setRedeliveryDelayMethod.invoke(redeliveryPolicy, factoryConfiguration.getRedeliveryDelay());
  }

  private String getFactoryClass() {
    return factoryConfiguration.isEnableXA()
        ? ACTIVEMQ_XA_CONNECTION_FACTORY_CLASS : ACTIVEMQ_CONNECTION_FACTORY_CLASS;
  }

}
