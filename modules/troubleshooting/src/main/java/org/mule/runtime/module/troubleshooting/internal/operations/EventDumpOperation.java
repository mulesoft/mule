/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal.operations;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;

import static org.apache.commons.lang3.StringUtils.leftPad;

import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.api.event.EventContextService.FlowStackEntry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.troubleshooting.api.ArgumentDefinition;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperation;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationCallback;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;
import org.mule.runtime.module.troubleshooting.internal.DefaultArgumentDefinition;
import org.mule.runtime.module.troubleshooting.internal.DefaultTroubleshootingOperationDefinition;

import java.io.IOException;
import java.io.Writer;

/**
 * Operation used to collect an event dump.
 * <p>
 * The name of the operation is "events".
 * <p>
 * Arguments:
 * <ul>
 * <li>application (Optional): The application to collect the event dump from</li>
 * </ul>
 */
public class EventDumpOperation implements TroubleshootingOperation {

  public static final String EVENT_DUMP_OPERATION_NAME = "events";
  public static final String EVENT_DUMP_OPERATION_DESCRIPTION = "Collects an EventDump of currently active events";

  public static final String APPLICATION_ARGUMENT_NAME = "application";
  public static final String APPLICATION_ARGUMENT_DESCRIPTION = "Application to collect the event dump from";

  private static final TroubleshootingOperationDefinition definition = createOperationDefinition();

  private final DeploymentService deploymentService;

  public EventDumpOperation(DeploymentService deploymentService) {
    this.deploymentService = deploymentService;
  }

  @Override
  public TroubleshootingOperationDefinition getDefinition() {
    return definition;
  }

  @Override
  public TroubleshootingOperationCallback getCallback() {
    return (arguments, writer) -> {
      final String applicationName = arguments.get(APPLICATION_ARGUMENT_NAME);
      if (applicationName == null) {
        writeFlowStacksForAllApplications(writer);
      } else {
        Application application = deploymentService.findApplication(applicationName);
        writeFlowStackEntries(application, writer);
      }
    };
  }

  private static void writeFlowStacksFor(Application application, Writer writer)
      throws IOException {
    final var appsTitle = "Active Events for application '" + application.getArtifactName() + "'";
    writer.write(appsTitle + lineSeparator());
    writer.write(leftPad("", appsTitle.length(), "-") + lineSeparator());
    writer.write(lineSeparator());

    writeFlowStackEntries(application, writer);
  }

  private void writeFlowStacksForAllApplications(Writer writer) throws IOException {
    for (Application application : deploymentService.getApplications()) {
      writeFlowStacksFor(application, writer);
    }
  }

  private static void writeFlowStackEntries(Application application, Writer writer) throws IOException {
    EventContextService eventContextService = application
        .getArtifactContext()
        .getRegistry()
        .lookupByName(EventContextService.REGISTRY_KEY)
        .map(EventContextService.class::cast)
        .orElseThrow(() -> new IllegalArgumentException(format("Could not get EventContextService for application %s.",
                                                               application.getArtifactName())));

    final var currentlyActiveFlowStacks = eventContextService.getCurrentlyActiveFlowStacks();

    for (FlowStackEntry fs : currentlyActiveFlowStacks) {
      writer.write(format("\"%s\"%n%s",
                          fs.getEventId(),
                          fs.getFlowCallStack().toString().indent(4)));
      writer.write(lineSeparator());
    }
  }

  private static TroubleshootingOperationDefinition createOperationDefinition() {
    return new DefaultTroubleshootingOperationDefinition(EVENT_DUMP_OPERATION_NAME, EVENT_DUMP_OPERATION_DESCRIPTION,
                                                         createApplicationArgumentDefinition());
  }

  private static ArgumentDefinition createApplicationArgumentDefinition() {
    return new DefaultArgumentDefinition(APPLICATION_ARGUMENT_NAME, APPLICATION_ARGUMENT_DESCRIPTION, false);
  }
}
