/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
