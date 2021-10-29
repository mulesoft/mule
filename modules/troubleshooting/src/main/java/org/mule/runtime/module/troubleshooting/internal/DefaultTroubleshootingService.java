/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal;

import static java.lang.String.format;

import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.troubleshooting.api.ArgumentDefinition;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperation;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationCallback;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationException;
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
    registerOperation(new EventDumpOperation(deploymentService));
  }

  @Override
  public void registerOperation(TroubleshootingOperation operation) {
    TroubleshootingOperationDefinition definition = operation.getDefinition();
    String operationName = definition.getName();
    this.definitionsByName.put(operationName, definition);

    TroubleshootingOperationCallback callback = operation.getCallback();
    this.callbacksByName.put(operationName, callback);
  }

  @Override
  public void unregisterOperation(String name) {
    this.definitionsByName.remove(name);
    this.callbacksByName.remove(name);
  }

  @Override
  public List<TroubleshootingOperationDefinition> getAvailableOperations() {
    return new ArrayList<>(definitionsByName.values());
  }

  @Override
  public Object executeOperation(String name, Map<String, String> arguments) throws TroubleshootingOperationException {
    TroubleshootingOperationCallback callback = getCallback(name, arguments);
    return callback.execute(arguments);
  }

  private TroubleshootingOperationCallback getCallback(String operationName, Map<String, String> receivedArguments)
      throws TroubleshootingOperationException {
    TroubleshootingOperationCallback callback = callbacksByName.get(operationName);
    TroubleshootingOperationDefinition operationDefinition = definitionsByName.get(operationName);
    if (callback == null || operationDefinition == null) {
      throw new TroubleshootingOperationException(format("The operation '%s' is not supported or not available", operationName));
    }

    checkRequiredParametersArePresent(operationName, receivedArguments, operationDefinition);
    checkReceivedArgumentsAreExpected(operationName, receivedArguments, operationDefinition);
    return callback;
  }

  private void checkReceivedArgumentsAreExpected(String operationName, Map<String, String> receivedArguments,
                                                 TroubleshootingOperationDefinition operationDefinition)
      throws TroubleshootingOperationException {
    for (String receivedArgument : receivedArguments.keySet()) {
      operationDefinition.getArgumentDefinitions().stream()
          .map(ArgumentDefinition::getName)
          .filter(receivedArgument::equals)
          .findAny()
          .orElseThrow(() -> new TroubleshootingOperationException(format("Received unexpected argument '%s' when invoking operation '%s'",
                                                                          receivedArgument, operationName)));
    }
  }

  private void checkRequiredParametersArePresent(String operationName, Map<String, String> receivedArguments,
                                                 TroubleshootingOperationDefinition operationDefinition)
      throws TroubleshootingOperationException {
    for (ArgumentDefinition argumentDefinition : operationDefinition.getArgumentDefinitions()) {
      String argumentName = argumentDefinition.getName();
      if (argumentDefinition.isRequired() && !receivedArguments.containsKey(argumentName)) {
        throw new TroubleshootingOperationException(format("Missing required argument '%s' when invoking operation '%s'",
                                                           argumentName, operationName));
      }
    }
  }
}
