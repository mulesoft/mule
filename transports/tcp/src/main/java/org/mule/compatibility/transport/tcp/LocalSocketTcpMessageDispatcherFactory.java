/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.MessageDispatcher;
import org.mule.compatibility.core.transport.AbstractMessageDispatcherFactory;
import org.mule.runtime.core.api.MuleException;

/**
 * A factory for <code>LocalSocketTcpMessageDispatcher</code>
 * 
 * @since 2.2.6
 */
public class LocalSocketTcpMessageDispatcherFactory extends AbstractMessageDispatcherFactory {

  @Override
  public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException {
    return new LocalSocketTcpMessageDispatcher(endpoint);
  }

}
