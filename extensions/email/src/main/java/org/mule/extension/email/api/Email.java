/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import org.mule.runtime.extension.api.annotation.Parameter;

/**
 * //TODO
 */
public class Email
{

    @Parameter
    private EmailContent content;

    @Parameter
    private EmailAttributes attributes;

    public Email()
    {
    }

    public Email(EmailContent emailContent, EmailAttributes attributes)
    {
        this.content = emailContent;
        this.attributes = attributes;
    }

    public EmailContent getContent()
    {
        return content;
    }

    public EmailAttributes getAttributes()
    {
        return attributes;
    }

}
