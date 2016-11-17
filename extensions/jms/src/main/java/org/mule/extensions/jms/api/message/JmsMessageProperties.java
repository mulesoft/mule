/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.message;

import java.util.Map;

import javax.jms.Message;

/**
 * Container element for all the properties present in a JMS {@link Message}.
 * <p>
 * This container not only allows to fetch the all properties in a single Map representation,
 * but also provides accessors for the properties according to their origin.
 * Properties may be those predefined by JMS (the {@link JmsxProperties}),
 * those that are used by the JMS broker or provider (known as plain JMS properties),
 * and finally the ones provided by the User who created the {@link Message}.
 *
 * @since 4.0
 */
public interface JmsMessageProperties {

  /**
   * @return all the properties of the JMS message as a flattened map
   */
  Map<String, Object> asMap();

  /**
   * @return the {@link JmsxProperties} of the {@link Message}
   */
  JmsxProperties getJmsxProperties();

  /**
   * @return the broker and provider specific of the {@link Message}
   */
  Map<String, Object> getJmsProperties();

  /**
   * @return the user provider properties of the {@link Message}
   */
  Map<String, Object> getUserProperties();

}
