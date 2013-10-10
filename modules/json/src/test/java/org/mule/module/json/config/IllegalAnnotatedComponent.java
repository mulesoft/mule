/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.config;

import org.mule.api.annotations.expression.JsonPath;

import java.util.Map;

public class IllegalAnnotatedComponent
{
    public Map<String, Object> doStuff3(
            @JsonPath("/foo/bar[1]") Exception bar)
    {
        //this will fail since you cannot return an integer from an XPath expression
        return null;
    }
}
