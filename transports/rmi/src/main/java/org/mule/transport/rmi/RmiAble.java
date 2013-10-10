/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.rmi;

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
