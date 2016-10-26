/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.endpoint;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;

/**
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface EndpointMessageProcessorChainFactory {

  public Processor createInboundMessageProcessorChain(InboundEndpoint endpoint, FlowConstruct flowConstruct,
                                                      Processor target)
      throws MuleException;

  public Processor createOutboundMessageProcessorChain(OutboundEndpoint endpoint, Processor target)
      throws MuleException;
}


