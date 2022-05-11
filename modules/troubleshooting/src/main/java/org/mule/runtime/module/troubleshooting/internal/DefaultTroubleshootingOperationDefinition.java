/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
