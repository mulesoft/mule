/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;


import static org.mule.extension.email.internal.util.EmailConnectorUtils.TEXT_PLAIN;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.UTF_8;
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


    public EmailContent(String body)
    {
        this.body = body;
        this.contentType = TEXT_PLAIN;
        this.charset = UTF_8;
    }

    public EmailContent(String body, MediaType contentType, Charset charset)
    {
        this.body = body;
        this.contentType = contentType.toString();
        this.charset = charset.name();
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
    @Optional(defaultValue = TEXT_PLAIN)
    private String contentType;

    /**
     * The character encoding of the body.
     * <p>
     * The default value is "UTF-8"
     */
    @Parameter
    @Optional(defaultValue = UTF_8)
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
        return DataType.builder().mediaType(contentType).build().getMediaType();
    }

    /**
     * @return the charset of the body.
     */
    public Charset getCharset()
    {
        return Charset.forName(charset);
    }
}
