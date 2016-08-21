/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.endpoint;

import org.mule.compatibility.core.api.transport.MessageRequesting;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transport.LegacyInboundEndpoint;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;

/**
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface InboundEndpoint
    extends ImmutableEndpoint, MessageRequesting, MessageSource, FlowConstructAware, Startable, Stoppable, LegacyInboundEndpoint {

  AbstractRedeliveryPolicy createDefaultRedeliveryPolicy(int maxRedelivery);

  @Override
  default boolean isCompatibleWithAsync() {
    return true;
  }

  FlowConstruct getFlowConstruct();

  @Override
  default String getCanonicalURI() {
    return getConnector().getCanonicalURI(getEndpointURI());
  }
}
