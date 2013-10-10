/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.processor;

import org.mule.api.MuleException;

/**
 * A {@link MessageProcessor} that routes messages to zero or more destination
 * message processors. Implementations determine exactly how this is done by making
 * decisions about which route(s) should be used and if the message should be copied
 * or not.
 */
public interface MessageRouter extends MessageProcessor
{
    /**
     * Adds a new message processor to the list of routes
     * 
     * @param processor new destination message processor
     * @throws MuleException 
     */
    void addRoute(MessageProcessor processor) throws MuleException;

    /**
     * Removes a message processor from the list of routes
     * 
     * @param processor destination message processor to remove
     */
    void removeRoute(MessageProcessor processor) throws MuleException;

}
