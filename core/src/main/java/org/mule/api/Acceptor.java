/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api;

/**
 *  Provides capability to only accept handling certain MuleEvents.
 */
public interface Acceptor
{
    /**
     * @param event {@link MuleEvent} to route through exception handler
     * @return true if this {@link org.mule.api.exception.MessagingExceptionHandler} should handler exception
     *         false otherwise
     */
    boolean accept(MuleEvent event);

    /**
     * @return true if accepts any message, false otherwise.
     */
    boolean acceptsAll();
}
