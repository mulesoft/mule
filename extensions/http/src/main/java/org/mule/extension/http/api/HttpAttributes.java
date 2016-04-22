/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import java.io.Serializable;
import java.util.Map;

import javax.activation.DataHandler;

/**
 * Base representation of HTTP message attributes.
 */
public abstract class HttpAttributes implements Serializable
{
    protected final Map<String, Object> headers;
    protected final Map<String, DataHandler> parts;

    public HttpAttributes(Map<String, Object> headers, Map<String, DataHandler> parts)
    {
        this.headers = headers;
        this.parts = parts;
    }

    public Map<String, Object> getHeaders()
    {
        return headers;
    }
    public Map<String, DataHandler> getParts()
    {
        return parts;
    }
}
