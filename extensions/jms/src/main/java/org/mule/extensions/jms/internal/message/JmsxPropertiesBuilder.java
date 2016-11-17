/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.message;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.String.valueOf;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXAppID;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXConsumerTXID;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXDeliveryCount;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXGroupID;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXGroupSeq;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXProducerTXID;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXRcvTimestamp;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXUserID;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSX_NAMES;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.extensions.jms.api.message.JmsxProperties;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Message;

/**
 * Builder that provides a simple way of creating a {@link JmsxProperties} instance based on
 * the predefined properties {@link JMSXDefinedPropertiesNames names}.
 * <p>
 * This is useful for converting the properties {@link Map} found in the original {@link Message}
 * to their representation as {@link JmsxProperties}.
 * A default value is provided for the properties that are not set.
 *
 * @since 4.0
 */
final class JmsxPropertiesBuilder {

  private final Map<String, Object> properties = new HashMap<>();

  private JmsxPropertiesBuilder() {}

  public static JmsxPropertiesBuilder create() {
    return new JmsxPropertiesBuilder();
  }

  public JmsxPropertiesBuilder add(String key, Object value) {
    checkArgument(JMSX_NAMES.contains(key),
                  format("Invalid key [%s], supported keys for JMSXProperties are [%s]", key, join(", ", JMSX_NAMES)));

    properties.put(key, value);
    return this;
  }

  public JmsxProperties build() {
    return new JmsxProperties(valueOf(properties.getOrDefault(JMSXUserID, "")),
                              valueOf(properties.getOrDefault(JMSXAppID, "")),
                              Integer.valueOf(properties.getOrDefault(JMSXDeliveryCount, "1").toString()),
                              valueOf(properties.getOrDefault(JMSXGroupID, "")),
                              Integer.valueOf(properties.getOrDefault(JMSXGroupSeq, "1").toString()),
                              valueOf(properties.getOrDefault(JMSXProducerTXID, "")),
                              valueOf(properties.getOrDefault(JMSXConsumerTXID, "")),
                              Long.valueOf(properties.getOrDefault(JMSXRcvTimestamp, "0").toString()));
  }
}
