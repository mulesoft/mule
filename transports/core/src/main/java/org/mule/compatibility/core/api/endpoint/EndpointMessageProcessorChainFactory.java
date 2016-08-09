/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.endpoint;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.MessageProcessor;

/**
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface EndpointMessageProcessorChainFactory {

  public MessageProcessor createInboundMessageProcessorChain(InboundEndpoint endpoint, FlowConstruct flowConstruct,
                                                             MessageProcessor target)
      throws MuleException;

  public MessageProcessor createOutboundMessageProcessorChain(OutboundEndpoint endpoint, MessageProcessor target)
      throws MuleException;
}


