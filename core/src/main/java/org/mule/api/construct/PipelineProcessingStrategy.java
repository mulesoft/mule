/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.construct;

import org.mule.api.processor.MessageProcessorChainBuilder;

/**
 * Determines how a {@link Pipeline} should process messages using the configured message processors.
 */
public interface PipelineProcessingStrategy
{

    public void configureProcessors(Pipeline pipeline, MessageProcessorChainBuilder chainBuilder);

}
