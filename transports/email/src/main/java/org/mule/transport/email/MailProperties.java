/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    // these properties will be used to avoid conflict with the outbound SMTP properties
    public static final String INBOUND_PREFIX = "inbound.";
    public static final String INBOUND_CONTENT_TYPE_PROPERTY = INBOUND_PREFIX + "contentType";
    public static final String INBOUND_TO_ADDRESSES_PROPERTY = INBOUND_PREFIX + "toAddresses";
    public static final String INBOUND_CC_ADDRESSES_PROPERTY = INBOUND_PREFIX + "ccAddresses";
    public static final String INBOUND_BCC_ADDRESSES_PROPERTY = INBOUND_PREFIX + "bccAddresses";
    public static final String INBOUND_FROM_ADDRESS_PROPERTY = INBOUND_PREFIX + "fromAddress";
    public static final String INBOUND_REPLY_TO_ADDRESSES_PROPERTY = INBOUND_PREFIX + "replyToAddresses";
    public static final String INBOUND_SUBJECT_PROPERTY = INBOUND_PREFIX + "subject";
    public static final String INBOUND_CUSTOM_HEADERS_MAP_PROPERTY = INBOUND_PREFIX + "customHeaders";
    public static final String INBOUND_SENT_DATE_PROPERTY = INBOUND_PREFIX + "sentDate";
}
