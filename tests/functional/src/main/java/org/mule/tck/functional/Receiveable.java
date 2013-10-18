/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
