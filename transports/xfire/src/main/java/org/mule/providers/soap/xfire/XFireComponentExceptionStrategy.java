/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.impl.DefaultComponentExceptionStrategy;

import org.codehaus.xfire.fault.FaultInfoException;

/**
 * This exception strategy forces the exception thrown from a web service invocation to be passed as-is, not wrapped in
 * a Mule exception object.  This ensures the XFire serialiser/deserialiser can send the correct exception object
 * to the client.
 */
public class XFireComponentExceptionStrategy extends DefaultComponentExceptionStrategy
{
    protected void defaultHandler(Throwable t)
    {
        if (t.getCause() instanceof FaultInfoException)
        {
            super.defaultHandler(t.getCause());
        }
        else
        {
            super.defaultHandler(t);
        }
    }
}
