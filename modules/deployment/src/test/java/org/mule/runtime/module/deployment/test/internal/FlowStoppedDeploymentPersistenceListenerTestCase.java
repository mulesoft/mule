/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static java.lang.String.valueOf;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.container.api.MuleFoldersUtil.getExecutionFolder;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.getPersistedFlowDeploymentProperties;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveFlowDeploymentProperties;
import static org.mule.runtime.module.deployment.internal.FlowStoppedDeploymentPersistenceListener.START_FLOW_ON_DEPLOYMENT_PROPERTY;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FlowStatePersistenceStory.FLOW_STATE_PERSISTENCE;

import org.mule.runtime.module.deployment.internal.FlowStoppedDeploymentPersistenceListener;
import java.io.File;
import java.util.Optional;
import java.util.Properties;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Test;

@Issue("MULE-19127")
@Feature(DEPLOYMENT_CONFIGURATION)
@Story(FLOW_STATE_PERSISTENCE)
public class FlowStoppedDeploymentPersistenceListenerTestCase {

  private String appName;
  private String propertyName;
  private FlowStoppedDeploymentPersistenceListener flowStoppedDeploymentListener;

  public void createListener() {
    String flowName = "testFlow";
    appName = "testApp";
    propertyName = flowName + "_" + START_FLOW_ON_DEPLOYMENT_PROPERTY;

    flowStoppedDeploymentListener =
        new FlowStoppedDeploymentPersistenceListener(flowName, appName);
  }

  @After
  public void tearDown() throws Exception {
    // Cleaning up deployment properties
    File file = new File(getExecutionFolder(), appName);
    deleteDirectory(file);
  }

  @Test
  @Description("When a flow is stopped, this status should be persisted as a deployment property")
  public void onStopShouldSaveDeploymentProperty() throws Exception {
    createListener();
    flowStoppedDeploymentListener.onStop();

    Optional<Properties> deploymentProperties = getPersistedFlowDeploymentProperties(appName);
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(propertyName), is(notNullValue()));
    assertThat(deploymentProperties.get().get(propertyName), is("false"));
  }

  @Test
  @Description("When doNotPersist method is called, if the flow is stopped afterwards, this should not be persisted")
  public void whenDoNotPersistIsCalledOnStopMethodShouldNotSaveDeploymentProperty() throws Exception {
    createListener();

    flowStoppedDeploymentListener.doNotPersist();
    flowStoppedDeploymentListener.onStop();

    Optional<Properties> deploymentProperties = getPersistedFlowDeploymentProperties(appName);
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(propertyName), is(nullValue()));
  }

  @Test
  @Description("When a flow is started, this status should be persisted as a deployment property")
  public void onStartShouldSaveDeploymentPropertyAsTrue() throws Exception {
    createListener();

    flowStoppedDeploymentListener.onStart();

    Optional<Properties> deploymentProperties = getPersistedFlowDeploymentProperties(appName);
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(propertyName), is(notNullValue()));
    assertThat(deploymentProperties.get().get(propertyName), is("true"));
  }

  @Test
  @Description("shouldStart method should check deployment properties")
  public void shouldStartMethodMustReturnFalseWhenDeploymentPropertyIsFalse() throws Exception {
    createListener();

    Properties properties = new Properties();
    properties.setProperty(propertyName, valueOf(false));
    resolveFlowDeploymentProperties(appName, of(properties));

    assertThat(flowStoppedDeploymentListener.shouldStart(), is(false));
  }
}
