/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import org.mule.util.StringUtils;

import org.ibeans.api.InvocationContext;
import org.ibeans.api.ParamFactory;

/**
 * TODO
 */
public class ReversePropertyParamFactory implements ParamFactory
{
    private String propertyName;

    public ReversePropertyParamFactory(String propertyName)
    {
        this.propertyName = propertyName;
    }

    public String create(String paramName, boolean optional, InvocationContext invocationContext)
    {
        String prop = (String) invocationContext.getIBeanConfig().getPropertyParams().get(propertyName);
        if (prop == null && !optional)
        {
            throw new IllegalArgumentException("PropertyParam value was null for: " + propertyName);
        }
        return StringUtils.reverse(prop);
    }
}
