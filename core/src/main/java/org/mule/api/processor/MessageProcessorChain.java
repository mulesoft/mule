/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

import org.mule.api.processor.policy.Policies;

import java.util.List;

/**
 *
 */
public interface MessageProcessorChain extends MessageProcessor
{
    List<MessageProcessor> getMessageProcessors();

    String getName();

    Policies getPolicies();

}
