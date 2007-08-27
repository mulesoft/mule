/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.rmi;

/**
 * A callback proxy for binding a RmiMessage receiver to a Remote object
 */

public interface RmiAble
{
    /**
     * Set Mule receiver as parameter for callback
     * 
     * @param receiver
     */
    public void setReceiver(RmiMessageReceiver receiver);

    /**
     * Implementing method should route message back to Mule receiver and receive
     * possible reply for program that calls this Receiver
     * 
     * @param message from calling program
     * @return possible reply from Mule to be routed back to calling program as
     *         method result
     */
    public Object route(Object message);
}
