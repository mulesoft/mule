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
 * Represents and enables the construction of an email attachment.
 *
 * @since 4.0
 */
public class EmailAttachment
{

    public EmailAttachment()
    {
    }

    public EmailAttachment(String id, Object content, String contentType)
    {
        this.id = id;
        this.content = content;
        this.contentType = contentType;
    }

    /**
     * the name of the attachment.
     */
    @Parameter
    private String id;

    /**
     * the content of the attachment.
     */
    //TODO add @NoRef when is available - MULE-9811
    @Parameter
    private Object content;

    /**
     * the content type of the attachment content.
     */
    @Parameter
    @Optional
    private String contentType;

    /**
     * @return the name of the attachment.
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return the content of the attachment.
     */
    public Object getContent()
    {
        return content;
    }

    /**
     * @return the content type of the attachment content.
     */
    public String getContentType()
    {
        return contentType;
    }
}
