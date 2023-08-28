/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
