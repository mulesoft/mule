/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

/**
 * Configuration of a custom parameter to extract from the token response.
 */
public class ParameterExtractor
{

    private String paramName;
    private String value;

    public void setParamName(final String paramName)
    {
        this.paramName = paramName;
    }

    public void setValue(final String value)
    {
        this.value = value;
    }

    /**
     * @return name of the parameter used to store it in the oauth context.
     */
    public String getParamName()
    {
        return paramName;
    }

    /**
     * @return value extracted from the token response.
     */
    public String getValue()
    {
        return value;
    }
}
