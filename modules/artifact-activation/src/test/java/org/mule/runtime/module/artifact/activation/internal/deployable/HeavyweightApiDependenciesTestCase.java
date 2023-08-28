/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder;

import java.io.File;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
public class HeavyweightApiDependenciesTestCase extends AbstractApiDependenciesTestCase {

  @Override
  protected String getDeploymentType() {
    return "heavyweight";
  }

  @Override
  protected DeployableProjectModelBuilder getDeployableProjectModelBuilder(File deployableFolder) {
    return new MuleDeployableProjectModelBuilder(deployableFolder);
  }
}
