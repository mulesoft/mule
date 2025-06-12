/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;

import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.troubleshooting.api.ArgumentDefinition;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperation;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationCallback;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationException;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingService;
import org.mule.runtime.module.troubleshooting.internal.operations.AlertFuseboardOperation;
import org.mule.runtime.module.troubleshooting.internal.operations.BasicInfoOperation;
import org.mule.runtime.module.troubleshooting.internal.operations.EventDumpOperation;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DefaultTroubleshootingService implements TroubleshootingService {

  private final Map<String, TroubleshootingOperationDefinition> definitionsByName = new HashMap<>();
  private final Map<String, TroubleshootingOperationCallback> callbacksByName = new LinkedHashMap<>();

  public DefaultTroubleshootingService(DeploymentService deploymentService) {
    registerOperation(new BasicInfoOperation());
    registerOperation(new EventDumpOperation(deploymentService));
    registerOperation(new AlertFuseboardOperation(deploymentService));
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
  public String executeAllOperations(Map<String, String> arguments) throws TroubleshootingOperationException {
    final var writer = new StringWriter();
    writeHeader(writer);

    for (Entry<String, TroubleshootingOperationCallback> callbackEntry : callbacksByName.entrySet()) {
      doExecuteOperation(callbackEntry.getKey(), arguments, callbackEntry.getValue(), writer);
    }
    return writer.toString();
  }

  @Override
  public String executeOperation(String name, Map<String, String> arguments) throws TroubleshootingOperationException {
    final var writer = new StringWriter();
    writeHeader(writer);

    doExecuteOperation(name, arguments, getCallback(name, arguments), writer);
    return writer.toString();
  }

  private void writeHeader(final StringWriter writer) {
    writer.write("Mule Runtime supportability information" + lineSeparator());
    writer.write(lineSeparator());
    writer.write("  Generated at " + now().format(ISO_DATE_TIME) + lineSeparator());
    writer.write(lineSeparator());
  }

  private void doExecuteOperation(String name, Map<String, String> arguments, TroubleshootingOperationCallback callback,
                                  final StringWriter writer)
      throws TroubleshootingOperationException {
    final var niceName = capitalize(join(splitByCharacterTypeCamelCase(name), ' '));
    writer.write(niceName + lineSeparator());
    writer.write(leftPad("", niceName.length(), "=") + lineSeparator());
    writer.write(lineSeparator());

    try {
      callback.execute(arguments, writer);
    } catch (IOException e) {
      throw new TroubleshootingOperationException("Exception executing troubleshooting operation '" + name + ":(" + arguments
          + ")'", e);
    }

    writer.write(lineSeparator());
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
