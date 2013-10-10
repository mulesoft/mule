/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.processor;

import org.mule.api.MuleContext;

import java.util.List;

/**
 * Determines how a list of message processors should processed.
 */
public interface ProcessingStrategy
{

    public void configureProcessors(List<MessageProcessor> processors,
                                    StageNameSource nameSource,
                                    MessageProcessorChainBuilder chainBuilder,
                                    MuleContext muleContext);

    public interface StageNameSource
    {
        String getName();
    }

}
