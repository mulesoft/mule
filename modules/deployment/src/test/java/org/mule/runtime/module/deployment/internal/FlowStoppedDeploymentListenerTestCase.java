/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static java.lang.String.valueOf;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.container.api.MuleFoldersUtil.getExecutionFolder;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveFlowDeploymentProperties;
import static org.mule.runtime.module.deployment.internal.FlowStoppedDeploymentListener.START_FLOW_ON_DEPLOYMENT_PROPERTY;

import java.io.File;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FlowStoppedDeploymentListenerTestCase {
  private String appName;
  private String flowName;

  @Before
  public void setUp() {
    flowName = "testFlow";
    appName = "testApp";
  }

  @After
  public void tearDown() throws Exception {
    // Cleaning deployment properties
    File file = new File(getExecutionFolder(), appName);
    deleteDirectory(file);
  }

  @Test
  public void onStopShouldSaveDeploymentProperty() throws Exception {
    String propertyName = flowName + "_" + START_FLOW_ON_DEPLOYMENT_PROPERTY;

    FlowStoppedDeploymentListener flowStoppedDeploymentListener = new FlowStoppedDeploymentListener(flowName, appName);
    flowStoppedDeploymentListener.onStop();

    Properties deploymentProperties = resolveFlowDeploymentProperties(appName, empty());
    assertThat(deploymentProperties.get(propertyName), is(notNullValue()));
    assertThat(deploymentProperties.get(propertyName), is("false"));
  }

  @Test
  public void onStopWhenShouldNotPersistIsFalseShouldNotSaveDeploymentProperty() throws Exception {
    String propertyName = flowName + "_" + START_FLOW_ON_DEPLOYMENT_PROPERTY;

    FlowStoppedDeploymentListener flowStoppedDeploymentListener = new FlowStoppedDeploymentListener(flowName, appName);
    flowStoppedDeploymentListener.doNotPersist();
    flowStoppedDeploymentListener.onStop();

    Properties deploymentProperties = resolveFlowDeploymentProperties(appName, empty());
    assertThat(deploymentProperties.get(propertyName), is(nullValue()));
  }

  @Test
  public void onStartShouldSaveDeploymentPropertyAsTrue() throws Exception {
    String propertyName = flowName + "_" + START_FLOW_ON_DEPLOYMENT_PROPERTY;

    FlowStoppedDeploymentListener flowStoppedDeploymentListener = new FlowStoppedDeploymentListener(flowName, appName);
    flowStoppedDeploymentListener.onStart();

    Properties deploymentProperties = resolveFlowDeploymentProperties(appName, empty());
    assertThat(deploymentProperties.get(propertyName), is(notNullValue()));
    assertThat(deploymentProperties.get(propertyName), is("true"));
  }

  @Test
  public void checkIfFlowShouldStartMethodMustSetShouldStartPropertyAsFalseWhenDeploymentPropertyIsFalse() throws Exception {
    String propertyName = flowName + "_" + START_FLOW_ON_DEPLOYMENT_PROPERTY;

    Properties properties = new Properties();
    properties.setProperty(propertyName, valueOf(false));
    resolveFlowDeploymentProperties(appName, of(properties));

    FlowStoppedDeploymentListener flowStoppedDeploymentListener = new FlowStoppedDeploymentListener(flowName, appName);
    flowStoppedDeploymentListener.checkIfFlowShouldStart();

    assertThat(flowStoppedDeploymentListener.shouldStart(), is(false));
    // Property should be reset
    assertThat(flowStoppedDeploymentListener.shouldStart(), is(true));
  }
}
