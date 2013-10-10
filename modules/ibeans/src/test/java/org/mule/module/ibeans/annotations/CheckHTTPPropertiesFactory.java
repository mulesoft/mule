/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.annotations;

import org.ibeans.api.InvocationContext;
import org.ibeans.api.ParamFactory;
import org.ibeans.api.channel.HTTP;

public class CheckHTTPPropertiesFactory implements ParamFactory
{
    public String create(String paramName, boolean optional, InvocationContext invocationContext)
    {
        String method = (String) invocationContext.getIBeanConfig().getPropertyParams().get(HTTP.METHOD_KEY);
        if (method == null)
        {
            throw new IllegalArgumentException("HTTP Method not set");
        }
        return method;
    }
}
