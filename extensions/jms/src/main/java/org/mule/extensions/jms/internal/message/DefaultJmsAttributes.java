/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.message;


import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.api.message.JmsHeaders;
import org.mule.extensions.jms.api.message.JmsMessageProperties;

/**
 * Default implementation of {@link JmsAttributes}
 *
 * @since 4.0
 */
class DefaultJmsAttributes implements JmsAttributes {

  private final JmsMessageProperties properties;
  private final JmsHeaders headers;
  private final String ackId;

  public DefaultJmsAttributes(JmsMessageProperties properties, JmsHeaders headers, String ackId) {
    this.properties = properties;
    this.headers = headers;
    this.ackId = ackId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JmsMessageProperties getProperties() {
    return properties;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JmsHeaders getHeaders() {
    return headers;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAckId() {
    return ackId;
  }

  /**
   * Builder implementation for creating a {@link JmsAttributes} instance
   * @since 4.0
   */
  public static class Builder {

    private JmsMessageProperties properties;
    private JmsHeaders headers;
    private String ackId;

    private Builder() {}

    public static Builder newInstance() {
      return new Builder();
    }

    public Builder withProperties(JmsMessageProperties properties) {
      this.properties = properties;
      return this;
    }

    public Builder withHeaders(JmsHeaders headers) {
      this.headers = headers;
      return this;
    }

    public Builder withAckId(String ackId) {
      this.ackId = ackId;
      return this;
    }

    public JmsAttributes build() {
      checkArgument(properties != null, "No JmsMessageProperties were provided, but they are required for the JmsAttributes");
      checkArgument(headers != null, "No JmsHeaders were provided, but they are required for the JmsAttributes");
      return new DefaultJmsAttributes(properties, headers, ackId);
    }
  }
}
