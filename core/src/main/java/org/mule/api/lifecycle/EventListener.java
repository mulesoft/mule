/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;

/**
 * <code>EventListener</code> is a marker interface that is implemented by
 * objects wishing to receive Mule events in managed environments, such as an EJB
 * container. There are not methods on this interface as typically Mule will work out
 * the method to invoke on the listener. Developers can implement
 * <code>org.mule.api.lifecycle.Callable</code> to implement a specific Mule
 * listener interface.
 * 
 * @see Callable
 */
public interface EventListener
{
    // no methods
}
