/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

/**
 * Mail properties that are sent on the DefaultMuleMessage when receiving a Mail Message or which can be set
 * on the endpoint or event to be added to outgoing mail
 */
public interface MailProperties
{
    /**
     * MuleEvent properties
     */
    public static final String CONTENT_TYPE_PROPERTY = "contentType";
    public static final String TO_ADDRESSES_PROPERTY = "toAddresses";
    public static final String CC_ADDRESSES_PROPERTY = "ccAddresses";
    public static final String BCC_ADDRESSES_PROPERTY = "bccAddresses";
    public static final String FROM_ADDRESS_PROPERTY = "fromAddress";
    public static final String REPLY_TO_ADDRESSES_PROPERTY = "replyToAddresses";
    public static final String SUBJECT_PROPERTY = "subject";
    public static final String CUSTOM_HEADERS_MAP_PROPERTY = "customHeaders";
    public static final String SENT_DATE_PROPERTY = "sentDate";
}
