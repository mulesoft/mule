/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.maven.MavenDeployableProjectModelBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.Test;

@Feature(DEPLOYMENT_CONFIGURATION)
public class DeployableProjectModelVersionParseTestCase extends AbstractMuleTestCase {

  @Test
  @Issue("W-15228973")
  @Description("When a version is provided using pom properties, include the information when building DeployableProjectModel.")
  public void buildDeployableProjectModelWithPomProperty() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/test-app");
    assertThat(deployableProjectModel.getDescriptor().getVersion(), is("1.2.0-POM"));
  }

  @Test
  @Issue("W-15228973")
  @Description("When a version is provided using system properties, include the information when building DeployableProjectModel.")
  public void buildDeployableProjectModelWithSystemProperty() throws Exception {
    System.setProperty("revision", "0-SYSTEM");
    try {
      DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/test-app");
      assertThat(deployableProjectModel.getDescriptor().getVersion(), is("1.2.0-SYSTEM"));
    } finally {
      System.clearProperty("revision");
    }
  }

  private DeployableProjectModel getDeployableProjectModel(String deployablePath,
                                                           boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor,
                                                           boolean includeTestDependencies)
      throws URISyntaxException {
    DeployableProjectModel model =
        new MavenDeployableProjectModelBuilder(getDeployableFolder(deployablePath),
                                               exportAllResourcesAndPackagesIfEmptyLoaderDescriptor, includeTestDependencies)
            .build();

    model.validate();

    return model;
  }

  private DeployableProjectModel getDeployableProjectModel(String deployablePath) throws URISyntaxException {
    return getDeployableProjectModel(deployablePath, false, false);
  }

  protected File getDeployableFolder(String deployableArtifactPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(deployableArtifactPath).toURI());
  }

}
