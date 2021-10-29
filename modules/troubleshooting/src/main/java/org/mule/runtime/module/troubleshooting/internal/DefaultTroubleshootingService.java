/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal;

import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingService;
import org.mule.runtime.module.troubleshooting.internal.operations.EventDumpOperation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultTroubleshootingService implements TroubleshootingService {

  private final Map<String, TroubleshootingOperationDefinition> definitionsByName = new HashMap<>();
  private final Map<String, TroubleshootingOperationCallback> callbacksByName = new HashMap<>();

  public DefaultTroubleshootingService(DeploymentService deploymentService) {
    addOperation(new EventDumpOperation(deploymentService));
  }

  private void addOperation(TroubleshootingOperation operation) {
    TroubleshootingOperationDefinition definition = operation.getDefinition();
    String operationName = definition.getName();
    this.definitionsByName.put(operationName, definition);

    TroubleshootingOperationCallback callback = operation.getCallback();
    this.callbacksByName.put(operationName, callback);
  }

  @Override
  public List<TroubleshootingOperationDefinition> getAvailableOperations() {
    return new ArrayList<>(definitionsByName.values());
  }

  @Override
  public Object executeOperation(String name, Map<String, String> arguments) {
    return callbacksByName.get(name).execute(arguments);
  }
}
