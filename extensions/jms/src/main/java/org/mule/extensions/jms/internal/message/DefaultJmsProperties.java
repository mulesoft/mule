/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.message;


import static com.google.common.collect.ImmutableMap.copyOf;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSX_NAMES;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.extensions.jms.api.message.JmsMessageProperties;
import org.mule.extensions.jms.api.message.JmsxProperties;

import java.util.HashMap;
import java.util.Map;

public class DefaultJmsProperties implements JmsMessageProperties {

  private static final String JMSX_PREFIX = "JMSX";
  private static final String JMS_PREFIX = "JMS";

  private final Map<String, Object> allPropertiesMap;

  private final Map<String, Object> userProperties = new HashMap<>();

  private final Map<String, Object> jmsProperties = new HashMap<>();

  private JmsxProperties jmsxProperties;

  public DefaultJmsProperties(Map<String, Object> messageProperties) {
    checkArgument(messageProperties != null, "Initializer properties Map expected, but it was null");

    allPropertiesMap = copyOf(messageProperties);
    JmsxPropertiesBuilder jmsxPropertiesBuilder = JmsxPropertiesBuilder.create();

    allPropertiesMap.entrySet().forEach(e -> {
      String key = e.getKey();
      if (key.startsWith(JMSX_PREFIX) && JMSX_NAMES.contains(key)) {
        jmsxPropertiesBuilder.add(key, e.getValue());

      } else if (key.startsWith(JMS_PREFIX)) {
        jmsProperties.put(key, e.getValue());

      } else {
        userProperties.put(key, e.getValue());
      }
    });

    jmsxProperties = jmsxPropertiesBuilder.build();
  }

  @Override
  public Map<String, Object> asMap() {
    return copyOf(allPropertiesMap);
  }

  @Override
  public Map<String, Object> getUserProperties() {
    return copyOf(userProperties);
  }

  @Override
  public Map<String, Object> getJmsProperties() {
    return copyOf(jmsProperties);
  }

  @Override
  public JmsxProperties getJmsxProperties() {
    return jmsxProperties;
  }


  @Override
  public boolean equals(Object o) {
    return allPropertiesMap.equals(o);
  }

  @Override
  public int hashCode() {
    return allPropertiesMap.hashCode();
  }

}
