/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

/**
 * Mail properties that are sent on the MuleMessage when receiving a Mail Message or
 * which can be set on the endpoint or event to be added to outgoing mail
 */
public interface MailProperties
{
    /**
     * Event properties
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
    
    //these properties will be used to avoid conflict with the outbound SMTP properties
    public static final String INBOUND_CONTENT_TYPE_PROPERTY = "inbound.contentType";
    public static final String INBOUND_TO_ADDRESSES_PROPERTY = "inbound.toAddresses";
    public static final String INBOUND_CC_ADDRESSES_PROPERTY = "inbound.ccAddresses";
    public static final String INBOUND_BCC_ADDRESSES_PROPERTY = "inbound.bccAddresses";
    public static final String INBOUND_FROM_ADDRESS_PROPERTY = "inbound.fromAddress";
    public static final String INBOUND_REPLY_TO_ADDRESSES_PROPERTY = "inbound.replyToAddresses";
    public static final String INBOUND_SUBJECT_PROPERTY = "inbound.subject";
    public static final String INBOUND_CUSTOM_HEADERS_MAP_PROPERTY = "inbound.customHeaders";
    public static final String INBOUND_SENT_DATE_PROPERTY = "inbound.sentDate";
}
