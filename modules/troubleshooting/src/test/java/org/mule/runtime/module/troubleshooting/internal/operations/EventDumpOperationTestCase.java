/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal.operations;

import static com.google.gson.JsonParser.parseString;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.troubleshooting.internal.operations.EventDumpOperation.APPLICATION_ARGUMENT_DESCRIPTION;
import static org.mule.runtime.module.troubleshooting.internal.operations.EventDumpOperation.APPLICATION_ARGUMENT_NAME;
import static org.mule.runtime.module.troubleshooting.internal.operations.EventDumpOperation.EVENT_DUMP_OPERATION_DESCRIPTION;
import static org.mule.runtime.module.troubleshooting.internal.operations.EventDumpOperation.EVENT_DUMP_OPERATION_NAME;

import com.google.gson.JsonElement;
import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.api.event.EventContextService.FlowStackEntry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.troubleshooting.api.ArgumentDefinition;

import java.util.HashMap;
import java.util.Map;

public class EventDumpOperationTestCase {

  private DeploymentService deploymentService;
  private EventDumpOperation eventDumpOperation;

  @Before
  public void setup() {
    FlowStackEntry flowStackEntry = mockFlowStackEntry();
    Application app1 = mockApplication("app1", flowStackEntry);
    Application app2 = mockApplication("app2");
    deploymentService = mockDeploymentService(app1, app2);
    eventDumpOperation = new EventDumpOperation(deploymentService);
  }

  @Test
  public void definitionHasCorrectNameDescriptionAndNumberOfArguments() {
    assertThat(eventDumpOperation.getDefinition().getName(), is(EVENT_DUMP_OPERATION_NAME));
    assertThat(eventDumpOperation.getDefinition().getDescription(), is(EVENT_DUMP_OPERATION_DESCRIPTION));
    assertThat(eventDumpOperation.getDefinition().getArgumentDefinitions().size(), is(1));
  }

  @Test
  public void applicationArgumentDefinitionIsCorrect() {
    ArgumentDefinition applicationArgumentDefinition = eventDumpOperation.getDefinition().getArgumentDefinitions().get(0);
    assertThat(applicationArgumentDefinition.getName(), is(APPLICATION_ARGUMENT_NAME));
    assertThat(applicationArgumentDefinition.getDescription(), is(APPLICATION_ARGUMENT_DESCRIPTION));
    assertThat(applicationArgumentDefinition.isRequired(), is(false));
  }

  @Test
  public void whenNoApplicationIsPassedItReturnsAllApplications() {
    Map<String, String> argumentsWithoutApplication = new HashMap<>();
    Object result = eventDumpOperation.getCallback().execute(argumentsWithoutApplication);

    JsonElement resultJson = parseString((String) result);
    JsonElement expectedJson = parseString("{\"app1\":[{\"eventId\":\"EventId\",\"serverId\":\"ServerId\",\"flowCallStack\":[\"MockFlow(MockLocation)\"]}],\"app2\":[]}");
    assertThat(resultJson, is(equalTo(expectedJson)));
  }

  @Test
  public void whenApplicationIsPassedItReturnsOnlyThePassedOne() {
    Map<String, String> argumentsWithApplication = new HashMap<>();
    argumentsWithApplication.put(APPLICATION_ARGUMENT_NAME, "app1");
    Object result = eventDumpOperation.getCallback().execute(argumentsWithApplication);

    JsonElement resultJson = parseString((String) result);
    JsonElement expectedJson = parseString("{\"app1\":[{\"eventId\":\"EventId\",\"serverId\":\"ServerId\",\"flowCallStack\":[\"MockFlow(MockLocation)\"]}]}");
    assertThat(resultJson, is(equalTo(expectedJson)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenTheEventContextServiceIsNotPresentItRaisesAnException() {
    for (Application application : deploymentService.getApplications()) {
      Registry registry = application.getArtifactContext().getRegistry();
      when(registry.lookupByName(EventContextService.REGISTRY_KEY)).thenReturn(empty());
    }

    Map<String, String> arguments = new HashMap<>();
    arguments.put(APPLICATION_ARGUMENT_NAME, "app1");
    eventDumpOperation.getCallback().execute(arguments);
  }

  private DeploymentService mockDeploymentService(Application... applications) {
    DeploymentService deploymentService = mock(DeploymentService.class);
    when(deploymentService.getApplications()).thenReturn(asList(applications));
    for (Application application : applications) {
      when(deploymentService.findApplication(eq(application.getArtifactName()))).thenReturn(application);
    }
    return deploymentService;
  }

  private Application mockApplication(String appName, FlowStackEntry... flowStackEntries) {
    Application mockApp = mock(Application.class);
    when(mockApp.getArtifactName()).thenReturn(appName);

    EventContextService eventContextService = mock(EventContextService.class);
    when(eventContextService.getCurrentlyActiveFlowStacks()).thenReturn(asList(flowStackEntries));

    Registry registry = mock(Registry.class);
    when(registry.lookupByName(EventContextService.REGISTRY_KEY)).thenReturn(of(eventContextService));

    ArtifactContext artifactContext = mock(ArtifactContext.class);
    when(artifactContext.getRegistry()).thenReturn(registry);
    when(mockApp.getArtifactContext()).thenReturn(artifactContext);
    return mockApp;
  }

  private FlowStackEntry mockFlowStackEntry() {
    FlowStackEntry mockEntry = mock(FlowStackEntry.class);
    when(mockEntry.getEventId()).thenReturn("EventId");
    when(mockEntry.getServerId()).thenReturn("ServerId");

    FlowStackElement flowStackElement = new FlowStackElement("MockFlow", "MockLocation");

    FlowCallStack flowCallStack = mock(FlowCallStack.class);
    when(flowCallStack.getElements()).thenReturn(singletonList(flowStackElement));

    when(mockEntry.getFlowCallStack()).thenReturn(flowCallStack);
    return mockEntry;
  }
}
