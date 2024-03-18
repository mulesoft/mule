/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.troubleshooting.internal.TestTroubleshootingOperation.REQUIRED_ARGUMENT_NAME;
import static org.mule.runtime.module.troubleshooting.internal.TestTroubleshootingOperation.TEST_OPERATION_NAME;
import static org.mule.runtime.module.troubleshooting.internal.TroubleshootingTestUtils.mockApplication;
import static org.mule.runtime.module.troubleshooting.internal.TroubleshootingTestUtils.mockDeploymentService;
import static org.mule.runtime.module.troubleshooting.internal.TroubleshootingTestUtils.mockFlowStackEntry;
import static org.mule.runtime.module.troubleshooting.internal.operations.EventDumpOperation.EVENT_DUMP_OPERATION_NAME;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.core.api.event.EventContextService.FlowStackEntry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultTroubleshootingServiceTestCase {

  private DefaultTroubleshootingService troubleshootingService;

  @Before
  public void setUp() {
    FlowStackEntry flowStackEntry = mockFlowStackEntry();
    Application app1 = mockApplication("app1", flowStackEntry);
    Application app2 = mockApplication("app2");
    DeploymentService deploymentService = mockDeploymentService(app1, app2);
    troubleshootingService = new DefaultTroubleshootingService(deploymentService);
    troubleshootingService.registerOperation(new TestTroubleshootingOperation());
  }

  @After
  public void tearDown() {
    troubleshootingService.unregisterOperation(TEST_OPERATION_NAME);
  }

  @Test
  public void checkOperations() {
    List<TroubleshootingOperationDefinition> availableOperations = troubleshootingService.getAvailableOperations();
    assertThat(availableOperations.size(), is(2));

    List<String> operationNames = availableOperations.stream().map(TroubleshootingOperationDefinition::getName).collect(toList());
    assertThat(operationNames, containsInAnyOrder(EVENT_DUMP_OPERATION_NAME, TEST_OPERATION_NAME));
  }

  @Test(expected = TroubleshootingOperationException.class)
  public void tryToExecuteAnUnableOperationThrowsException() throws TroubleshootingOperationException {
    troubleshootingService.executeOperation("notExistingOperation", new HashMap<>());
  }

  @Test(expected = TroubleshootingOperationException.class)
  public void missingRequiredParameter() throws TroubleshootingOperationException {
    troubleshootingService.executeOperation(TEST_OPERATION_NAME, new HashMap<>());
  }

  @Test(expected = TroubleshootingOperationException.class)
  public void unexpectedParameter() throws TroubleshootingOperationException {
    Map<String, String> unexpectedParameter = new HashMap<>();
    unexpectedParameter.put(REQUIRED_ARGUMENT_NAME, "some value");
    unexpectedParameter.put("unexpected", "other value");
    troubleshootingService.executeOperation(TEST_OPERATION_NAME, unexpectedParameter);
  }

  @Test
  public void notIncludingOptionalIsOk() throws TroubleshootingOperationException {
    Map<String, String> onlyRequired = new HashMap<>();
    onlyRequired.put(REQUIRED_ARGUMENT_NAME, "some value");
    troubleshootingService.executeOperation(TEST_OPERATION_NAME, onlyRequired);
  }

}
