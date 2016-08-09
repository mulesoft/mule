/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import java.io.Serializable;

/**
 * A PropertyScope is used to associate a message property with a lifetime.
 */
public enum PropertyScope implements Serializable {

  INBOUND(PropertyScope.INBOUND_NAME), OUTBOUND(PropertyScope.OUTBOUND_NAME);

  public static final String INBOUND_NAME = "inbound";
  public static final String OUTBOUND_NAME = "outbound";
  public static final String FLOW_VAR_NAME = "flow variables";
  public static final String SESSION_VAR_NAME = "session variables";

  private String name;

  PropertyScope(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

}

