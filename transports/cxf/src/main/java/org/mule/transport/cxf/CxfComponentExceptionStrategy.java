/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.service.DefaultServiceExceptionStrategy;

import org.apache.cxf.interceptor.Fault;

/**
 * This exception strategy forces the exception thrown from a web service invocation
 * to be passed as-is, not wrapped in a Mule exception object. This ensures the Cxf
 * serialiser/deserialiser can send the correct exception object to the client.
 */
public class CxfComponentExceptionStrategy extends DefaultServiceExceptionStrategy
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
