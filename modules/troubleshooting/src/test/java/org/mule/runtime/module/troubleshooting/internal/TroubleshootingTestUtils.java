/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.core.api.event.EventContextService.EventContextState.EXECUTING;

import static java.time.Clock.fixed;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneId.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.alert.MuleAlertingSupport;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.api.event.EventContextService.FlowStackEntry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.module.deployment.api.DeploymentService;

import java.time.Instant;

import org.mockito.MockedStatic;

public final class TroubleshootingTestUtils {

  private TroubleshootingTestUtils() {}

  public static DeploymentService mockDeploymentService(Application... applications) {
    DeploymentService deploymentService = mock(DeploymentService.class);
    when(deploymentService.getApplications()).thenReturn(asList(applications));
    for (Application application : applications) {
      when(deploymentService.findApplication(eq(application.getArtifactName()))).thenReturn(application);
    }
    return deploymentService;
  }

  public static Application mockApplication(String appName, FlowStackEntry... flowStackEntries) {
    Application mockApp = mock(Application.class);
    when(mockApp.getArtifactName()).thenReturn(appName);

    EventContextService eventContextService = mock(EventContextService.class);
    when(eventContextService.getCurrentlyActiveFlowStacks()).thenReturn(asList(flowStackEntries));

    Registry registry = mock(Registry.class);
    when(registry.lookupByName(EventContextService.REGISTRY_KEY)).thenReturn(of(eventContextService));
    when(registry.lookupByType(MuleAlertingSupport.class)).thenReturn(of(mock(MuleAlertingSupport.class)));

    ArtifactContext artifactContext = mock(ArtifactContext.class);
    when(artifactContext.getRegistry()).thenReturn(registry);
    when(mockApp.getArtifactContext()).thenReturn(artifactContext);
    return mockApp;
  }

  public static FlowStackEntry mockFlowStackEntry(String eventId) {
    FlowStackEntry mockEntry = mock(FlowStackEntry.class, withSettings().name("<FlowStackEntry>"));
    when(mockEntry.getEventId()).thenReturn(eventId);
    when(mockEntry.getServerId()).thenReturn("ServerId");
    when(mockEntry.getState()).thenReturn(EXECUTING);

    final var tci = TypedComponentIdentifier.builder()
        .identifier(buildFromStringRepresentation("ns:component"))
        .type(OPERATION)
        .build();
    final var location = mock(ComponentLocation.class);
    when(location.getComponentIdentifier()).thenReturn(tci);
    when(location.getLocation()).thenReturn("MockLocation");

    Instant instant = now(fixed(ofEpochMilli(0), of("UTC")));
    try (MockedStatic<Instant> mockedStatic = mockStatic(Instant.class)) {
      mockedStatic.when(Instant::now).thenReturn(instant);

      final var flowStackElement = new FlowStackElement("MockFlow", "MockLocation", location, emptyMap());

      FlowCallStack flowCallStack = mock(FlowCallStack.class, withSettings().name("<FlowCallStack>"));
      when(flowCallStack.getElements()).thenReturn(singletonList(flowStackElement));

      when(mockEntry.getFlowCallStack()).thenReturn(flowCallStack);
      return mockEntry;
    }
  }

  public static FlowStackEntry mockFlowStackEntry(String eventId, FlowStackEntry parent) {
    FlowStackEntry mockEntry = mockFlowStackEntry(eventId);
    final var parentEventId = parent.getEventId();
    when(mockEntry.getParentEventId()).thenReturn(parentEventId);
    return mockEntry;
  }
}
