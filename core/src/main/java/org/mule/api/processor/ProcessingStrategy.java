/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
                                    org.mule.api.processor.StageNameSource nameSource,
                                    MessageProcessorChainBuilder chainBuilder,
                                    MuleContext muleContext);

    /**
     * To be removed in Mule 4. Use {@link org.mule.api.processor.StageNameSource} instead
     */
    @Deprecated
    public interface StageNameSource extends org.mule.api.processor.StageNameSource
    {

    }

}
