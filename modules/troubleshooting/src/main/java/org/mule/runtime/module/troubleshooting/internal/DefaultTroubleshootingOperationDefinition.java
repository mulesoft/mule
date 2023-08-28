/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.troubleshooting.internal;

import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.module.troubleshooting.api.ArgumentDefinition;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;

import java.util.List;

public class DefaultTroubleshootingOperationDefinition implements TroubleshootingOperationDefinition {

  private static final long serialVersionUID = -1540639676860149452L;

  private final String name;
  private final String description;
  private final List<ArgumentDefinition> argumentDefinitions;

  public DefaultTroubleshootingOperationDefinition(String name, String description, ArgumentDefinition... argumentDefinitions) {
    this.name = name;
    this.description = description;
    this.argumentDefinitions = stream(argumentDefinitions).collect(toList());
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
  public List<ArgumentDefinition> getArgumentDefinitions() {
    return unmodifiableList(argumentDefinitions);
  }
}
