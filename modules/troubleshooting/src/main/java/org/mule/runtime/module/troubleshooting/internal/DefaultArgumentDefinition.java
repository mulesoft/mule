/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal;

import org.mule.runtime.module.troubleshooting.api.ArgumentDefinition;

public class DefaultArgumentDefinition implements ArgumentDefinition {

  private static final long serialVersionUID = -6414068782959732107L;

  private final String name;
  private final String description;
  private final boolean required;

  public DefaultArgumentDefinition(String name, String description, boolean required) {
    this.name = name;
    this.description = description;
    this.required = required;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public boolean isRequired() {
    return required;
  }
}
