/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.api.event.EventContextService.FlowStackEntry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.module.deployment.api.DeploymentService;

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

    ArtifactContext artifactContext = mock(ArtifactContext.class);
    when(artifactContext.getRegistry()).thenReturn(registry);
    when(mockApp.getArtifactContext()).thenReturn(artifactContext);
    return mockApp;
  }

  public static FlowStackEntry mockFlowStackEntry() {
    FlowStackEntry mockEntry = mock(FlowStackEntry.class, withSettings().name("<FlowStackEntry>"));
    when(mockEntry.getEventId()).thenReturn("EventId");
    when(mockEntry.getServerId()).thenReturn("ServerId");

    FlowStackElement flowStackElement = new FlowStackElement("MockFlow", "MockLocation");

    FlowCallStack flowCallStack = mock(FlowCallStack.class, withSettings().name("<FlowCallStack>"));
    when(flowCallStack.getElements()).thenReturn(singletonList(flowStackElement));

    when(mockEntry.getFlowCallStack()).thenReturn(flowCallStack);
    return mockEntry;
  }
}
