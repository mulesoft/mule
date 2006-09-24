/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

// @Immutable
public class JmsConstants
{
    public static final String JMS_SPECIFICATION_102B = "1.0.2b";
    public static final String JMS_SPECIFICATION_11 = "1.1";

    public static final String JMS_CORRELATION_ID = "JMSCorrelationID";
    public static final String JMS_DELIVERY_MODE = "JMSDeliveryMode";
    public static final String JMS_DESTINATION = "JMSDestination";
    public static final String JMS_EXPIRATION = "JMSExpiration";
    public static final String JMS_MESSAGE_ID = "JMSMessageID";
    public static final String JMS_PRIORITY = "JMSPriority";
    public static final String JMS_REDELIVERED = "JMSRedelivered";
    public static final String JMS_REPLY_TO = "JMSReplyTo";
    public static final String JMS_TIMESTAMP = "JMSTimestamp";
    public static final String JMS_TYPE = "JMSType";

    // QoS properties
    public static final String TIME_TO_LIVE_PROPERTY = "timeToLive";
    public static final String PERSISTENT_DELIVERY_PROPERTY = "persistentDelivery";
    public static final String PRIORITY_PROPERTY = "priority";
    public static final String JMS_SELECTOR_PROPERTY = "selector";
    public static final String TOPIC_PROPERTY = "topic";
    public static final String DURABLE_PROPERTY = "durable";
    public static final String DURABLE_NAME_PROPERTY = "durableName";
    public static final String CACHE_JMS_SESSIONS_PROPERTY = "cacheJmsSessions";

}
