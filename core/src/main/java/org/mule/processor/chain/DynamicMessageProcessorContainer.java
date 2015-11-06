/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.util.NotificationUtils.FlowMap;

/**
 * Specific case of a {@link MessageProcessorContainer} when its inner processors are dynamic (for instance, with a dynamically referenced subflow).
 */
public interface DynamicMessageProcessorContainer extends MessageProcessorContainer, MessageProcessor
{

    /**
     * Builds the flowMap for the inner processors relative to the calling flow.
     */
    FlowMap buildInnerPaths();

}
