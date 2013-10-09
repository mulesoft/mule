/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.annotations;

import org.ibeans.api.InvocationContext;
import org.ibeans.api.ParamFactory;

/**
 * Relies on ordering with the {@link FirstParamFactory} being created first
 */
public class SecondParamFactory implements ParamFactory
{
    public String create(String paramName, boolean optional, InvocationContext invocationContext)
    {
        //Just return what the {@link FirstParamFactory } created
        return invocationContext.getIBeanConfig().getUriParams().get("param1") + " " + invocationContext.getIBeanConfig().getUriParams().get("foo");
    }
}
