/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.maven.MavenDeployableProjectModelBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
public class MavenDeployableProjectModelBuilderTestCase extends AbstractMuleTestCase {

  @Test
  public void createBasicDeployableProjectModel() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/basic");

    assertThat(deployableProjectModel.getPackages(), contains("org.test"));
    assertThat(deployableProjectModel.getResources(),
               containsInAnyOrder("test-script.dwl", "app.xml"));

    assertThat(deployableProjectModel.getDependencies(), hasSize(3));
  }

  @Test
  public void createBasicDeployableProjectModelWithDifferentSource() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/basic-with-different-source-directory");

    assertThat(deployableProjectModel.getPackages(), contains("org.test"));
    assertThat(deployableProjectModel.getResources(),
               containsInAnyOrder("test-script.dwl", "app.xml"));

    assertThat(deployableProjectModel.getDependencies(), hasSize(3));
  }

  @Test
  public void createBasicDeployableProjectModelWithCustomResources() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/basic-with-resources");

    assertThat(deployableProjectModel.getPackages(), contains("org.test"));
    assertThat(deployableProjectModel.getResources(),
               containsInAnyOrder("test-script.dwl", "app.xml", "test-script2.dwl"));

    assertThat(deployableProjectModel.getDependencies(), hasSize(3));
  }

  @Test
  public void createDeployableProjectModelWithSharedLibrary() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/shared-lib");

    assertThat(deployableProjectModel.getSharedLibraries(), contains(hasProperty("artifactId", equalTo("derby"))));
  }

  @Test
  public void createDeployableProjectModelWithTransitiveSharedLibrary() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/shared-lib-transitive");

    assertThat(deployableProjectModel.getSharedLibraries(), hasSize(6));
    assertThat(deployableProjectModel.getSharedLibraries(), hasItem(hasProperty("artifactId", equalTo("spring-context"))));
  }

  @Test
  public void createDeployableProjectModelWithAdditionalPluginDependency() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/additional-plugin-dependency");

    assertThat(deployableProjectModel.getPackages(), hasSize(0));
    assertThat(deployableProjectModel.getDependencies(), hasSize(3));

    assertThat(deployableProjectModel.getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-db-connector")))));

    assertThat(deployableProjectModel.getAdditionalPluginDependencies(), aMapWithSize(1));
    assertThat(deployableProjectModel.getAdditionalPluginDependencies(),
               hasEntry(hasProperty("artifactId", equalTo("mule-db-connector")),
                        contains(hasProperty("descriptor", hasProperty("artifactId", equalTo("derby"))))));
  }

  @Test
  public void createDeployableProjectModelWithAdditionalPluginDependencyAndDependency() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/additional-plugin-dependency-and-dep");

    assertThat(deployableProjectModel.getPackages(), hasSize(0));
    assertThat(deployableProjectModel.getDependencies(), hasSize(4));

    assertThat(deployableProjectModel.getDependencies(),
               hasItems(hasProperty("descriptor", hasProperty("artifactId", equalTo("derby"))),
                        hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-db-connector")))));

    assertThat(deployableProjectModel.getAdditionalPluginDependencies(), aMapWithSize(1));
    assertThat(deployableProjectModel.getAdditionalPluginDependencies(),
               hasEntry(hasProperty("artifactId", equalTo("mule-db-connector")),
                        contains(hasProperty("descriptor", hasProperty("artifactId", equalTo("derby"))))));
  }

  @Test
  public void createDeployableProjectModelWithTransitiveAdditionalPluginDependency() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/additional-plugin-dependency-transitive");

    assertThat(deployableProjectModel.getPackages(), hasSize(0));
    assertThat(deployableProjectModel.getDependencies(), hasSize(1));

    assertThat(deployableProjectModel.getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-spring-module")))));

    assertThat(deployableProjectModel.getAdditionalPluginDependencies(), aMapWithSize(1));
    assertThat(deployableProjectModel.getAdditionalPluginDependencies(),
               hasEntry(hasProperty("artifactId", equalTo("mule-spring-module")), hasSize(6)));
    assertThat(deployableProjectModel.getAdditionalPluginDependencies(),
               hasEntry(hasProperty("artifactId", equalTo("mule-spring-module")),
                        hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("spring-context"))))));
  }

  private DeployableProjectModel getDeployableProjectModel(String deployablePath) throws URISyntaxException {
    MavenDeployableProjectModelBuilder modelFactory =
        new MavenDeployableProjectModelBuilder(getDeployableFolder(deployablePath));

    return modelFactory.build();
  }

  protected File getDeployableFolder(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }

}
