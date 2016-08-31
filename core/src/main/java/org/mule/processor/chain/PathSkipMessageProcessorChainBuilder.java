/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.processor.MessageProcessor;

/**
 * Constructs a chain of {@link MessageProcessor}s and wraps the invocation of the chain in a composite
 * MessageProcessor. This Builder wrapps the chain in an {@link InterceptingChainLifecycleWrapperPathSkip},
 * so it does not add a level into the path elements.
 */
public class PathSkipMessageProcessorChainBuilder extends DefaultMessageProcessorChainBuilder
{
    @Override
    protected MessageProcessorChain buildMessageProcessorChain(DefaultMessageProcessorChain chain)
    {
        return new InterceptingChainLifecycleWrapperPathSkip(chain, processors, name);
    }
}
