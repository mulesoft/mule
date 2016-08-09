/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import org.mule.runtime.core.api.MuleContext;

import java.util.List;

/**
 * Determines how a list of message processors should processed.
 */
public interface ProcessingStrategy {

  void configureProcessors(List<MessageProcessor> processors, org.mule.runtime.core.api.processor.StageNameSource nameSource,
                           MessageProcessorChainBuilder chainBuilder, MuleContext muleContext);
}
