/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.redelivery;

import static org.mule.compatibility.core.DefaultMuleEventEndpointUtils.createEventUsingInboundEndpoint;
import static org.mule.compatibility.transport.jms.i18n.JmsMessages.tooManyRedeliveries;
import static org.mule.runtime.core.DefaultEventContext.create;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;

public class MessageRedeliveredException extends org.mule.compatibility.core.api.exception.EndpointMessageRedeliveredException {

  public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, InboundEndpoint endpoint,
                                     FlowConstruct flow, InternalMessage muleMessage) {
    super(messageId, redeliveryCount, maxRedelivery, endpoint, buildEvent(endpoint, flow, muleMessage),
          tooManyRedeliveries(messageId, redeliveryCount, maxRedelivery, endpoint));
  }

  protected static Event buildEvent(InboundEndpoint endpoint, FlowConstruct flow, InternalMessage muleMessage) {
    final Event.Builder eventBuilder = Event.builder(create(flow, "MessageRedeliveredException")).flow(flow);
    return createEventUsingInboundEndpoint(eventBuilder, muleMessage, endpoint);
  }

}
