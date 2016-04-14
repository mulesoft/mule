/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

/**
 * Processes {@link MuleEvent}'s. Implementations that do not mutate the
 * {@link MuleEvent} or pass it on to another MessageProcessor should return the
 * MuleEvent they receive.
 * 
 * @since 3.0
 */
public interface MessageProcessor
{
    /**
     * Invokes the MessageProcessor.
     * 
     * @param event MuleEvent to be processed
     * @return optional response MuleEvent
     * @throws MuleException
     */
    MuleEvent process(MuleEvent event) throws MuleException;
}
