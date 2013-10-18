/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.api.annotations.expression.XPath;

import java.util.Map;

public class IllegalAnnotatedComponent
{
    public Map<String, Object> doStuff3(
            @XPath("/foo/bar[1]") Integer bar)
    {
        //this will fail since you cannot return an integer from an XPath expression
        return null;
    }
}
