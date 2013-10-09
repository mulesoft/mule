/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.functional;

/**
 * Interface only to be used by the {@link org.mule.tck.functional.FunctionalTestComponent}.
 */
public interface Receiveable
{
    /**
     * This method is used by some WebServices tests where you don' want to be introducing the {@link org.mule.api.MuleEventContext} as
     * a complex type.
     *
     * @param data the event data received
     * @return the processed message
     * @throws Exception
     */
    public Object onReceive(Object data) throws Exception;
}
