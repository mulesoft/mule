/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations.invoke;

import org.ibeans.api.InvocationContext;
import org.ibeans.api.ParamFactory;

/**
 * TODO
 */
public class HelloParamFactory implements ParamFactory
{
    public Object create(String paramName, boolean optional, InvocationContext invocationContext) throws Exception
    {
        return new DummyObject();
    }
}
