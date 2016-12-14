/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.config;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extensions.jms.JmsExtension;
import org.mule.extensions.jms.api.operation.JmsAck;
import org.mule.extensions.jms.api.operation.JmsConsume;
import org.mule.extensions.jms.api.operation.JmsPublish;
import org.mule.extensions.jms.api.operation.JmsPublishConsume;
import org.mule.extensions.jms.api.source.JmsSubscribe;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;

/**
 * Base configuration for {@link JmsExtension}
 *
 * @since 4.0
 */
@Configuration(name = "config")
@Operations({JmsConsume.class, JmsPublish.class, JmsPublishConsume.class, JmsAck.class})
@Sources({JmsSubscribe.class})
public class JmsConfig implements Initialisable {

  @Inject
  private MuleContext muleContext;

  //TODO MULE-10904: remove this logic
  @Override
  public void initialise() throws InitialisationException {
    if (encoding == null) {
      encoding = muleContext.getConfiguration().getDefaultEncoding();
    }
  }

  /**
   * the encoding of the {@link Message} {@code body}
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private String encoding;

  /**
   * the content type of the {@link Message} {@code body}
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional(defaultValue = "*/*")
  private String contentType;

  /**
   * Configuration parameters for consuming {@link Message}s from a JMS {@link Queue} or {@link Topic}
   */
  @Parameter
  @Optional
  @NullSafe
  @Expression(NOT_SUPPORTED)
  @Placement(tab = "Consumer")
  private JmsConsumerConfig consumerConfig;

  /**
   * Configuration parameters for sending {@link Message}s to a JMS {@link Queue} or {@link Topic}
   */
  @Parameter
  @Optional
  @NullSafe
  @Expression(NOT_SUPPORTED)
  @Placement(tab = "Producer")
  private JmsProducerConfig producerConfig;


  public String getContentType() {
    return contentType;
  }

  public String getEncoding() {
    return encoding;
  }

  public JmsConsumerConfig getConsumerConfig() {
    return consumerConfig;
  }

  public JmsProducerConfig getProducerConfig() {
    return producerConfig;
  }

}
