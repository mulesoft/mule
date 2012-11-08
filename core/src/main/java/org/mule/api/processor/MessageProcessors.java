/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.processor;

import org.mule.processor.chain.DefaultMessageProcessorChain;

/**
 * Some convenience methods for message processors.
 */
public class MessageProcessors
{
    private MessageProcessors()
    {
        // do not instantiate
    }

    public static MessageProcessorChain singletonChain(MessageProcessor mp)
    {
        return DefaultMessageProcessorChain.from(mp);
    }
}
