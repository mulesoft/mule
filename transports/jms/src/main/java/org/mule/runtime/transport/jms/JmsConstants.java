/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// @ThreadSafe
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

    // extended properties
    public static final String JMS_X_DELIVERY_COUNT = "JMSXDeliveryCount";

    // QoS properties
    public static final String TIME_TO_LIVE_PROPERTY = "timeToLive";
    public static final String PERSISTENT_DELIVERY_PROPERTY = "persistentDelivery";
    public static final String PRIORITY_PROPERTY = "priority";
    public static final String JMS_SELECTOR_PROPERTY = "selector";
    public static final String TOPIC_PROPERTY = "topic";
    public static final String DURABLE_PROPERTY = "durable";
    public static final String DURABLE_NAME_PROPERTY = "durableName";
    public static final String CACHE_JMS_SESSIONS_PROPERTY = "cacheJmsSessions";
    public static final String DISABLE_TEMP_DESTINATIONS_PROPERTY = "disableTemporaryReplyToDestinations";
    public static final String RETURN_ORIGINAL_MESSAGE_PROPERTY = "returnOriginalMessageAsReply";

    public static final Set JMS_PROPERTY_NAMES = Collections.unmodifiableSet(new HashSet(
        Arrays.asList(new String[]{JMS_SPECIFICATION_102B, JMS_SPECIFICATION_11, JMS_CORRELATION_ID,
            JMS_DELIVERY_MODE, JMS_DELIVERY_MODE, JMS_DESTINATION, JMS_EXPIRATION, JMS_MESSAGE_ID,
            JMS_PRIORITY, JMS_REDELIVERED, JMS_REPLY_TO, JMS_TIMESTAMP, JMS_TYPE, JMS_SELECTOR_PROPERTY})));

}
