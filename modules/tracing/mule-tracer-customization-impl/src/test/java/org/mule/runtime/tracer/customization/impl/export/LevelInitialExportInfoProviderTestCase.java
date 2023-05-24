/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.impl.export;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.TRACING_CUSTOMIZATION;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.tracer.customization.api.InitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.provider.DebugInitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.provider.MonitoringInitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.provider.OverviewInitialExportInfoProvider;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@Feature(PROFILING)
@Story(TRACING_CUSTOMIZATION)
@RunWith(Parameterized.class)
public class LevelInitialExportInfoProviderTestCase {

  private static final String HTTP_CONNECTOR_COMPONENT_NAMESPACE = "http";
  private static final String HTTP_REQUEST_COMPONENT_NAME = "request";
  private static final String MULE_COMPONENT_NAMESPACE = "mule";
  private static final String DUMMY_COMPONENT_NAME = "dummy";
  private static final String GET_CONNECTION = "get-connection";
  public static final String PARAMETER_RESOLUTION = "parameters-resolution";

  private static final String OPERATION_EXECUTION = "operation-execution";
  private final InitialExportInfoProvider initialExportInfoProvider;
  private final String componentNamespace;
  private final String componentName;
  private final boolean expectedExportableResult;

  @Parameterized.Parameters(name = "test: {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"debug-http", new DebugInitialExportInfoProvider(), HTTP_CONNECTOR_COMPONENT_NAMESPACE, HTTP_REQUEST_COMPONENT_NAME,
            TRUE},
        {"monitoring-http", new MonitoringInitialExportInfoProvider(), HTTP_CONNECTOR_COMPONENT_NAMESPACE,
            HTTP_REQUEST_COMPONENT_NAME, TRUE},
        {"overview-http", new OverviewInitialExportInfoProvider(), HTTP_CONNECTOR_COMPONENT_NAMESPACE,
            HTTP_REQUEST_COMPONENT_NAME, TRUE},
        {"debug-dummy", new DebugInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE, DUMMY_COMPONENT_NAME,
            TRUE},
        {"monitoring-dummy", new MonitoringInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE,
            DUMMY_COMPONENT_NAME, TRUE},
        {"overview-dummy", new OverviewInitialExportInfoProvider(), HTTP_CONNECTOR_COMPONENT_NAMESPACE,
            DUMMY_COMPONENT_NAME, FALSE},
        {"debug-get-connection", new DebugInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE, GET_CONNECTION,
            TRUE},
        {"monitoring-get-connection", new MonitoringInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE,
            GET_CONNECTION, FALSE},
        {"overview-get-connection", new OverviewInitialExportInfoProvider(), HTTP_CONNECTOR_COMPONENT_NAMESPACE,
            GET_CONNECTION, FALSE},
        {"debug-parameter-resolution", new DebugInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE,
            PARAMETER_RESOLUTION,
            TRUE},
        {"monitoring-parameter-resolution", new MonitoringInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE,
            PARAMETER_RESOLUTION, FALSE},
        {"overview-parameter-resolution", new OverviewInitialExportInfoProvider(), HTTP_CONNECTOR_COMPONENT_NAMESPACE,
            PARAMETER_RESOLUTION, FALSE},
        {"debug-execution-time", new DebugInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE, OPERATION_EXECUTION,
            TRUE},
        {"monitoring-execution-time", new MonitoringInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE,
            OPERATION_EXECUTION, FALSE},
        {"overview-execution-time", new OverviewInitialExportInfoProvider(), HTTP_CONNECTOR_COMPONENT_NAMESPACE,
            OPERATION_EXECUTION, FALSE}});
  }

  public LevelInitialExportInfoProviderTestCase(String testName, InitialExportInfoProvider initialExportInfoProvider,
                                                String componentNamespace, String componentName,
                                                Boolean expectedExportableResult) {
    this.initialExportInfoProvider = initialExportInfoProvider;
    this.componentNamespace = componentNamespace;
    this.componentName = componentName;
    this.expectedExportableResult = expectedExportableResult;
  }

  @Test
  public void testComponent() {
    Component component = mock(Component.class);
    ComponentIdentifier identifier = mock(ComponentIdentifier.class);
    when(component.getIdentifier()).thenReturn(identifier);
    when(identifier.getNamespace()).thenReturn(componentNamespace);
    when(identifier.getName()).thenReturn(componentName);
    assertThat(initialExportInfoProvider.getInitialExportInfo(component).isExportable(), equalTo(expectedExportableResult));
  }
}
