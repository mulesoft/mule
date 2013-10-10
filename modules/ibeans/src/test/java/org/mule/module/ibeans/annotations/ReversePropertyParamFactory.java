/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
