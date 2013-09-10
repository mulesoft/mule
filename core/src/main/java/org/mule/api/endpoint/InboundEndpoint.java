/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.endpoint;

import org.mule.api.construct.FlowConstructAware;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.source.MessageSource;
import org.mule.api.transport.MessageRequesting;
import org.mule.processor.AbstractRedeliveryPolicy;

public interface InboundEndpoint
    extends ImmutableEndpoint, MessageRequesting, MessageSource, FlowConstructAware, Startable, Stoppable
{
    AbstractRedeliveryPolicy createDefaultRedeliveryPolicy(int maxRedelivery);
}
