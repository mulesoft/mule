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
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;

import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
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
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

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
    final var roots = hierarchicalEvents(currentlyActiveFlowStacks);

    writer.write(format(""
        + "Total Event Contexts: %6d%n"
        + "Total Root Contexts:  %6d%n"
        + "%n",
                        currentlyActiveFlowStacks.size(),
                        roots.size()));

    for (NestedEventsNode rootFlowStackNode : roots) {
      if (rootFlowStackNode.getDirectChidren().isEmpty()) {
        writer.write(formatFlowStack(rootFlowStackNode.getFlowStack()));
        writer.write(lineSeparator());
      } else {
        writer.write(format("\"%s\" hierarchy%n%n", rootFlowStackNode.getEventId()));
        writeHierarchy(writer, rootFlowStackNode, 0);
      }
    }
  }

  private static Collection<NestedEventsNode> hierarchicalEvents(final List<FlowStackEntry> currentlyActiveFlowStacks) {
    // use treesets to ensure consistent sorting in the report

    var nodesById = new TreeMap<String, NestedEventsNode>();
    for (FlowStackEntry fs : currentlyActiveFlowStacks) {
      final NestedEventsNode node = new NestedEventsNode(fs.getEventId(), fs);
      nodesById.put(fs.getEventId(), node);
    }

    var roots = new TreeMap<String, NestedEventsNode>();
    for (Entry<String, NestedEventsNode> node : nodesById.entrySet()) {
      final var parentEventId = node.getValue().getFlowStack().getParentEventId();
      if (parentEventId != null) {
        nodesById.get(parentEventId).addDirectChild(node.getValue());
      } else {
        roots.put(node.getKey(), node.getValue());
      }
    }
    return roots.values();
  }

  private static void writeHierarchy(Writer writer, NestedEventsNode node, int identIndex) throws IOException {
    for (NestedEventsNode childNode : node.getDirectChidren()) {
      writeHierarchy(writer, childNode, identIndex + 4);
    }

    writer.write(formatFlowStack(node.getFlowStack()).indent(identIndex));
    writer.write(lineSeparator());
  }

  private static class NestedEventsNode {

    private final String eventId;

    private final FlowStackEntry flowStack;

    private SortedMap<String, NestedEventsNode> directChildren = new TreeMap<>();

    public NestedEventsNode(String eventId, FlowStackEntry flowStack) {
      this.eventId = eventId;
      this.flowStack = flowStack;
    }

    public String getEventId() {
      return eventId;
    }

    public FlowStackEntry getFlowStack() {
      return flowStack;
    }

    public void addDirectChild(NestedEventsNode child) {
      directChildren.put(child.getEventId(), child);
    }

    public Collection<NestedEventsNode> getDirectChidren() {
      return directChildren.values();
    }

  }

  private static String formatFlowStack(FlowStackEntry fs) throws IOException {
    return format("\"%s\", running for: %s, state: %s%n%s",
                  fs.getEventId(),
                  formatDuration(fs.getExecutingTime().toMillis(), "mm:ss.SSS"),
                  fs.getState().name(),
                  flowCallStackString(fs.getFlowCallStack()).indent(4));
  }

  // put this logic here so the current representation of stacks that is logged is not modified.
  private static String flowCallStackString(FlowCallStack flowCallStack) {
    StringBuilder stackString = new StringBuilder(256);

    int i = 0;
    final var flowStackElements = flowCallStack.getElements();
    for (FlowStackElement flowStackElement : flowStackElements) {
      stackString.append("at ").append(flowStackElement.toStringEventDumpFormat());
      if (++i != flowCallStack.getElements().size()) {
        stackString.append(lineSeparator());
      }
    }
    return stackString.toString();
  }

  private static TroubleshootingOperationDefinition createOperationDefinition() {
    return new DefaultTroubleshootingOperationDefinition(EVENT_DUMP_OPERATION_NAME, EVENT_DUMP_OPERATION_DESCRIPTION,
                                                         createApplicationArgumentDefinition());
  }

  private static ArgumentDefinition createApplicationArgumentDefinition() {
    return new DefaultArgumentDefinition(APPLICATION_ARGUMENT_NAME, APPLICATION_ARGUMENT_DESCRIPTION, false);
  }
}
