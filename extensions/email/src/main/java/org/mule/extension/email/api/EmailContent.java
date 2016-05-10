/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * Represents and enables the construction of the content
 *
 * @since 4.0
 */
public class EmailContent
{

    public EmailContent()
    {
    }

    public EmailContent(String body, String contentType)
    {
        this.body = body;
        this.contentType = contentType;
    }

    /**
     * The text body of the message content.
     * <p>
     * The default value is an empty body.
     */
    @Parameter
    @Optional(defaultValue = " ")
    private String body;

    /**
     * The contentType of the body. One of "text/html" or "text/plain"
     * <p>
     * The default value is "text/plain"
     */
    @Parameter
    @Optional(defaultValue = "text/plain")
    private String contentType;

    /**
     * @return the body of the message content. The body
     * aims to be text.
     */
    public String getBody()
    {
        return body;
    }

    /**
     * @return the contentType of the body. one of "text/html" or "text/plain"
     */
    public String getContentType()
    {
        return contentType;
    }
}
