/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

import org.mule.api.MuleException;

/**
 * Builds {@link org.mule.api.processor.MessageProcessorChain} instances.
 *
 * @since 3.1
 */
public interface MessageProcessorChainBuilder extends MessageProcessorBuilder
{
    MessageProcessorChain build() throws MuleException;

    MessageProcessorChainBuilder chain(MessageProcessor... processors);

    MessageProcessorChainBuilder chain(MessageProcessorBuilder... builders);
}
