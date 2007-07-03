/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap;

import org.mule.config.MuleProperties;
import org.mule.interceptors.EnvelopeInterceptor;
import org.mule.umo.Invocation;
import org.mule.umo.UMOException;

/**
 * This Interceptor decorates the current message with a property that tells the
 * DynamicEntryPointResolver to ignore the 'method' property. The reason being is
 * that for the SOAP endpoints have an additional hop - http --->
 * AxisServiceComponent ---> WebService the 'method' param is targetted for the
 * WebService not the AxisServiceComponent, so we need to ignore it
 */
public class MethodFixInterceptor extends EnvelopeInterceptor
{

    public void before(Invocation invocation) throws UMOException
    {
        invocation.getMessage().setProperty(MuleProperties.MULE_IGNORE_METHOD_PROPERTY, new Boolean(true));
    }

    public void after(Invocation invocation) throws UMOException
    {
        if (invocation.getMessage() != null)
        {
            invocation.getMessage().removeProperty(MuleProperties.MULE_IGNORE_METHOD_PROPERTY);
        }
    }
}
