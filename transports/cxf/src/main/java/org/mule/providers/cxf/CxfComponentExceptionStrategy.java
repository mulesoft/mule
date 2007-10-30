/*
 * $Id: XFireComponentExceptionStrategy.java 6306 2007-05-04 03:02:55Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.apache.cxf.interceptor.Fault;
import org.mule.impl.DefaultComponentExceptionStrategy;

/**
 * This exception strategy forces the exception thrown from a web service invocation
 * to be passed as-is, not wrapped in a Mule exception object. This ensures the XFire
 * serialiser/deserialiser can send the correct exception object to the client.
 */
public class CxfComponentExceptionStrategy extends DefaultComponentExceptionStrategy
{
    protected void defaultHandler(Throwable t)
    {
        if (t.getCause() instanceof Fault)
        {
            super.defaultHandler(t.getCause());
        }
        else
        {
            super.defaultHandler(t);
        }
    }
}
