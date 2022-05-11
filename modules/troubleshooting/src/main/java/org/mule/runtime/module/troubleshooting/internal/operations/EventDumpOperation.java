/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal.operations;

import static java.lang.String.format;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.api.event.EventContextService.FlowStackEntry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.troubleshooting.api.ArgumentDefinition;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;
import org.mule.runtime.module.troubleshooting.internal.DefaultArgumentDefinition;
import org.mule.runtime.module.troubleshooting.internal.DefaultTroubleshootingOperationDefinition;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperation;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationCallback;

import java.util.List;

/**
 * Operation used to collect an event dump in JSON format.
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
  public static final String EVENT_DUMP_OPERATION_DESCRIPTION = "Collects an EventDump in JSON format";

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
    return arguments -> {
      JSONObject flowStacks = new JSONObject();
      final String applicationName = arguments.get(APPLICATION_ARGUMENT_NAME);
      if (applicationName == null) {
        addFlowStacksForAllApplications(flowStacks);
      } else {
        Application application = deploymentService.findApplication(applicationName);
        addFlowStacksFor(application, flowStacks);
      }
      return flowStacks.toString(2);
    };
  }

  private static void addFlowStacksFor(Application application, JSONObject flowStacks) {
    flowStacks.put(application.getArtifactName(), getFlowStackEntries(application));
  }

  private void addFlowStacksForAllApplications(JSONObject flowStacks) {
    for (Application application : deploymentService.getApplications()) {
      addFlowStacksFor(application, flowStacks);
    }
  }

  private static JSONArray getFlowStackEntries(Application application) {
    EventContextService eventContextService = application
        .getArtifactContext()
        .getRegistry()
        .lookupByName(EventContextService.REGISTRY_KEY)
        .map(EventContextService.class::cast)
        .orElseThrow(() -> new IllegalArgumentException(format("Could not get EventContextService for application %s.",
                                                               application.getArtifactName())));

    return flowStackEntriesToJSON(eventContextService.getCurrentlyActiveFlowStacks());
  }

  private static JSONArray flowStackEntriesToJSON(List<FlowStackEntry> flowStackEntries) {
    JSONArray entriesArrayAsJSON = new JSONArray();
    for (FlowStackEntry flowStackEntry : flowStackEntries) {
      entriesArrayAsJSON.put(flowStackEntryToJSON(flowStackEntry));
    }
    return entriesArrayAsJSON;
  }

  private static JSONObject flowStackEntryToJSON(FlowStackEntry flowStackEntry) {
    JSONObject entryAsJSON = new JSONObject();
    entryAsJSON.put("eventId", flowStackEntry.getEventId());
    entryAsJSON.put("serverId", flowStackEntry.getServerId());
    entryAsJSON.put("flowCallStack", flowCallStackToJSON(flowStackEntry.getFlowCallStack()));
    return entryAsJSON;
  }

  private static JSONArray flowCallStackToJSON(FlowCallStack flowCallStack) {
    JSONArray callStackAsJSON = new JSONArray();
    for (FlowStackElement element : flowCallStack.getElements()) {
      callStackAsJSON.put(element.toString());
    }
    return callStackAsJSON;
  }

  private static TroubleshootingOperationDefinition createOperationDefinition() {
    return new DefaultTroubleshootingOperationDefinition(EVENT_DUMP_OPERATION_NAME, EVENT_DUMP_OPERATION_DESCRIPTION,
                                                         createApplicationArgumentDefinition());
  }

  private static ArgumentDefinition createApplicationArgumentDefinition() {
    return new DefaultArgumentDefinition(APPLICATION_ARGUMENT_NAME, APPLICATION_ARGUMENT_DESCRIPTION, false);
  }
}
