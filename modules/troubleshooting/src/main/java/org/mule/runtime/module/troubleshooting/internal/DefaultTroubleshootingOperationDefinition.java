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

import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;

import java.util.List;

public class DefaultTroubleshootingOperationDefinition implements TroubleshootingOperationDefinition {

  private static final long serialVersionUID = -1540639676860149452L;

  private final String name;
  private final List<String> argumentNames;

  public DefaultTroubleshootingOperationDefinition(String name, String... argumentNames) {
    this.name = name;
    this.argumentNames = stream(argumentNames).collect(toList());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<String> getArgumentNames() {
    return unmodifiableList(argumentNames);
  }
}
