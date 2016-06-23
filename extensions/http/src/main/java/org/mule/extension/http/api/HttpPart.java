/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * Representation of an HTTP part.
 *
 * @since 4.0
 */
@Alias("part")
public class HttpPart
{
    /**
     * Name to identify this part.
     */
    @Parameter
    private String id;

    /**
     * Actual value the part holds.
     */
    @Parameter
    private Object data;

    /**
     * Content type from the data.
     */
    @Parameter
    @Optional
    private MediaType contentType;

    /**
     * File name of this part, if required.
     */
    @Parameter
    @Optional
    private String filename;

    public HttpPart()
    {
    }

    public HttpPart(String id, Object data, MediaType contentType, String filename)
    {
        this.id = id;
        this.data = data;
        this.contentType = contentType;
        this.filename = filename;
    }

    public Object getData()
    {
        return data;
    }

    public MediaType getContentType()
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
