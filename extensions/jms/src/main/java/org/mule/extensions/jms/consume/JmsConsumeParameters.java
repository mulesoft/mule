/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.consume;

import static org.mule.extensions.jms.internal.common.JmsCommons.EXAMPLE_CONTENT_TYPE;
import static org.mule.extensions.jms.internal.common.JmsCommons.EXAMPLE_ENCODING;
import org.mule.extensions.jms.api.config.ConsumerAckMode;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.concurrent.TimeUnit;

import javax.jms.Message;

/**
 * Contains the parameters that can override the default values for
 * publishing a {@link Message}
 *
 * @since 4.0
 */
@XmlHints(allowTopLevelDefinition = true)
public class JmsConsumeParameters {

  @Parameter
  @Optional
  @Summary("The Session ACK mode to use when consuming the message")
  private ConsumerAckMode ackMode;

  @Parameter
  @Optional(defaultValue = "10000")
  @Summary("Maximum time to wait for a message to arrive before timeout")
  private long maximumWait;

  @Parameter
  @Optional(defaultValue = "MILLISECONDS")
  @Summary("Time unit to be used in the maximumWaitTime configuration")
  private TimeUnit maximumWaitUnit;

  @Parameter
  @Optional
  @Example(EXAMPLE_CONTENT_TYPE)
  @Summary("The content type of the message body to be consumed")
  @DisplayName("Content Type")
  private String inboundContentType;

  @Parameter
  @Optional
  @Example(EXAMPLE_ENCODING)
  @Summary("The encoding of the message body to be consumed")
  @DisplayName("Encoding")
  private String inboundEncoding;

  public ConsumerAckMode getAckMode() {
    return ackMode;
  }

  public long getMaximumWait() {
    return maximumWait;
  }

  public TimeUnit getMaximumWaitUnit() {
    return maximumWaitUnit;
  }

  public String getInboundContentType() {
    return inboundContentType;
  }

  public String getInboundEncoding() {
    return inboundEncoding;
  }
}
