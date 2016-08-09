/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.exception;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.config.i18n.TransportCoreMessages;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.exception.MessageRedeliveredException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.Message;

public class EndpointMessageRedeliveredException extends MessageRedeliveredException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 9013890402770563931L;

  protected final transient ImmutableEndpoint endpoint;

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  protected EndpointMessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery,
                                                InboundEndpoint endpoint, MuleEvent event, Message message) {
    super(messageId, redeliveryCount, maxRedelivery, event, message);
    this.endpoint = endpoint;
  }

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public EndpointMessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, InboundEndpoint endpoint,
                                             MuleEvent event, Message message, MessageProcessor failingMessageProcessor) {
    super(messageId, redeliveryCount, maxRedelivery, event, message, failingMessageProcessor);
    this.endpoint = endpoint;
  }

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public EndpointMessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, InboundEndpoint endpoint,
                                             MuleEvent event, MessageProcessor failingMessageProcessor) {
    this(messageId, redeliveryCount, maxRedelivery, endpoint, event,
         TransportCoreMessages.createStaticMessage("Maximum redelivery attempts reached"), failingMessageProcessor);
  }

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public ImmutableEndpoint getEndpoint() {
    return endpoint;
  }
}
