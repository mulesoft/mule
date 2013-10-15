/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import org.ibeans.api.InvocationContext;
import org.ibeans.api.ParamFactory;

/**
 * Will simply echo the param name
 */
public class EchoParamFactory implements ParamFactory
{
    public String create(String paramName, boolean optional, InvocationContext invocationContext)
    {
        return paramName;
    }
}
