/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

/**
 * A {@link NonBlockingVoidMuleEvent} represents a void return, but in contrast to {@link org.mule.VoidMuleEvent}
 * while the return up the stack is void, a asynchronous response is expected.  Because Mule support both blocking and
 * non-blocking the it is the return of this event instance that signals that a callback is to be expected.
 */
public class NonBlockingVoidMuleEvent extends VoidMuleEvent
{

    private static final NonBlockingVoidMuleEvent instance = new NonBlockingVoidMuleEvent();

    public static NonBlockingVoidMuleEvent getInstance()
    {
        return instance;
    }

    protected NonBlockingVoidMuleEvent()
    {
        super();
    }

}
