/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.launcher;

import org.codehaus.jackson.annotate.JsonProperty;

public class HTTPResponse
{
    @JsonProperty
    private String response;

    /**
     *
     */
    public HTTPResponse()
    {
        this(null);
    }

    public HTTPResponse(String response)
    {
        super();
        setResponse(response);
    }

    public String getResponse()
    {
        return response;
    }

    public void setResponse(String response)
    {
        this.response = response;
    }
}
