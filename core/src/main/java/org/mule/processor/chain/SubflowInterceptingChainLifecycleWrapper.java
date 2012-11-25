/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.util.NotificationUtils;

import java.util.List;
import java.util.Map;

/**
 * Generates message processor identfiers specific for subflows.
 */
public class SubflowInterceptingChainLifecycleWrapper extends InterceptingChainLifecycleWrapper
{

    public SubflowInterceptingChainLifecycleWrapper(MessageProcessorChain chain, List<MessageProcessor> processors, String name)
    {
        super(chain, processors, name);
    }

    @Override
    public Map<MessageProcessor, String> getMessageProcessorPaths()
    {
        return NotificationUtils.buildMessageProcessorPaths(processors, name + "/subprocessors");
    }

}
