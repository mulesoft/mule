/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection;

import org.mule.extensions.jms.JmsExtension;

/**
 * Versions of the JMS Spec supported by the {@link JmsExtension}
 *
 * @since 4.0
 */
public enum JmsSpecification {
  JMS_1_0_2b("1.0.2b"), JMS_1_1("1.1"), JMS_2_0("2.0");

  private final String name;

  JmsSpecification(String s) {
    name = s;
  }

  public String getName() {
    return name;
  }
}
