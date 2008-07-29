/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.interceptor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.service.Service;

/**
 * Call the embedded component.  Because interception is "lower level" in 2.x, it cannot
 * return a MuleMessage - instead an Object is returned.
 *
 * <p>If you are using the adapter in the Spring Extras package then, if you want that same object to be
 * returned as the result, you do not need to construct a new message, just return null and the
 * adapter will manage the result correctly. 
 *
 * @deprecated - This is only used for backwards compatability with old style (Mule 1.x) interceptors
 */
public interface Invocation
{

    /**
     * Calls the component
     *
     * @return the result of invoking the component
     * @throws org.mule.api.MuleException if something goes wrong
     */
    Object execute() throws MuleException;

    Service getService();

    MuleEvent getEvent();

    MuleMessage getMessage();

}
