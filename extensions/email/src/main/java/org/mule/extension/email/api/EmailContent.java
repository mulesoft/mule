/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mule.runtime.api.metadata.MediaType.TEXT;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.nio.charset.Charset;

/**
 * Represents and enables the construction of the content of an email
 * with a body of type "text/*" and a specific character encoding.
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
     */
    @Parameter
    @Optional(defaultValue = "#[payload]")
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
     * The character encoding of the body.
     * <p>
     * The default value is "UTF-8"
     */
    @Parameter
    @Optional(defaultValue = "UTF-8")
    private String charset;

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
    public MediaType getContentType()
    {
        // TODO: remove if when MULE-9960 is fixed since default values are not being injected properly.
        return contentType == null ? TEXT : DataType.builder().mediaType(contentType).build().getMediaType();
    }

    /**
     * @return the charset of the body.
     */
    public Charset getCharset()
    {
        // TODO: remove if when MULE-9960 is fixed since default values are not being injected properly.
        return charset != null ? Charset.forName(charset) : UTF_8;
    }
}
