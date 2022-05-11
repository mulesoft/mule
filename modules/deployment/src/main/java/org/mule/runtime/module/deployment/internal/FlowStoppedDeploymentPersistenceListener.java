/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.valueOf;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveFlowDeploymentProperties;

import org.mule.runtime.core.internal.context.FlowStoppedPersistenceListener;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a listener to persist stop events of Mule flows using deployment properties.
 *
 * @since 4.2.3 4.3.1 4.4.0
 */
public class FlowStoppedDeploymentPersistenceListener implements FlowStoppedPersistenceListener {

  private transient final Logger logger = LoggerFactory.getLogger(getClass());
  private AtomicBoolean shouldPersist;
  private String flowName;
  private String appName;
  static final String START_FLOW_ON_DEPLOYMENT_PROPERTY = "startFlowOnDeployment";
  private String propertyName;
  private AtomicBoolean shouldStart;

  public FlowStoppedDeploymentPersistenceListener(String flowName, String appName) {
    this.flowName = flowName;
    this.appName = appName;
    this.propertyName = flowName + "_" + START_FLOW_ON_DEPLOYMENT_PROPERTY;
    shouldPersist = new AtomicBoolean(true);
    shouldStart = new AtomicBoolean(true);
  }

  @Override
  public void onStart() {
    try {
      Properties properties = resolveFlowDeploymentProperties(appName, empty());
      properties.setProperty(propertyName, valueOf(true));

      resolveFlowDeploymentProperties(appName, of(properties));
    } catch (IOException e) {
      logger.error("FlowStoppedDeploymentListener failed to process notification onStart for flow "
          + flowName, e);
    }
  }

  @Override
  public void onStop() {
    if (!shouldPersist.get()) {
      return;
    }
    try {
      Properties properties = resolveFlowDeploymentProperties(appName, empty());
      properties.setProperty(propertyName, valueOf(false));

      resolveFlowDeploymentProperties(appName, of(properties));
    } catch (IOException e) {
      logger.error("FlowStoppedDeploymentListener failed to process notification onStop for flow "
          + flowName, e);
    }
  }

  @Override
  public void doNotPersist() {
    shouldPersist.set(false);
  }

  @Override
  public Boolean shouldStart() {
    Properties deploymentProperties = null;
    try {
      deploymentProperties = resolveFlowDeploymentProperties(appName, empty());
    } catch (IOException e) {
      logger.error("FlowStoppedDeploymentListener failed to process shouldStart for flow "
          + flowName, e);
    }
    return deploymentProperties != null
        && parseBoolean(deploymentProperties.getProperty(propertyName, "true"));
  }
}
