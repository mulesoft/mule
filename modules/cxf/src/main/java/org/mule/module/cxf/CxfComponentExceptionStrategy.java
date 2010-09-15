/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import org.mule.api.MuleContext;
import org.mule.exception.DefaultServiceExceptionStrategy;

import org.apache.cxf.interceptor.Fault;

/**
 * This exception strategy forces the exception thrown from a web service invocation
 * to be passed as-is, not wrapped in a Mule exception object. This ensures the Cxf
 * serialiser/deserialiser can send the correct exception object to the client.
 */
public class CxfComponentExceptionStrategy extends DefaultServiceExceptionStrategy
{
    public CxfComponentExceptionStrategy(MuleContext context)
    {
        super(context);
    }
    
    @Override
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
