/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * Representation of an HTTP attachment.
 */
@Alias("part")
public class HttpPart
{
    @Parameter
    private String id;

    @Parameter
    private Object data;

    @Parameter
    private String contentType;

    @Parameter
    @Optional
    private String filename;

    public Object getData()
    {
        return data;
    }

    public String getContentType()
    {
        return contentType;
    }

    public String getId()
    {
        return id;
    }

    public String getFilename()
    {
        return filename;
    }
}
