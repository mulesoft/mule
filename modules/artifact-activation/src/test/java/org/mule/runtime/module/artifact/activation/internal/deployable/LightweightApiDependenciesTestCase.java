/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.getMavenConfig;
import static org.mule.runtime.module.artifact.activation.internal.MavenTestUtils.installArtifact;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.activation.internal.maven.LightweightDeployableProjectModelBuilder;

import java.io.File;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
public class LightweightApiDependenciesTestCase extends AbstractApiDependenciesTestCase {

  public LightweightApiDependenciesTestCase() throws Exception {
    installDependency("library-1.0.0.pom");
    installDependency("library-2.0.0.pom");
    installDependency("library-depends-on-api-1.0.0.pom");
    installDependency("api-depends-on-library-1.0.0-raml.pom");
    installDependency("raml-api-a-1.0.0-raml.pom");
    installDependency("raml-api-b-1.0.0-raml.pom");
    installDependency("raml-api-c-1.0.0-raml.pom");
    installDependency("raml-fragment-1.0.0-raml-fragment.pom");
    installDependency("raml-fragment-2.0.0-raml-fragment.pom");
    installDependency("raml-fragment-b-1.0.0-raml-fragment.pom");
    installDependency("raml-fragment-c-1.0.0-raml-fragment.pom");
    installDependency("wsdl-api-a-1.0.0-wsdl.pom");
    installDependency("wsdl-api-b-1.0.0-wsdl.pom");
    installDependency("wsdl-fragment-1.0.0-wsdl.pom");
    installDependency("wsdl-fragment-2.0.0-wsdl.pom");
    installDependency("oas-api-a-1.0.0-oas.pom");
    installDependency("oas-api-b-1.0.0-oas.pom");
    installDependency("oas-fragment-1.0.0-oas.pom");
    installDependency("oas-fragment-2.0.0-oas.pom");
  }

  private void installDependency(String dependency) throws Exception {
    installArtifact(getDeployableFolder("dependencies/" + dependency),
                    getMavenConfig().getLocalMavenRepositoryLocation());
  }

  @Override
  protected String getDeploymentType() {
    return "lightweight";
  }

  @Override
  protected DeployableProjectModelBuilder getDeployableProjectModelBuilder(File deployableFolder) {
    return new LightweightDeployableProjectModelBuilder(deployableFolder, false);
  }
}
