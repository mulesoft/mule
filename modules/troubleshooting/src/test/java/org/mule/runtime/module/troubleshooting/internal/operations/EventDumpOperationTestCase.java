/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal.operations;

import static org.mule.runtime.module.troubleshooting.internal.TroubleshootingTestUtils.mockApplication;
import static org.mule.runtime.module.troubleshooting.internal.TroubleshootingTestUtils.mockDeploymentService;
import static org.mule.runtime.module.troubleshooting.internal.TroubleshootingTestUtils.mockFlowStackEntry;
import static org.mule.runtime.module.troubleshooting.internal.operations.EventDumpOperation.APPLICATION_ARGUMENT_DESCRIPTION;
import static org.mule.runtime.module.troubleshooting.internal.operations.EventDumpOperation.APPLICATION_ARGUMENT_NAME;
import static org.mule.runtime.module.troubleshooting.internal.operations.EventDumpOperation.EVENT_DUMP_OPERATION_DESCRIPTION;
import static org.mule.runtime.module.troubleshooting.internal.operations.EventDumpOperation.EVENT_DUMP_OPERATION_NAME;

import static java.time.Clock.fixed;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneId.of;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.api.event.EventContextService.FlowStackEntry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.troubleshooting.api.ArgumentDefinition;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class EventDumpOperationTestCase {

  private DeploymentService deploymentService;
  private EventDumpOperation eventDumpOperation;

  @BeforeEach
  public void setup() {
    FlowStackEntry flowStackEntry = mockFlowStackEntry("001");
    Application app1 = mockApplication("app1",
                                       // shufled to ensure sorting
                                       mockFlowStackEntry("001_z", flowStackEntry),
                                       // an envent context withput hierarchy
                                       mockFlowStackEntry("abc"),
                                       mockFlowStackEntry("001_1", flowStackEntry),
                                       flowStackEntry);
    Application app2 = mockApplication("app2");
    deploymentService = mockDeploymentService(app1, app2);
    eventDumpOperation = new EventDumpOperation(deploymentService);
  }

  @Test
  public void definitionHasCorrectNameDescriptionAndNumberOfArguments() {
    assertThat(eventDumpOperation.getDefinition().getName(), is(EVENT_DUMP_OPERATION_NAME));
    assertThat(eventDumpOperation.getDefinition().getDescription(), is(EVENT_DUMP_OPERATION_DESCRIPTION));
    assertThat(eventDumpOperation.getDefinition().getArgumentDefinitions(), iterableWithSize(1));
  }

  @Test
  public void applicationArgumentDefinitionIsCorrect() {
    ArgumentDefinition applicationArgumentDefinition = eventDumpOperation.getDefinition().getArgumentDefinitions().get(0);
    assertThat(applicationArgumentDefinition.getName(), is(APPLICATION_ARGUMENT_NAME));
    assertThat(applicationArgumentDefinition.getDescription(), is(APPLICATION_ARGUMENT_DESCRIPTION));
    assertThat(applicationArgumentDefinition.isRequired(), is(false));
  }

  @Test
  public void whenNoApplicationIsPassedItReturnsAllApplications() throws IOException {
    final var writer = new StringWriter();
    executeEventDump(emptyMap(), writer);
    Object result = writer.toString();

    var expected = """
        Active Events for application 'app1'
        ------------------------------------

        Total Event Contexts:      4
        Total Root Contexts:       2

        "001" hierarchy

            "001_1", running for: 00:00.000, state: EXECUTING
                at ns:component@MockLocation(null) 66 ms

            "001_z", running for: 00:00.000, state: EXECUTING
                at ns:component@MockLocation(null) 66 ms

        "001", running for: 00:00.000, state: EXECUTING
            at ns:component@MockLocation(null) 66 ms

        "abc", running for: 00:00.000, state: EXECUTING
            at ns:component@MockLocation(null) 66 ms

        Active Events for application 'app2'
        ------------------------------------

        Total Event Contexts:      0
        Total Root Contexts:       0

        """;
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  public void whenApplicationIsPassedItReturnsOnlyThePassedOne() throws IOException {
    final var writer = new StringWriter();
    executeEventDump(singletonMap(APPLICATION_ARGUMENT_NAME, "app1"), writer);
    String result = writer.toString();

    var expected = """
        Total Event Contexts:      4
        Total Root Contexts:       2

        "001" hierarchy

            "001_1", running for: 00:00.000, state: EXECUTING
                at ns:component@MockLocation(null) 66 ms

            "001_z", running for: 00:00.000, state: EXECUTING
                at ns:component@MockLocation(null) 66 ms

        "001", running for: 00:00.000, state: EXECUTING
            at ns:component@MockLocation(null) 66 ms

        "abc", running for: 00:00.000, state: EXECUTING
            at ns:component@MockLocation(null) 66 ms

        """;
    assertThat(result, is(equalTo(expected)));
  }

  private void executeEventDump(Map<String, String> args, final StringWriter writer) throws IOException {
    Instant instant = now(fixed(ofEpochMilli(66), of("UTC")));
    try (MockedStatic<Instant> mockedStatic = mockStatic(Instant.class)) {
      mockedStatic.when(Instant::now).thenReturn(instant);

      eventDumpOperation.getCallback().execute(args, writer);
    }
  }

  @Test
  public void whenTheEventContextServiceIsNotPresentItRaisesAnException() throws IOException {
    for (Application application : deploymentService.getApplications()) {
      Registry registry = application.getArtifactContext().getRegistry();
      when(registry.lookupByName(EventContextService.REGISTRY_KEY)).thenReturn(empty());
    }

    Map<String, String> arguments = new HashMap<>();
    arguments.put(APPLICATION_ARGUMENT_NAME, "app1");
    final var writer = new StringWriter();
    assertThrows(IllegalArgumentException.class, () -> executeEventDump(arguments, writer));
  }
}
