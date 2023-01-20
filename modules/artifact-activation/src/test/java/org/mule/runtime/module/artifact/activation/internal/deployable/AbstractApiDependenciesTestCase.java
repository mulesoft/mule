/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.module.artifact.activation.internal.BundleDependencyMatcher.bundleDependency;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static java.lang.String.format;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
public abstract class AbstractApiDependenciesTestCase extends AbstractMuleTestCase {

  @Test
  public void allApiDependenciesAreAddedRAML() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel(format("apps/%s/raml-api-app", getDeploymentType()));

    assertThat(deployableProjectModel.getDependencies(), containsInAnyOrder(
                                                                    bundleDependency("raml-api-a"),
                                                                    bundleDependency("raml-api-b"),
                                                                    bundleDependency("raml-fragment", "1.0.0"),
                                                                    bundleDependency("raml-fragment", "2.0.0")));

  }

  @Test
  public void allApiDependenciesAreAddedWSDL() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel(format("apps/%s/wsdl-api-app", getDeploymentType()));

    assertThat(deployableProjectModel.getDependencies(), containsInAnyOrder(
                                                                    bundleDependency("wsdl-api-a"),
                                                                    bundleDependency("wsdl-api-b"),
                                                                    bundleDependency("wsdl-fragment", "1.0.0"),
                                                                    bundleDependency("wsdl-fragment", "2.0.0"),
                                                                    bundleDependency("library", "2.0.0")));
  }

  @Test
  public void allApiDependenciesAreAddedOAS() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel(format("apps/%s/oas-api-app", getDeploymentType()));
    assertThat(deployableProjectModel.getDependencies(), containsInAnyOrder(
                                                                    bundleDependency("oas-api-a"),
                                                                    bundleDependency("oas-api-b"),
                                                                    bundleDependency("oas-fragment", "1.0.0"),
                                                                    bundleDependency("oas-fragment", "2.0.0")));
  }

  @Test
  public void apiDependsOnLibraryThatDependsOnApiThatDependsOnApi() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel(format("apps/%s/api-multiple-levels-app", getDeploymentType()));
    assertThat(deployableProjectModel.getDependencies(), containsInAnyOrder(
                                                                    bundleDependency("raml-api-a"),
                                                                    bundleDependency("library-depends-on-api"),
                                                                    bundleDependency("api-depends-on-library"),
                                                                    bundleDependency("raml-fragment", "1.0.0"),
                                                                    bundleDependency("raml-fragment", "2.0.0")));
  }

  @Test
  public void apiTransitiveDependenciesDontOverrideMavenResolved() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel(format("apps/%s/api-app", getDeploymentType()));
    assertThat(deployableProjectModel.getDependencies(), containsInAnyOrder(
                                                                    bundleDependency("wsdl-api-a"),
                                                                    bundleDependency("wsdl-api-b"),
                                                                    bundleDependency("wsdl-fragment", "1.0.0"),
                                                                    bundleDependency("wsdl-fragment", "2.0.0"),
                                                                    bundleDependency("library", "1.0.0")));
  }

  protected abstract String getDeploymentType();

  protected abstract DeployableProjectModelBuilder getDeployableProjectModelBuilder(File deployableFolder);

  private DeployableProjectModel getDeployableProjectModel(String deployablePath) throws URISyntaxException {
    DeployableProjectModel model = getDeployableProjectModelBuilder(getDeployableFolder(deployablePath)).build();

    model.validate();

    return model;
  }

  protected File getDeployableFolder(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }
  
}
