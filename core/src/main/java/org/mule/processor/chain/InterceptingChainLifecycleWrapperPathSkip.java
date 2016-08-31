/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;

import java.util.List;

/**
 * When adding path elements to the {@link MessageProcessor}s, the chains add a level to this path. For some
 * MessageProcessors this is the desired case, like {@link org.mule.routing.ChoiceRouter}, but for others this
 * is undesired behaviour. Use only if it is necessary to avoid adding a level into the path elements.
 * TODO MULE-10418
 */
public class InterceptingChainLifecycleWrapperPathSkip extends InterceptingChainLifecycleWrapper
{

    public InterceptingChainLifecycleWrapperPathSkip(MessageProcessorChain chain,
                                                     List<MessageProcessor> processors,
                                                     String name)
    {
        super(chain, processors, name);
    }

}
