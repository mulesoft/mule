/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
