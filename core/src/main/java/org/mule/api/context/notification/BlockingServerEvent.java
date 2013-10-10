/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.context.notification;

/**
 * <code>BlockingServerEvent</code> is a marker interface that tells the server
 * event manager to publish this event in the current thread, thus blocking the
 * current thread of execution until all listeners have been processed
 */

public interface BlockingServerEvent
{
    // no methods
}
