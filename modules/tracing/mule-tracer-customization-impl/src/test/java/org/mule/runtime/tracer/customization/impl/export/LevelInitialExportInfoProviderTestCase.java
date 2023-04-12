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
import org.mule.runtime.tracer.customization.impl.export.InitialExportInfoProvider;
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

  public static final String HTTP_CONNECTOR_COMPONENT_NAMESPACE = "http";
  public static final String HTTP_REQUEST_COMPONENT_NAME = "request";
  public static final String MULE_COMPONENT_NAMESPACE = "mule";
  public static final String DUMMY_COMPONENT_NAME = "dummy";
  public static final String CONNECTION_CREATION_NAME = "connection-creation";

  public static final String PARAMETER_RESOLUTION_NAME = "parameter-resolution";

  public static final String EXECUTION_TIME = "execution-time";
  private final String testName;
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
        {"debug-connection-creation", new DebugInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE, CONNECTION_CREATION_NAME,
            TRUE},
        {"monitoring-connection-creation", new MonitoringInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE,
            CONNECTION_CREATION_NAME, FALSE},
        {"overview-connection-creation", new OverviewInitialExportInfoProvider(), HTTP_CONNECTOR_COMPONENT_NAMESPACE,
            CONNECTION_CREATION_NAME, FALSE},
        {"debug-parameter-resolution", new DebugInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE, PARAMETER_RESOLUTION_NAME,
            TRUE},
        {"monitoring-parameter-resolution", new MonitoringInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE,
            PARAMETER_RESOLUTION_NAME, FALSE},
        {"overview-parameter-resolution", new OverviewInitialExportInfoProvider(), HTTP_CONNECTOR_COMPONENT_NAMESPACE,
            PARAMETER_RESOLUTION_NAME, FALSE},
        {"debug-execution-time", new DebugInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE, EXECUTION_TIME,
            TRUE},
        {"monitoring-execution-time", new MonitoringInitialExportInfoProvider(), MULE_COMPONENT_NAMESPACE,
            EXECUTION_TIME, FALSE},
        {"overview-execution-time", new OverviewInitialExportInfoProvider(), HTTP_CONNECTOR_COMPONENT_NAMESPACE,
            EXECUTION_TIME, FALSE}});
  }

  public LevelInitialExportInfoProviderTestCase(String testName, InitialExportInfoProvider initialExportInfoProvider,
                                                String componentNamespace, String componentName,
                                                Boolean expectedExportableResult) {
    this.testName = testName;
    this.initialExportInfoProvider = initialExportInfoProvider;
    this.componentNamespace = componentNamespace;
    this.componentName = componentName;
    this.expectedExportableResult = expectedExportableResult;

  }

  @Test
  public void tetComponent() {
    Component component = mock(Component.class);
    ComponentIdentifier identifier = mock(ComponentIdentifier.class);
    when(component.getIdentifier()).thenReturn(identifier);
    when(identifier.getNamespace()).thenReturn(componentNamespace);
    when(identifier.getName()).thenReturn(componentName);
    assertThat(initialExportInfoProvider.getInitialExportInfo(component).isExportable(), equalTo(expectedExportableResult));
  }
}
