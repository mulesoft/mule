/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.application.wrapper;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.DeploymentStopException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationWrapper;

import java.io.IOException;

/**
 * Adds a way to simulate errors on application deployment phases
 */
public class TestApplicationWrapper extends ApplicationWrapper {

  private boolean failOnStopApplication;

  private boolean failOnDisposeApplication;

  public TestApplicationWrapper(Application delegate) throws IOException {
    super(delegate);
  }

  @Override
  public void dispose() {
    if (failOnDisposeApplication) {
      throw new DeploymentException(createStaticMessage("Error disposing application"));
    }

    getDelegate().dispose();
  }

  @Override
  public void stop() {
    if (failOnStopApplication) {
      throw new DeploymentStopException(createStaticMessage("Error stopping application"));
    }

    getDelegate().stop();
  }

  public void setFailOnStopApplication(boolean failOnStopApplication) {
    this.failOnStopApplication = failOnStopApplication;
  }

  public void setFailOnDisposeApplication(boolean failOnDisposeApplication) {
    this.failOnDisposeApplication = failOnDisposeApplication;
  }
}
