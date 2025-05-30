/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal;

import static org.mule.runtime.module.troubleshooting.internal.TestTroubleshootingOperation.REQUIRED_ARGUMENT_NAME;
import static org.mule.runtime.module.troubleshooting.internal.TestTroubleshootingOperation.TEST_OPERATION_NAME;
import static org.mule.runtime.module.troubleshooting.internal.TroubleshootingTestUtils.mockApplication;
import static org.mule.runtime.module.troubleshooting.internal.TroubleshootingTestUtils.mockDeploymentService;
import static org.mule.runtime.module.troubleshooting.internal.TroubleshootingTestUtils.mockFlowStackEntry;
import static org.mule.runtime.module.troubleshooting.internal.operations.BasicInfoOperation.BASIC_INFO_OPERATION_NAME;
import static org.mule.runtime.module.troubleshooting.internal.operations.EventDumpOperation.EVENT_DUMP_OPERATION_NAME;

import static java.util.Collections.emptyMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.hamcrest.core.StringContains.containsString;

import org.mule.runtime.core.api.event.EventContextService.FlowStackEntry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

    assertThat(availableOperations.stream().map(TroubleshootingOperationDefinition::getName).toList().toString(),
               availableOperations, iterableWithSize(3));

    List<String> operationNames = availableOperations.stream().map(TroubleshootingOperationDefinition::getName).toList();
    assertThat(operationNames, containsInAnyOrder(BASIC_INFO_OPERATION_NAME, EVENT_DUMP_OPERATION_NAME, TEST_OPERATION_NAME));
  }

  @Test
  public void tryToExecuteAllOperations() throws TroubleshootingOperationException {
    var result = troubleshootingService.executeAllOperations(emptyMap());

    assertThat(result, containsString("""
        Basic Info
        =========="""));
    assertThat(result, containsString("""
        Events
        ======"""));
    assertThat(result, containsString("""
        Test
        ===="""));
  }

  @Test(expected = TroubleshootingOperationException.class)
  public void tryToExecuteAnUnableOperationThrowsException() throws TroubleshootingOperationException {
    troubleshootingService.executeOperation("notExistingOperation", emptyMap());
  }

  @Test(expected = TroubleshootingOperationException.class)
  public void missingRequiredParameter() throws TroubleshootingOperationException {
    troubleshootingService.executeOperation(TEST_OPERATION_NAME, emptyMap());
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
