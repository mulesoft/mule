/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.processor.chain.AbstractMessageProcessorChainBuilder;
import org.mule.processor.chain.SubflowMessageProcessorChainBuilder;

/**
 * Uses a custom message processor chain builder for subflows
 * in order to genearte the proper message processor ids.
 */
public class SubflowMessageProcessorChainFactoryBean extends MessageProcessorChainFactoryBean
{

    @Override
    protected MessageProcessorChainBuilder getBuilderInstance()
    {
        AbstractMessageProcessorChainBuilder builder = new SubflowMessageProcessorChainBuilder();
        builder.setName(name);
        return builder;
    }
}
