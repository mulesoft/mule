/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.factory.activemq;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;

/**
 * Contains the parameters required to configure an {@link ActiveMQConnectionFactory}
 *
 * @since 4.0
 */
public final class ActiveMQConnectionFactoryConfiguration {

  private static final String DEFAULT_BROKER_URL = "vm://localhost?broker.persistent=false&broker.useJmx=false";

  /**
   * The address of the broker to connect
   */
  @Parameter
  @Optional(defaultValue = DEFAULT_BROKER_URL)
  @Expression(NOT_SUPPORTED)
  private String brokerUrl;

  /**
   * {@code true} if the {@link ConnectionFactory} should support XA
   */
  @Parameter
  @Alias("enable-xa")
  @Optional(defaultValue = "false")
  @Expression(NOT_SUPPORTED)
  private boolean enableXA;

  /**
   * Used to configure the {@link RedeliveryPolicy#getInitialRedeliveryDelay()}
   */
  @Parameter
  @Optional(defaultValue = "1000")
  @Expression(NOT_SUPPORTED)
  private long initialRedeliveryDelay;

  /**
   * Used to configure the {@link RedeliveryPolicy#getRedeliveryDelay()}
   */
  @Parameter
  @Optional(defaultValue = "1000")
  @Expression(NOT_SUPPORTED)
  private long redeliveryDelay;

  /**
   * Used to configure the {@link RedeliveryPolicy#getMaximumRedeliveries()}
   * No redelivery is represented with 0, while -1 means infinite re deliveries accepted.
   */
  @Parameter
  @Optional(defaultValue = "0")
  @Expression(NOT_SUPPORTED)
  private int maxRedelivery;

  public int getMaxRedelivery() {
    return maxRedelivery;
  }

  public boolean isEnableXA() {
    return enableXA;
  }

  public String getBrokerUrl() {
    return brokerUrl;
  }

  public long getInitialRedeliveryDelay() {
    return initialRedeliveryDelay;
  }

  public long getRedeliveryDelay() {
    return redeliveryDelay;
  }
}
