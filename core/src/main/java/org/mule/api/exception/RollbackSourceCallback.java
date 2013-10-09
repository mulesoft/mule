/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.exception;

/**
 * This is used to provide a rollback method in order to achieve atomic message delivery without relying on JTA transactions, 
 * The exact behavior of this method will depend on the transport, e.g. it may send a negative ack, reset a semaphore, 
 * put the resource back in its original state/location, etc.
 */
public interface RollbackSourceCallback
{
    public void rollback();
}


