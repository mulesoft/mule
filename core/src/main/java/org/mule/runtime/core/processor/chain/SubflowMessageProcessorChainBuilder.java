/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import org.mule.api.processor.MessageProcessorChain;

/**
 * Constructs a custom chain for subflows using the subflow name as the chain name.
 */
public class SubflowMessageProcessorChainBuilder extends DefaultMessageProcessorChainBuilder
{

    @Override
    protected MessageProcessorChain buildMessageProcessorChain(DefaultMessageProcessorChain chain)
    {
        return new SubflowInterceptingChainLifecycleWrapper(chain, processors, name);
    }
}
